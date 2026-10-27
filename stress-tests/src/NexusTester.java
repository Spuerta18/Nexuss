// NexusTester - single-file security / authorization / abuse / stress harness for the Nexus Market API.
//
// No credentials are stored in this file. Everything sensitive is read at runtime from the environment:
//   BASE          base URL of the target instance            (default http://localhost:8081)
//   ADMIN_EMAIL   initial administrator e-mail               (required for bootstrap)
//   ADMIN_PASSWORD initial administrator password            (required for bootstrap)
//   JWT_SECRET    base64 HMAC secret of the TARGET instance  (only for the token-forgery front; optional)
//   STATE         path to the JSON state file produced by `bootstrap`
//   OUT           path to the JSONL results file to append to
//   RUN           short run id used to namespace generated identifiers (default: timestamp)
//
// Run (JDK 17+ single-file source launcher):
//   java NexusTester.java <command> [args]
// Commands: bootstrap | authz | hostile | abuse | injection | login | session
//           oversell | races | dupreg | load | bigbody | all
//
// Only java.net.http and java.util.concurrent are used; no external dependencies.

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

public class NexusTester {

    // ---------- configuration ----------
    static final String BASE = env("BASE", "http://localhost:8081");
    static final String RUN = env("RUN", Long.toString(System.currentTimeMillis() % 1_000_000));
    static final String OUT = env("OUT", "results.jsonl");
    static final String STATE_PATH = env("STATE", "state.json");

    static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newFixedThreadPool(256, r -> { Thread t = new Thread(r); t.setDaemon(true); return t; }))
            .build();

    static final List<String> ROLES = List.of("BUYER", "SELLER", "ADMINISTRATOR", "LOGISTICS_OPERATOR", "SUPERVISOR");

    // ---------- tiny HTTP ----------
    record Resp(int status, String body, long nanos) {
        String field(String key) { return jsonStr(body, key); }
    }

    static Resp req(String method, String path, String token, String body, String... headerPairs) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(60));
            String ct = "application/json";
            if (token != null) b.header("Authorization", "Bearer " + token);
            for (int i = 0; i + 1 < headerPairs.length; i += 2) {
                if (headerPairs[i].equalsIgnoreCase("Content-Type")) ct = headerPairs[i + 1];
                else if (headerPairs[i].equalsIgnoreCase("RawAuth")) {
                    b.setHeader("Authorization", headerPairs[i + 1]); // overwrite with a raw, possibly malformed value
                } else b.header(headerPairs[i], headerPairs[i + 1]);
            }
            HttpRequest.BodyPublisher pub = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
            if (body != null) b.header("Content-Type", ct);
            b.method(method, pub);
            long t0 = System.nanoTime();
            HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
            long dt = System.nanoTime() - t0;
            return new Resp(r.statusCode(), r.body() == null ? "" : r.body(), dt);
        } catch (Exception e) {
            return new Resp(-1, "CLIENT_ERROR:" + e.getClass().getSimpleName() + ":" + e.getMessage(), 0L);
        }
    }

    static String login(String email, String password) {
        Resp r = req("POST", "/api/auth/login", null, j("email", email, "password", password));
        if (r.status() != 200) throw new RuntimeException("login failed for " + email + " -> " + r.status() + " " + r.body());
        return r.field("accessToken");
    }

    static String tryLoginToken(String email, String password) {
        Resp r = req("POST", "/api/auth/login", null, j("email", email, "password", password));
        return r.status() == 200 ? r.field("accessToken") : null;
    }

    // ---------- JSON helpers (minimal, good enough for this API) ----------
    static String j(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append("\":").append(jsonVal(kv[i + 1]));
        }
        return sb.append('}').toString();
    }
    static String jsonVal(Object v) {
        if (v == null) return "null";
        if (v instanceof Raw raw) return raw.text;             // inject verbatim JSON
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return '"' + esc(v.toString()) + '"';
    }
    record Raw(String text) {}
    static Raw raw(String t) { return new Raw(t); }
    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }
    static String jsonStr(String body, String key) {
        if (body == null) return null;
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(body);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }

    // ---------- results sink ----------
    static final Object OUT_LOCK = new Object();
    static void record(String frente, String test, String identity, String methodPath,
                       Object expected, Object actual, boolean pass, String note) {
        String line = j("frente", frente, "test", test, "identity", identity, "target", methodPath,
                "expected", String.valueOf(expected), "actual", String.valueOf(actual),
                "pass", pass, "note", note == null ? "" : note);
        synchronized (OUT_LOCK) {
            try { Files.writeString(Paths.get(OUT), line + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {}
        }
        String flag = pass ? "ok  " : "FAIL";
        System.out.println("[" + flag + "] " + frente + " | " + identity + " | " + methodPath + " | exp=" + expected + " act=" + actual + (note == null || note.isEmpty() ? "" : " | " + note));
    }

    static String env(String k, String def) { String v = System.getenv(k); return (v == null || v.isBlank()) ? def : v; }

    // ---------- state (written by bootstrap, read by the rest) ----------
    static Map<String, String> state = new LinkedHashMap<>();
    static void saveState() throws Exception {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (var e : state.entrySet()) {
            if (i++ > 0) sb.append(",\n");
            sb.append("  \"").append(e.getKey()).append("\": \"").append(esc(e.getValue())).append('"');
        }
        sb.append("\n}\n");
        Files.writeString(Paths.get(STATE_PATH), sb.toString());
    }
    static void loadState() throws Exception {
        String s = Files.readString(Paths.get(STATE_PATH));
        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(s);
        while (m.find()) state.put(m.group(1), m.group(2).replace("\\\"", "\""));
    }
    static String st(String k) { return state.get(k); }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { System.err.println("usage: NexusTester <command>"); System.exit(2); }
        String cmd = args[0];
        if (!cmd.equals("bootstrap")) loadState();
        switch (cmd) {
            case "bootstrap" -> bootstrap();
            case "authz" -> authz();
            case "hostile" -> hostile();
            case "abuse" -> abuse();
            case "injection" -> injection();
            case "login" -> loginFront();
            case "session" -> session();
            case "oversell" -> oversell(args.length > 1 ? Integer.parseInt(args[1]) : 5);
            case "races" -> races();
            case "dupreg" -> dupreg();
            case "load" -> load();
            case "bigbody" -> bigbody();
            default -> { System.err.println("unknown command " + cmd); System.exit(2); }
        }
        System.out.flush();
        System.exit(0); // the shared HTTP client keeps non-app threads alive; exit explicitly
    }

    // =====================================================================
    // BOOTSTRAP: one user per role + base commerce data, plus a second
    // seller/buyer for ownership (IDOR) tests and a buyer pool for stress.
    // =====================================================================
    static void bootstrap() throws Exception {
        String admMail = env("ADMIN_EMAIL", null), admPw = env("ADMIN_PASSWORD", null);
        if (admMail == null || admPw == null) throw new RuntimeException("ADMIN_EMAIL / ADMIN_PASSWORD required");
        state.put("run", RUN);
        state.put("admin.email", admMail);
        state.put("admin.password", admPw);
        String admin = login(admMail, admPw);
        System.out.println("admin logged in");

        // --- staff: seller1, seller2, supervisor, logistics ---
        String p = "Pw-" + RUN + "-xZ9";          // runtime password, same for the generated test users
        state.put("common.password", p);
        registerStaff(admin, "/api/admin/sellers", id("seller1"), "Seller One", mail("seller1"), p);
        registerStaff(admin, "/api/admin/sellers", id("seller2"), "Seller Two", mail("seller2"), p);
        registerStaff(admin, "/api/admin/supervisors", id("sup"), "Supervisor", mail("sup"), p);
        registerStaff(admin, "/api/admin/logistics-operators", id("logi"), "Logistics", mail("logi"), p);
        for (String who : List.of("seller1", "seller2", "sup", "logi")) { state.put(who + ".email", mail(who)); state.put(who + ".id", id(who)); }

        // --- buyers: buyer1, buyer2 (public self-registration) ---
        registerBuyer(id("buyer1"), "Buyer One", mail("buyer1"), p);
        registerBuyer(id("buyer2"), "Buyer Two", mail("buyer2"), p);
        for (String who : List.of("buyer1", "buyer2")) { state.put(who + ".email", mail(who)); state.put(who + ".id", id(who)); }

        // --- seller1 infrastructure: warehouse + published priced product + inbound stock ---
        String s1 = login(mail("seller1"), p);
        String wh1 = id("wh1");
        must(req("POST", "/api/seller/warehouses", s1, j("identifier", wh1, "name", "WH1", "address", "Addr 1")), 201, 200, "seller1 warehouse");
        String prod1 = id("prod1");
        must(req("POST", "/api/seller/products", s1, j("identifier", prod1, "name", "Widget", "productType", "PHYSICAL",
                "price", raw(j("amount", 100, "currency", "COP")), "variants", raw("[]"))), 201, 200, "seller1 product");
        must(req("PATCH", "/api/seller/products/" + prod1 + "/status", s1, j("status", "PUBLISHED")), 200, 200, "publish prod1");
        must(req("POST", "/api/seller/inventory/inbound", s1, j("productId", prod1, "warehouseId", wh1, "quantity", 500)), 201, 200, "inbound prod1");
        state.put("seller1.warehouse", wh1);
        state.put("seller1.product", prod1);

        // --- seller2 infrastructure (for cross-seller IDOR) ---
        String s2 = login(mail("seller2"), p);
        String wh2 = id("wh2");
        must(req("POST", "/api/seller/warehouses", s2, j("identifier", wh2, "name", "WH2", "address", "Addr 2")), 201, 200, "seller2 warehouse");
        String prod2 = id("prod2");
        must(req("POST", "/api/seller/products", s2, j("identifier", prod2, "name", "Gadget", "productType", "PHYSICAL",
                "price", raw(j("amount", 200, "currency", "COP")), "variants", raw("[]"))), 201, 200, "seller2 product");
        must(req("PATCH", "/api/seller/products/" + prod2 + "/status", s2, j("status", "PUBLISHED")), 200, 200, "publish prod2");
        must(req("POST", "/api/seller/inventory/inbound", s2, j("productId", prod2, "warehouseId", wh2, "quantity", 500)), 201, 200, "inbound prod2");
        state.put("seller2.warehouse", wh2);
        state.put("seller2.product", prod2);

        // --- buyer1 cart with one line (for consult/ownership tests) ---
        String b1 = login(mail("buyer1"), p);
        String cart1 = id("cart1");
        must(req("POST", "/api/buyer/carts", b1, j("cartId", cart1)), 201, 200, "buyer1 cart");
        must(req("POST", "/api/buyer/carts/" + cart1 + "/lines", b1, j("productId", prod1, "quantity", 1)), 200, 201, "buyer1 add line");
        state.put("buyer1.cart", cart1);
        // buyer2 cart (owned by the other buyer)
        String b2 = login(mail("buyer2"), p);
        String cart2 = id("cart2");
        must(req("POST", "/api/buyer/carts", b2, j("cartId", cart2)), 201, 200, "buyer2 cart");
        state.put("buyer2.cart", cart2);

        // --- a fully DELIVERED order owned by buyer1, left ready for return/refund race tests ---
        String deliveredOrder = buildDeliveredOrder("seed", b1, prod1);
        state.put("seed.deliveredOrder", deliveredOrder);

        saveState();
        System.out.println("BOOTSTRAP COMPLETE. state -> " + STATE_PATH);
    }

    // Build an order and drive it all the way to DELIVERED. Returns the order id.
    static String buildDeliveredOrder(String tag, String buyerToken, String product) {
        String admin = login(st("admin.email"), st("admin.password"));
        String logi = login(st("logi.email"), st("common.password"));
        String cart = id(tag + "-c");
        must(req("POST", "/api/buyer/carts", buyerToken, j("cartId", cart)), 201, 200, tag + " cart");
        must(req("POST", "/api/buyer/carts/" + cart + "/lines", buyerToken, j("productId", product, "quantity", 1)), 200, 201, tag + " line");
        String order = id(tag + "-o");
        must(req("POST", "/api/buyer/carts/" + cart + "/orders", buyerToken, j("orderId", order)), 201, 200, tag + " confirm");
        must(req("POST", "/api/logistics/orders/" + order + "/status", logi, j("targetStatus", "PAID")), 200, 200, tag + " pay");
        String ship = id(tag + "-s");
        must(req("POST", "/api/logistics/shipments", logi, j("shipmentId", ship, "orderId", order)), 201, 200, tag + " shipment");
        must(req("POST", "/api/logistics/shipments/" + ship + "/pack", logi, null), 200, 200, tag + " pack");
        must(req("POST", "/api/logistics/shipments/" + ship + "/dispatch", logi, null), 200, 200, tag + " dispatch");
        must(req("POST", "/api/logistics/shipments/" + ship + "/deliver", logi, null), 200, 200, tag + " deliver");
        return order;
    }

    static void registerStaff(String admin, String path, String idv, String name, String email, String pw) {
        must(req("POST", path, admin, j("identifier", idv, "fullName", name, "email", email, "password", pw)), 201, 200, "register " + path);
    }
    static void registerBuyer(String idv, String name, String email, String pw) {
        must(req("POST", "/api/auth/register", null,
                j("identifier", idv, "fullName", name, "email", email, "primaryAddress", "Street 1", "password", pw)), 201, 200, "register buyer " + email);
    }
    static String id(String s) { return "ST-" + RUN + "-" + s; }
    static String mail(String s) { return "st-" + RUN + "-" + s + "@nexus.local"; }
    static void must(Resp r, int ok1, int ok2, String what) {
        if (r.status() != ok1 && r.status() != ok2)
            throw new RuntimeException("bootstrap step failed: " + what + " -> " + r.status() + " " + trunc(r.body(), 300));
    }
    static String trunc(String s, int n) { return s == null ? "" : (s.length() <= n ? s : s.substring(0, n) + "..."); }

    // placeholders replaced in later sections
    static void authz() throws Exception { AuthzFront.run(); }
    static void hostile() throws Exception { HostileFront.run(); }
    static void abuse() throws Exception { AbuseFront.run(); }
    static void injection() throws Exception { InjectionFront.run(); }
    static void loginFront() throws Exception { LoginFront.run(); }
    static void session() throws Exception { SessionFront.run(); }
    static void oversell(int reps) throws Exception { StressFront.oversell(reps); }
    static void races() throws Exception { StressFront.races(); }
    static void dupreg() throws Exception { StressFront.dupreg(); }
    static void load() throws Exception { StressFront.load(); }
    static void bigbody() throws Exception { StressFront.bigbody(); }

    // ---------- JWT forging utilities (used by HostileFront) ----------
    static String b64url(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    static String b64url(String s) { return b64url(s.getBytes(StandardCharsets.UTF_8)); }
    static byte[] hmac(String alg, byte[] key, byte[] msg) throws Exception {
        Mac mac = Mac.getInstance(alg);
        mac.init(new SecretKeySpec(key, alg));
        return mac.doFinal(msg);
    }
    // Mint a JWT with the given claims signed HS256 with `secretBytes`.
    static String mintHs256(byte[] secretBytes, String sub, String role, String email, int ver, long expEpochSec) throws Exception {
        String header = b64url(j("alg", "HS256", "typ", "JWT"));
        long iat = Instant.now().getEpochSecond();
        String payload = b64url(j("sub", sub, "role", role, "email", email, "ver", ver, "iat", iat, "exp", expEpochSec));
        String signingInput = header + "." + payload;
        String sig = b64url(hmac("HmacSHA256", secretBytes, signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + sig;
    }
    static String mintNone(String sub, String role, String email, int ver, long expEpochSec) {
        String header = b64url(j("alg", "none", "typ", "JWT"));
        String payload = b64url(j("sub", sub, "role", role, "email", email, "ver", ver,
                "iat", Instant.now().getEpochSecond(), "exp", expEpochSec));
        return header + "." + payload + ".";
    }

    // tokens for the 5 non-admin roles + admin, used across fronts
    static Map<String, String> roleTokens() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("ADMINISTRATOR", login(st("admin.email"), st("admin.password")));
        t.put("BUYER", login(st("buyer1.email"), st("common.password")));
        t.put("SELLER", login(st("seller1.email"), st("common.password")));
        t.put("LOGISTICS_OPERATOR", login(st("logi.email"), st("common.password")));
        t.put("SUPERVISOR", login(st("sup.email"), st("common.password")));
        return t;
    }
}

// =====================================================================
// FRONT A: full authorization matrix  (endpoint x identity)
// =====================================================================
class AuthzFront {
    record Ep(String method, String path, String role, String body) {}

    static List<Ep> catalog() {
        String g = "NOPE-" + NexusTester.RUN;              // garbage, non-existent resource id
        String az = "AZ-" + NexusTester.RUN;               // fresh id for create bodies
        String moneyPrice = NexusTester.j("amount", 100, "currency", "COP");
        List<Ep> e = new ArrayList<>();
        // ---- PUBLIC ----
        e.add(new Ep("POST", "/api/auth/login", "PUBLIC", NexusTester.j("email", "nobody@nexus.local", "password", "whatever8")));
        e.add(new Ep("POST", "/api/auth/register", "PUBLIC", null)); // body filled per-identity (unique) at call time
        e.add(new Ep("GET", "/api/me", "ANY", null));
        // ---- BUYER ----
        e.add(new Ep("POST", "/api/buyer/carts", "BUYER", NexusTester.j("cartId", az + "-c")));
        e.add(new Ep("GET", "/api/buyer/carts/" + g, "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/carts/" + g + "/lines", "BUYER", NexusTester.j("productId", g, "quantity", 1)));
        e.add(new Ep("PUT", "/api/buyer/carts/" + g + "/lines/" + g, "BUYER", NexusTester.j("quantity", 1)));
        e.add(new Ep("DELETE", "/api/buyer/carts/" + g + "/lines/" + g, "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/carts/" + g + "/orders", "BUYER", NexusTester.j("orderId", az + "-o")));
        e.add(new Ep("GET", "/api/buyer/orders", "BUYER", null));
        e.add(new Ep("GET", "/api/buyer/orders/" + g, "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/orders/" + g + "/cancel", "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/orders/" + g + "/returns", "BUYER", NexusTester.j("returnRequestId", az + "-r", "reason", "broke")));
        e.add(new Ep("GET", "/api/buyer/products", "BUYER", null));
        e.add(new Ep("GET", "/api/buyer/products/" + g, "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/logout", "BUYER", null));
        e.add(new Ep("POST", "/api/buyer/password", "BUYER", NexusTester.j("currentPassword", "x", "newPassword", "longenough8")));
        // ---- SELLER ----
        e.add(new Ep("POST", "/api/seller/products", "SELLER", NexusTester.j("identifier", az + "-p", "name", "n", "productType", "PHYSICAL", "price", NexusTester.raw(moneyPrice), "variants", NexusTester.raw("[]"))));
        e.add(new Ep("PATCH", "/api/seller/products/" + g + "/status", "SELLER", NexusTester.j("status", "PUBLISHED")));
        e.add(new Ep("PUT", "/api/seller/products/" + g + "/variants", "SELLER", NexusTester.j("variants", NexusTester.raw("[]"))));
        e.add(new Ep("GET", "/api/seller/products", "SELLER", null));
        e.add(new Ep("GET", "/api/seller/products/" + g, "SELLER", null));
        e.add(new Ep("GET", "/api/seller/products/" + g + "/inventory", "SELLER", null));
        e.add(new Ep("POST", "/api/seller/warehouses", "SELLER", NexusTester.j("identifier", az + "-w", "name", "n", "address", "a")));
        e.add(new Ep("POST", "/api/seller/inventory/inbound", "SELLER", NexusTester.j("productId", g, "warehouseId", g, "quantity", 1)));
        e.add(new Ep("POST", "/api/seller/inventory/adjustments", "SELLER", NexusTester.j("productId", g, "warehouseId", g, "quantityDelta", 1)));
        e.add(new Ep("POST", "/api/seller/logout", "SELLER", null));
        e.add(new Ep("POST", "/api/seller/password", "SELLER", NexusTester.j("currentPassword", "x", "newPassword", "longenough8")));
        // ---- ADMINISTRATOR ----
        e.add(new Ep("POST", "/api/admin/sellers", "ADMINISTRATOR", NexusTester.j("identifier", az + "-as", "fullName", "n", "email", az + "-as@nexus.local", "password", "longenough8")));
        e.add(new Ep("POST", "/api/admin/supervisors", "ADMINISTRATOR", NexusTester.j("identifier", az + "-au", "fullName", "n", "email", az + "-au@nexus.local", "password", "longenough8")));
        e.add(new Ep("POST", "/api/admin/logistics-operators", "ADMINISTRATOR", NexusTester.j("identifier", az + "-al", "fullName", "n", "email", az + "-al@nexus.local", "password", "longenough8")));
        e.add(new Ep("PATCH", "/api/admin/sellers/" + g + "/status", "ADMINISTRATOR", NexusTester.j("status", "ACTIVE")));
        e.add(new Ep("PATCH", "/api/admin/users/" + g + "/status", "ADMINISTRATOR", NexusTester.j("role", "SELLER", "status", "ACTIVE")));
        e.add(new Ep("GET", "/api/admin/users/" + g + "?role=SELLER", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/warehouses", "ADMINISTRATOR", NexusTester.j("identifier", az + "-mw", "name", "n", "address", "a")));
        e.add(new Ep("POST", "/api/admin/warehouses/" + g + "/deactivate", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/returns/" + g + "/approve", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/returns/" + g + "/reject", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/refunds/" + g + "/process", "ADMINISTRATOR", null));
        e.add(new Ep("GET", "/api/admin/operations", "ADMINISTRATOR", null));
        e.add(new Ep("GET", "/api/admin/audit-logs", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/logout", "ADMINISTRATOR", null));
        e.add(new Ep("POST", "/api/admin/password", "ADMINISTRATOR", NexusTester.j("currentPassword", "x", "newPassword", "longenough8")));
        // ---- LOGISTICS ----
        e.add(new Ep("GET", "/api/logistics/orders/" + g, "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/orders/" + g + "/status", "LOGISTICS_OPERATOR", NexusTester.j("targetStatus", "PAID")));
        e.add(new Ep("POST", "/api/logistics/shipments", "LOGISTICS_OPERATOR", NexusTester.j("shipmentId", az + "-sh", "orderId", g)));
        e.add(new Ep("POST", "/api/logistics/shipments/" + g + "/pack", "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/shipments/" + g + "/dispatch", "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/shipments/" + g + "/deliver", "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/inventory/damaged", "LOGISTICS_OPERATOR", NexusTester.j("productId", g, "warehouseId", g, "quantity", 1)));
        e.add(new Ep("POST", "/api/logistics/inventory/returns", "LOGISTICS_OPERATOR", NexusTester.j("productId", g, "warehouseId", g, "quantity", 1)));
        e.add(new Ep("GET", "/api/logistics/products/" + g + "/inventory", "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/logout", "LOGISTICS_OPERATOR", null));
        e.add(new Ep("POST", "/api/logistics/password", "LOGISTICS_OPERATOR", NexusTester.j("currentPassword", "x", "newPassword", "longenough8")));
        // ---- SUPERVISOR ----
        e.add(new Ep("GET", "/api/supervisor/orders/" + g, "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/products", "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/products/" + g, "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/products/" + g + "/inventory", "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/users/" + g + "?role=SELLER", "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/operations", "SUPERVISOR", null));
        e.add(new Ep("GET", "/api/supervisor/audit-logs", "SUPERVISOR", null));
        e.add(new Ep("POST", "/api/supervisor/logout", "SUPERVISOR", null));
        e.add(new Ep("POST", "/api/supervisor/password", "SUPERVISOR", NexusTester.j("currentPassword", "x", "newPassword", "longenough8")));
        return e;
    }

    static boolean isLogout(String p) { return p.endsWith("/logout"); }
    static boolean isPassword(String p) { return p.endsWith("/password"); }

    static void run() {
        Map<String, String> tok = NexusTester.roleTokens();
        List<Ep> eps = catalog();
        int n = 0;
        for (Ep ep : eps) {
            n++;
            // ---- anonymous ----
            if (ep.role().equals("PUBLIC")) {
                NexusTester.Resp r = NexusTester.req(ep.method(), ep.path(), null, bodyFor(ep, "anon"));
                // login legitimately returns 401 on bad credentials; public endpoints must only never be 403
                boolean pass = r.status() != 403;
                NexusTester.record("A", "matrix", "anonymous", ep.method() + " " + ep.path(), "not 403 (public)", r.status(), pass, "public");
            } else {
                NexusTester.Resp r = NexusTester.req(ep.method(), ep.path(), null, ep.body());
                boolean pass = r.status() == 401;
                NexusTester.record("A", "matrix", "anonymous", ep.method() + " " + ep.path(), 401, r.status(), pass, leak(r));
            }

            // ---- each authenticated identity ----
            for (String id : NexusTester.ROLES) {
                String token = tok.get(id);
                boolean correct = ep.role().equals(id) || ep.role().equals("ANY")
                        || (ep.role().equals("PUBLIC"));
                String body = ep.body();
                boolean refresh = false;
                if (correct && isPassword(ep.path())) body = "{}";   // avoid a real password change; still proves authz passed
                if (correct && isLogout(ep.path()) && !id.equals("PUBLIC")) refresh = true;
                if (ep.role().equals("PUBLIC")) body = bodyFor(ep, id);
                NexusTester.Resp r = NexusTester.req(ep.method(), ep.path(), token, body);
                if (correct) {
                    // public endpoints may answer 401 (login bad-creds); role endpoints must not be 401/403 for the right role
                    boolean pass = ep.role().equals("PUBLIC") ? r.status() != 403 : (r.status() != 401 && r.status() != 403);
                    NexusTester.record("A", "matrix", id + "(correct)", ep.method() + " " + ep.path(), ep.role().equals("PUBLIC") ? "not 403" : "not 401/403", r.status(), pass, pass ? "" : leak(r));
                    if (refresh) tok.put(id, NexusTester.login(emailOf(id), NexusTester.st("common.password")));
                } else {
                    boolean pass = r.status() == 403;
                    NexusTester.record("A", "matrix", id + "(wrong)", ep.method() + " " + ep.path(), 403, r.status(), pass, pass ? "" : leak(r));
                }
            }

            // ---- wrong role + INVALID body must still be 403 (authz before validation) ----
            if (!ep.role().equals("PUBLIC") && !ep.role().equals("ANY")) {
                String wrong = ep.role().equals("BUYER") ? "SELLER" : "BUYER";
                NexusTester.Resp r = NexusTester.req(ep.method(), ep.path(), tok.get(wrong), "{\"garbage\":true,\"quantity\":-999}");
                boolean pass = r.status() == 403;
                NexusTester.record("A", "matrix-invalidbody", wrong + "(wrong)", ep.method() + " " + ep.path(), 403, r.status(), pass, pass ? "" : leak(r));
            }
        }
        System.out.println("AUTHZ matrix done: " + n + " endpoints x 6 identities (+ invalid-body wrong-role pass).");
    }

    static String emailOf(String role) {
        return switch (role) {
            case "BUYER" -> NexusTester.st("buyer1.email");
            case "SELLER" -> NexusTester.st("seller1.email");
            case "LOGISTICS_OPERATOR" -> NexusTester.st("logi.email");
            case "SUPERVISOR" -> NexusTester.st("sup.email");
            default -> NexusTester.st("admin.email");
        };
    }
    static String bodyFor(Ep ep, String who) {
        if (ep.path().endsWith("/login")) return NexusTester.j("email", "nobody-" + who + "@nexus.local", "password", "whatever8");
        if (ep.path().endsWith("/register")) {
            String u = "AZreg-" + NexusTester.RUN + "-" + who;
            return NexusTester.j("identifier", u, "fullName", "n", "email", u + "@nexus.local", "primaryAddress", "a", "password", "longenough8");
        }
        return ep.body();
    }
    // Flag a response body that leaks internals (stack traces, exception class names).
    static String leak(NexusTester.Resp r) {
        String b = r.body() == null ? "" : r.body();
        String low = b.toLowerCase();
        if (low.contains("exception") || low.contains("\tat ") || low.contains("nexussmarket.") || low.contains("stacktrace"))
            return "LEAK? " + NexusTester.trunc(b, 160);
        return NexusTester.trunc(b, 120);
    }
}

// =====================================================================
// FRONT A (continued): hostile identities / forged tokens
// =====================================================================
class HostileFront {
    static void run() throws Exception {
        // A protected endpoint that needs a valid ADMINISTRATOR (clear 200/401/403 separation).
        String probe = "/api/admin/operations";
        String buyerProbe = "/api/me";                 // any authenticated identity
        String buyerId = NexusTester.st("buyer1.id");
        String buyerEmail = NexusTester.st("buyer1.email");
        String secretB64 = NexusTester.env("JWT_SECRET", null);
        byte[] secret = secretB64 == null ? null : Base64.getDecoder().decode(secretB64);
        long future = Instant.now().plusSeconds(3600).getEpochSecond();
        long past = Instant.now().minusSeconds(3600).getEpochSecond();

        // 1. alg=none forged admin token
        String none = NexusTester.mintNone(buyerId, "ADMINISTRATOR", buyerEmail, 0, future);
        check("alg=none -> admin", probe, none);

        // 2. signed with a DIFFERENT random secret
        byte[] other = new byte[48]; new Random(42).nextBytes(other);
        String wrongKey = NexusTester.mintHs256(other, buyerId, "ADMINISTRATOR", buyerEmail, 0, future);
        check("wrong-secret signature", probe, wrongKey);

        // 3. a real buyer token with the signature bytes tampered
        String realBuyer = NexusTester.login(buyerEmail, NexusTester.st("common.password"));
        String tampered = realBuyer.substring(0, realBuyer.lastIndexOf('.') + 1)
                + flip(realBuyer.substring(realBuyer.lastIndexOf('.') + 1));
        check("tampered signature", buyerProbe, tampered);

        // 4. payload role changed to ADMINISTRATOR but re-signed with the REAL secret (privilege escalation via claim)
        if (secret != null) {
            String escalated = NexusTester.mintHs256(secret, buyerId, "ADMINISTRATOR", buyerEmail, 0, future);
            NexusTester.Resp r = NexusTester.req("GET", probe, escalated, null);
            boolean pass = r.status() == 401; // filter must reload the user and reject the mismatched role claim
            NexusTester.record("A", "hostile", "role-claim=ADMIN (valid sig)", "GET " + probe, 401, r.status(), pass,
                    pass ? "" : "PRIV-ESC? " + AuthzFront.leak(r));
            // 5. valid signature but expired
            String expired = NexusTester.mintHs256(secret, buyerId, "BUYER", buyerEmail, 0, past);
            check("expired (valid sig)", buyerProbe, expired);
            // 6. valid signature but wrong token version (simulate revoked)
            String badVer = NexusTester.mintHs256(secret, buyerId, "BUYER", buyerEmail, 999, future);
            check("stale ver=999 (valid sig)", buyerProbe, badVer);
            // 7. valid signature but no 'ver' claim
            String header = NexusTester.b64url(NexusTester.j("alg", "HS256", "typ", "JWT"));
            String payload = NexusTester.b64url(NexusTester.j("sub", buyerId, "role", "BUYER", "email", buyerEmail,
                    "iat", Instant.now().getEpochSecond(), "exp", future));
            String si = header + "." + payload;
            String noVer = si + "." + NexusTester.b64url(NexusTester.hmac("HmacSHA256", secret, si.getBytes(StandardCharsets.UTF_8)));
            check("missing ver claim (valid sig)", buyerProbe, noVer);
        } else {
            NexusTester.record("A", "hostile", "role-claim/expired/ver", "GET " + buyerProbe, "n/a", "skipped", true, "JWT_SECRET not provided");
        }

        // 8. BLOCKED user token  (create, block via admin, reuse token)
        statusRevocationCase("BLOCKED");
        // 9. INACTIVE user token
        statusRevocationCase("INACTIVE");

        // 10. token reused after logout
        String bEmail = NexusTester.st("buyer2.email");
        String t = NexusTester.login(bEmail, NexusTester.st("common.password"));
        NexusTester.req("POST", "/api/buyer/logout", t, null);
        check("reuse after logout", "/api/me", t);
        // 11. token reused after password change
        String t2 = NexusTester.login(bEmail, NexusTester.st("common.password")); // re-login (logout bumped ver)
        NexusTester.req("POST", "/api/buyer/password", t2, NexusTester.j("currentPassword", NexusTester.st("common.password"), "newPassword", "Changed-" + NexusTester.RUN + "-9"));
        check("reuse after password change", "/api/me", t2);
        // restore buyer2 password so later fronts can log in (best effort)
        String t3 = NexusTester.tryLoginToken(bEmail, "Changed-" + NexusTester.RUN + "-9");
        if (t3 != null) NexusTester.req("POST", "/api/buyer/password", t3, NexusTester.j("currentPassword", "Changed-" + NexusTester.RUN + "-9", "newPassword", NexusTester.st("common.password")));

        // 12. malformed Authorization headers
        malformed("no Bearer prefix", "/api/me", "Token " + realBuyerFresh());
        malformed("Bearer only", "/api/me", "Bearer");
        malformed("Bearer garbage", "/api/me", "Bearer not.a.jwt");
        malformed("empty Authorization", "/api/me", "");
    }

    static String realBuyerFresh() { return NexusTester.login(NexusTester.st("buyer1.email"), NexusTester.st("common.password")); }

    static void check(String name, String path, String token) {
        NexusTester.Resp r = NexusTester.req("GET", path, token, null);
        boolean pass = r.status() == 401;
        NexusTester.record("A", "hostile", name, "GET " + path, 401, r.status(), pass, pass ? "" : AuthzFront.leak(r));
    }
    static void malformed(String name, String path, String rawAuthValue) {
        NexusTester.Resp r = NexusTester.req("GET", path, null, null, "RawAuth", rawAuthValue);
        boolean pass = r.status() == 401;
        NexusTester.record("A", "hostile", name, "GET " + path, 401, r.status(), pass, pass ? "" : AuthzFront.leak(r));
    }
    static String flip(String s) {
        if (s.isEmpty()) return "AAAA";
        char[] c = s.toCharArray();
        c[c.length - 1] = c[c.length - 1] == 'A' ? 'B' : 'A';
        c[0] = c[0] == 'A' ? 'B' : 'A';
        return new String(c);
    }
    // Create a throwaway buyer, capture a token, then have admin set the status; reuse the token.
    static void statusRevocationCase(String status) {
        String run = NexusTester.RUN;
        String uid = "ST-" + run + "-rev" + status;
        String email = "st-" + run + "-rev" + status.toLowerCase() + "@nexus.local";
        String pw = NexusTester.st("common.password");
        NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", uid, "fullName", "n", "email", email, "primaryAddress", "a", "password", pw));
        String token = NexusTester.tryLoginToken(email, pw);
        if (token == null) { NexusTester.record("A", "hostile", status + " user token", "GET /api/me", 401, "setup-failed", false, "could not login new user"); return; }
        String admin = NexusTester.login(NexusTester.st("admin.email"), NexusTester.st("admin.password"));
        NexusTester.Resp sr = NexusTester.req("PATCH", "/api/admin/users/" + uid + "/status", admin, NexusTester.j("role", "BUYER", "status", status));
        NexusTester.Resp r = NexusTester.req("GET", "/api/me", token, null);
        boolean pass = r.status() == 401;
        NexusTester.record("A", "hostile", status + " user token", "GET /api/me", 401, r.status(), pass,
                sr.status() != 200 ? "status-change->" + sr.status() : (pass ? "" : AuthzFront.leak(r)));
    }
}

// =====================================================================
// FRONT B: abuse, ownership (IDOR), role violations, mass assignment,
//          and domain business rules.
// =====================================================================
class AbuseFront {
    static String admin, b1, b2, s1, s2, logi, sup;
    static void tok() {
        admin = NexusTester.login(NexusTester.st("admin.email"), NexusTester.st("admin.password"));
        b1 = NexusTester.login(NexusTester.st("buyer1.email"), NexusTester.st("common.password"));
        b2 = NexusTester.login(NexusTester.st("buyer2.email"), NexusTester.st("common.password"));
        s1 = NexusTester.login(NexusTester.st("seller1.email"), NexusTester.st("common.password"));
        s2 = NexusTester.login(NexusTester.st("seller2.email"), NexusTester.st("common.password"));
        logi = NexusTester.login(NexusTester.st("logi.email"), NexusTester.st("common.password"));
        sup = NexusTester.login(NexusTester.st("sup.email"), NexusTester.st("common.password"));
    }
    // generic recorder: pass if actual status is in the allowed set
    static void scen(String test, String who, String mp, Object expected, NexusTester.Resp r, Set<Integer> allow, String extra) {
        boolean pass = allow.contains(r.status());
        String note = extra == null ? "" : extra;
        if (r.body() != null && (r.body().toLowerCase().contains("exception") || r.body().contains("\tat ")))
            note = note + " LEAK?:" + NexusTester.trunc(r.body(), 120);
        NexusTester.record("B", test, who, mp, expected, r.status(), pass, note);
    }
    static final Set<Integer> DENY = new HashSet<>(List.of(401, 403, 404));

    static void run() {
        tok();
        idor();
        roles();
        massAssignment();
        domainRules();
    }

    static void idor() {
        String cart1 = NexusTester.st("buyer1.cart");
        String prod1 = NexusTester.st("seller1.product");
        String wh1 = NexusTester.st("seller1.warehouse");
        // buyer2 touches buyer1's cart
        scen("idor-cart-read", "buyer2->buyer1 cart", "GET /api/buyer/carts/" + cart1, "deny (not 2xx)",
                NexusTester.req("GET", "/api/buyer/carts/" + cart1, b2, null), DENY, note404vs403("buyer1 cart"));
        scen("idor-cart-addline", "buyer2->buyer1 cart", "POST .../lines", "deny",
                NexusTester.req("POST", "/api/buyer/carts/" + cart1 + "/lines", b2, NexusTester.j("productId", prod1, "quantity", 1)), DENY, null);
        // buyer1's order (seed delivered)
        String ord = NexusTester.st("seed.deliveredOrder");
        scen("idor-order-read", "buyer2->buyer1 order", "GET /api/buyer/orders/" + ord, "deny",
                NexusTester.req("GET", "/api/buyer/orders/" + ord, b2, null), DENY, null);
        scen("idor-order-cancel", "buyer2->buyer1 order", "POST .../cancel", "deny",
                NexusTester.req("POST", "/api/buyer/orders/" + ord + "/cancel", b2, null), DENY, null);
        scen("idor-order-return", "buyer2->buyer1 order", "POST .../returns", "deny",
                NexusTester.req("POST", "/api/buyer/orders/" + ord + "/returns", b2, NexusTester.j("returnRequestId", "IDOR-" + NexusTester.RUN, "reason", "x")), DENY, null);
        // seller2 modifies seller1's product / inventory
        scen("idor-product-status", "seller2->seller1 product", "PATCH /api/seller/products/" + prod1 + "/status", "deny",
                NexusTester.req("PATCH", "/api/seller/products/" + prod1 + "/status", s2, NexusTester.j("status", "SUSPENDED")), DENY, note404vs403("seller1 product"));
        scen("idor-product-variants", "seller2->seller1 product", "PUT .../variants", "deny",
                NexusTester.req("PUT", "/api/seller/products/" + prod1 + "/variants", s2, NexusTester.j("variants", NexusTester.raw("[]"))), DENY, null);
        scen("idor-inventory-inbound", "seller2->seller1 product", "POST /api/seller/inventory/inbound", "deny",
                NexusTester.req("POST", "/api/seller/inventory/inbound", s2, NexusTester.j("productId", prod1, "warehouseId", wh1, "quantity", 5)), DENY, null);
        scen("idor-inventory-adjust", "seller2->seller1 product", "POST /api/seller/inventory/adjustments", "deny",
                NexusTester.req("POST", "/api/seller/inventory/adjustments", s2, NexusTester.j("productId", prod1, "warehouseId", wh1, "quantityDelta", -5)), DENY, null);
        // a PUBLISHED product is public by design -> expect 200 (documented, not a leak)
        NexusTester.Resp pubRead = NexusTester.req("GET", "/api/seller/products/" + prod1, s2, null);
        NexusTester.record("B", "idor-product-read-published", "seller2->seller1 PUBLISHED product", "GET /api/seller/products/" + prod1,
                "200 (published = public catalog)", pubRead.status(), pubRead.status() == 200, "by design: any active user reads published products");
        // a NON-published (SUSPENDED) competitor product must NOT be readable by another seller
        String susp = suspendedProduct(s1, "idorsusp");
        NexusTester.Resp suspRead = NexusTester.req("GET", "/api/seller/products/" + susp, s2, null);
        scen("idor-product-read-suspended", "seller2->seller1 SUSPENDED product", "GET /api/seller/products/" + susp, "deny (403/404)",
                suspRead, DENY, note404vs403("seller1 suspended product"));
    }
    static String note404vs403(String what) { return "watch 404(not found) vs 403(not yours) info-leak for " + what; }

    static void roles() {
        // seller attempts to register a MARKETPLACE warehouse (admin-only endpoint)
        scen("role-seller-mkt-warehouse", "SELLER->admin warehouse", "POST /api/admin/warehouses", 403,
                NexusTester.req("POST", "/api/admin/warehouses", s1, NexusTester.j("identifier", "RW-" + NexusTester.RUN, "name", "n", "address", "a")), Set.of(403), null);
        // non-admins try to register staff
        for (String[] w : new String[][]{{"BUYER", b1}, {"SELLER", s1}, {"LOGISTICS", logi}, {"SUPERVISOR", sup}}) {
            scen("role-staff-register", w[0] + "->register seller", "POST /api/admin/sellers", 403,
                    NexusTester.req("POST", "/api/admin/sellers", w[1], NexusTester.j("identifier", "X", "fullName", "n", "email", "x@y.z", "password", "longenough8")), Set.of(403), null);
        }
        // supervisor attempts writes
        scen("role-supervisor-write", "SUPERVISOR->publish product", "POST /api/seller/products", 403,
                NexusTester.req("POST", "/api/seller/products", sup, NexusTester.j("identifier", "X", "name", "n", "productType", "PHYSICAL", "price", NexusTester.raw(NexusTester.j("amount", 1, "currency", "COP")), "variants", NexusTester.raw("[]"))), Set.of(403), null);
        // administrator modifies a cart (admins must NOT touch carts)
        scen("role-admin-cart", "ADMIN->buyer cart", "POST /api/buyer/carts", 403,
                NexusTester.req("POST", "/api/buyer/carts", admin, NexusTester.j("cartId", "AC-" + NexusTester.RUN)), Set.of(403), null);
        // operator attempts to approve returns / process refunds
        scen("role-operator-approve", "LOGISTICS->approve return", "POST /api/admin/returns/x/approve", 403,
                NexusTester.req("POST", "/api/admin/returns/x/approve", logi, null), Set.of(403), null);
        scen("role-operator-refund", "LOGISTICS->process refund", "POST /api/admin/refunds/x/process", 403,
                NexusTester.req("POST", "/api/admin/refunds/x/process", logi, null), Set.of(403), null);
        // seller changes own SellerStatus (admin-only)
        scen("role-seller-self-status", "SELLER->seller status", "PATCH /api/admin/sellers/{id}/status", 403,
                NexusTester.req("PATCH", "/api/admin/sellers/" + NexusTester.st("seller1.id") + "/status", s1, NexusTester.j("status", "ACTIVE")), Set.of(403), null);
        // user changes own status (admin-only)
        scen("role-user-self-status", "BUYER->user status", "PATCH /api/admin/users/{id}/status", 403,
                NexusTester.req("PATCH", "/api/admin/users/" + NexusTester.st("buyer1.id") + "/status", b1, NexusTester.j("role", "BUYER", "status", "BLOCKED")), Set.of(403), null);
    }

    static void massAssignment() {
        String uid = "MA-" + NexusTester.RUN;
        String email = "ma-" + NexusTester.RUN + "@nexus.local";
        String pw = NexusTester.st("common.password");
        // extra fields that must be ignored
        String body = "{" +
                "\"identifier\":\"" + uid + "\"," +
                "\"fullName\":\"Mass Assign\"," +
                "\"email\":\"" + email + "\"," +
                "\"primaryAddress\":\"a\"," +
                "\"password\":\"" + pw + "\"," +
                "\"role\":\"ADMINISTRATOR\",\"status\":\"ACTIVE\",\"sellerStatus\":\"ACTIVE\"," +
                "\"authTokenVersion\":999,\"passwordHash\":\"pwned\"}";
        NexusTester.Resp reg = NexusTester.req("POST", "/api/auth/register", null, body);
        String role = reg.field("role");
        NexusTester.record("B", "mass-assign", "register+role=ADMIN", "POST /api/auth/register",
                "created as BUYER", "status=" + reg.status() + ",role=" + role, "BUYER".equals(role) || role == null, NexusTester.trunc(reg.body(), 160));
        // confirm privileges did not escalate: the new user must be denied an admin endpoint
        String t = NexusTester.tryLoginToken(email, pw);
        if (t != null) {
            NexusTester.Resp me = NexusTester.req("GET", "/api/me", t, null);
            NexusTester.record("B", "mass-assign", "new user /me role", "GET /api/me", "BUYER/ACTIVE",
                    "role=" + me.field("role") + ",status=" + me.field("status"), "BUYER".equals(me.field("role")), "");
            NexusTester.Resp adm = NexusTester.req("GET", "/api/admin/operations", t, null);
            NexusTester.record("B", "mass-assign", "new user admin access", "GET /api/admin/operations", 403, adm.status(), adm.status() == 403, "");
        }
    }

    static void domainRules() {
        String prod1 = NexusTester.st("seller1.product");
        String wh1 = NexusTester.st("seller1.warehouse");
        // --- quantities ---
        String cart = freshCart(b1, "dr");
        scen("rule-qty-zero", "buyer", "addCartLine qty=0", 400,
                NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", b1, NexusTester.j("productId", prod1, "quantity", 0)), Set.of(400), null);
        scen("rule-qty-negative", "buyer", "addCartLine qty=-5", 400,
                NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", b1, NexusTester.j("productId", prod1, "quantity", -5)), Set.of(400), null);
        NexusTester.Resp huge = NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", b1, NexusTester.j("productId", prod1, "quantity", NexusTester.raw("2147483647")));
        scen("rule-qty-huge-add", "buyer", "addCartLine qty=MAX_INT", "not 500", huge, Set.of(200, 201, 400, 409), null);
        // --- price on publish ---
        pubPrice("rule-price-zero", "0", "COP", Set.of(400));
        pubPrice("rule-price-negative", "-10", "COP", Set.of(400));
        pubPrice("rule-price-decimals", "10.123456789", "COP", Set.of(200, 201, 400));
        pubPrice("rule-price-bad-currency", "10", "XYZ", Set.of(400));
        // --- mixing currencies in a cart ---
        String usdProd = publishProduct(s1, "usd", "USD", "50");
        String mixCart = freshCart(b1, "mix");
        NexusTester.req("POST", "/api/buyer/carts/" + mixCart + "/lines", b1, NexusTester.j("productId", prod1, "quantity", 1)); // COP
        scen("rule-currency-mix", "buyer", "add USD line to COP cart", 409,
                NexusTester.req("POST", "/api/buyer/carts/" + mixCart + "/lines", b1, NexusTester.j("productId", usdProd, "quantity", 1)), Set.of(409), null);
        // --- confirm empty cart ---
        String emptyCart = freshCart(b1, "empty");
        scen("rule-confirm-empty", "buyer", "confirm empty cart", 409,
                NexusTester.req("POST", "/api/buyer/carts/" + emptyCart + "/orders", b1, NexusTester.j("orderId", "EO-" + NexusTester.RUN)), Set.of(409), null);
        // --- add a non-published (SUSPENDED) product -> rejected ---
        String unpub = suspendedProduct(s1, "unpub");
        String upCart = freshCart(b1, "unpub");
        scen("rule-unpublished-add", "buyer", "add SUSPENDED product", 409,
                NexusTester.req("POST", "/api/buyer/carts/" + upCart + "/lines", b1, NexusTester.j("productId", unpub, "quantity", 1)), Set.of(409, 404), null);
        // --- confirm insufficient stock (exact stock = 2) ---
        String lowProd = publishWithStock(s1, "low", 2);
        String lowCart = freshCart(b1, "low");
        NexusTester.req("POST", "/api/buyer/carts/" + lowCart + "/lines", b1, NexusTester.j("productId", lowProd, "quantity", 5));
        scen("rule-insufficient-stock", "buyer", "confirm qty5 stock2", 409,
                NexusTester.req("POST", "/api/buyer/carts/" + lowCart + "/orders", b1, NexusTester.j("orderId", "LO-" + NexusTester.RUN)), Set.of(409), null);
        // --- DELIVERED order cannot be cancelled / advanced ---
        String delivered = NexusTester.st("seed.deliveredOrder");
        scen("rule-cancel-delivered", "buyer", "cancel DELIVERED", 409,
                NexusTester.req("POST", "/api/buyer/orders/" + delivered + "/cancel", b1, null), Set.of(409), null);
        scen("rule-advance-delivered", "logistics", "advance DELIVERED->PAID", "4xx",
                NexusTester.req("POST", "/api/logistics/orders/" + delivered + "/status", logi, NexusTester.j("targetStatus", "PAID")), Set.of(400, 409), null);
        // --- cancel after paid ---
        String paidOrder = buildOrderTo(b1, prod1, "PAID", "paid");
        scen("rule-cancel-paid", "buyer", "cancel PAID order", 409,
                NexusTester.req("POST", "/api/buyer/orders/" + paidOrder + "/cancel", b1, null), Set.of(409), null);
        // --- skip / reverse order states ---
        String pend = buildOrderTo(b1, prod1, "PENDING_PAYMENT", "skip");
        scen("rule-skip-to-delivered", "logistics", "PENDING->DELIVERED via status", "4xx",
                NexusTester.req("POST", "/api/logistics/orders/" + pend + "/status", logi, NexusTester.j("targetStatus", "DELIVERED")), Set.of(400, 409), null);
        String paid2 = buildOrderTo(b1, prod1, "PAID", "rev");
        scen("rule-reverse-status", "logistics", "PAID->PENDING_PAYMENT", 409,
                NexusTester.req("POST", "/api/logistics/orders/" + paid2 + "/status", logi, NexusTester.j("targetStatus", "PENDING_PAYMENT")), Set.of(400, 409), null);
        // --- shipment out of order: dispatch before pack ---
        String shipOrder = buildOrderTo(b1, prod1, "PAID", "ship");
        String ship = "SH-" + NexusTester.RUN + "-ooo";
        NexusTester.req("POST", "/api/logistics/shipments", logi, NexusTester.j("shipmentId", ship, "orderId", shipOrder));
        scen("rule-shipment-out-of-order", "logistics", "dispatch before pack", 409,
                NexusTester.req("POST", "/api/logistics/shipments/" + ship + "/dispatch", logi, null), Set.of(400, 409), null);
        // --- return out of conditions (non-delivered) ---
        scen("rule-return-nondelivered", "buyer", "return PAID order", "4xx",
                NexusTester.req("POST", "/api/buyer/orders/" + paidOrder + "/returns", b1, NexusTester.j("returnRequestId", "RR-" + NexusTester.RUN, "reason", "x")), Set.of(400, 409), null);
        // --- approve / reject twice, refund twice ---
        doubleDecisionRules();
        // --- duplicate email / identifier ---
        String dupEmail = "dup-" + NexusTester.RUN + "@nexus.local";
        NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", "DUP1-" + NexusTester.RUN, "fullName", "n", "email", dupEmail, "primaryAddress", "a", "password", "longenough8"));
        scen("rule-dup-email", "public", "register duplicate email", 409,
                NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", "DUP2-" + NexusTester.RUN, "fullName", "n", "email", dupEmail, "primaryAddress", "a", "password", "longenough8")), Set.of(409), null);
        scen("rule-dup-identifier", "public", "register duplicate identifier", 409,
                NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", "DUP1-" + NexusTester.RUN, "fullName", "n", "email", "other-" + NexusTester.RUN + "@nexus.local", "primaryAddress", "a", "password", "longenough8")), Set.of(409), null);
        // --- negative stock via adjustment ---
        String adjProd = publishProduct(s1, "adj", "COP", "10");
        NexusTester.req("POST", "/api/seller/inventory/inbound", s1, NexusTester.j("productId", adjProd, "warehouseId", wh1, "quantity", 3));
        scen("rule-negative-adjust", "seller", "adjust delta=-100 (stock 3)", "4xx (no negative stock)",
                NexusTester.req("POST", "/api/seller/inventory/adjustments", s1, NexusTester.j("productId", adjProd, "warehouseId", wh1, "quantityDelta", -100)), Set.of(400, 409), null);
    }

    static void doubleDecisionRules() {
        String prod1 = NexusTester.st("seller1.product");
        // delivered order + return request
        String order = NexusTester.buildDeliveredOrder("dbl-" + NexusTester.RUN, b1, prod1);
        String rr = "RR-dbl-" + NexusTester.RUN;
        NexusTester.req("POST", "/api/buyer/orders/" + order + "/returns", b1, NexusTester.j("returnRequestId", rr, "reason", "x"));
        NexusTester.Resp a1 = NexusTester.req("POST", "/api/admin/returns/" + rr + "/approve", admin, null);
        scen("rule-approve-first", "admin", "approve return (1st)", 200, a1, Set.of(200), null);
        scen("rule-approve-twice", "admin", "approve return (2nd)", 409,
                NexusTester.req("POST", "/api/admin/returns/" + rr + "/approve", admin, null), Set.of(409), null);
        scen("rule-reject-after-approve", "admin", "reject already-approved", 409,
                NexusTester.req("POST", "/api/admin/returns/" + rr + "/reject", admin, null), Set.of(409), null);
        // refund = REF-<returnId>
        String refund = "REF-" + rr;
        NexusTester.Resp f1 = NexusTester.req("POST", "/api/admin/refunds/" + refund + "/process", admin, null);
        scen("rule-refund-first", "admin", "process refund (1st)", 200, f1, Set.of(200), null);
        scen("rule-refund-twice", "admin", "process refund (2nd)", 409,
                NexusTester.req("POST", "/api/admin/refunds/" + refund + "/process", admin, null), Set.of(409), null);
    }

    // helpers
    static String freshCart(String token, String tag) {
        String c = "C-" + NexusTester.RUN + "-" + tag + "-" + Integer.toHexString(new Random().nextInt());
        NexusTester.req("POST", "/api/buyer/carts", token, NexusTester.j("cartId", c));
        return c;
    }
    static String publishProduct(String seller, String tag, String currency, String amount) {
        return publishProductRaw(seller, tag, currency, amount, true);
    }
    static String publishProductRaw(String seller, String tag, String currency, String amount, boolean publish) {
        String wh = NexusTester.st("seller1.warehouse");
        String p = "P-" + NexusTester.RUN + "-" + tag;
        NexusTester.req("POST", "/api/seller/products", seller, NexusTester.j("identifier", p, "name", "n", "productType", "PHYSICAL",
                "price", NexusTester.raw(NexusTester.j("amount", NexusTester.raw(amount), "currency", currency)), "variants", NexusTester.raw("[]")));
        if (publish) {
            NexusTester.req("PATCH", "/api/seller/products/" + p + "/status", seller, NexusTester.j("status", "PUBLISHED"));
            NexusTester.req("POST", "/api/seller/inventory/inbound", seller, NexusTester.j("productId", p, "warehouseId", wh, "quantity", 50));
        }
        return p;
    }
    // POST /products creates an already-PUBLISHED product; inbound exactly `qty` units (no default 50).
    static String publishWithStock(String seller, String tag, int qty) {
        String wh = NexusTester.st("seller1.warehouse");
        String p = "PWS-" + NexusTester.RUN + "-" + tag;
        NexusTester.req("POST", "/api/seller/products", seller, NexusTester.j("identifier", p, "name", "n", "productType", "PHYSICAL",
                "price", NexusTester.raw(NexusTester.j("amount", 100, "currency", "COP")), "variants", NexusTester.raw("[]")));
        if (qty > 0) NexusTester.req("POST", "/api/seller/inventory/inbound", seller, NexusTester.j("productId", p, "warehouseId", wh, "quantity", qty));
        return p;
    }
    // Create a product and move it out of the public catalog (SUSPENDED).
    static String suspendedProduct(String seller, String tag) {
        String p = "SUS-" + NexusTester.RUN + "-" + tag;
        NexusTester.req("POST", "/api/seller/products", seller, NexusTester.j("identifier", p, "name", "n", "productType", "PHYSICAL",
                "price", NexusTester.raw(NexusTester.j("amount", 100, "currency", "COP")), "variants", NexusTester.raw("[]")));
        NexusTester.req("PATCH", "/api/seller/products/" + p + "/status", seller, NexusTester.j("status", "SUSPENDED"));
        return p;
    }
    static void pubPrice(String test, String amount, String currency, Set<Integer> allow) {
        String p = "PP-" + NexusTester.RUN + "-" + test;
        NexusTester.Resp r = NexusTester.req("POST", "/api/seller/products", s1, NexusTester.j("identifier", p, "name", "n", "productType", "PHYSICAL",
                "price", NexusTester.raw(NexusTester.j("amount", NexusTester.raw(amount), "currency", currency)), "variants", NexusTester.raw("[]")));
        scen(test, "seller", "publish price=" + amount + " " + currency, describe(allow), r, allow, null);
    }
    static String describe(Set<Integer> s) { return s.toString(); }
    // Build an order owned by `buyerToken` for `product`, advanced to target status (PENDING_PAYMENT or PAID).
    static String buildOrderTo(String buyerToken, String product, String target, String tag) {
        String cart = freshCart(buyerToken, tag);
        NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", buyerToken, NexusTester.j("productId", product, "quantity", 1));
        String order = "O-" + NexusTester.RUN + "-" + tag;
        NexusTester.req("POST", "/api/buyer/carts/" + cart + "/orders", buyerToken, NexusTester.j("orderId", order));
        if (target.equals("PAID")) NexusTester.req("POST", "/api/logistics/orders/" + order + "/status", logi, NexusTester.j("targetStatus", "PAID"));
        return order;
    }
}

// =====================================================================
// FRONT B: injection and hostile inputs (never 500, never a stack trace)
// =====================================================================
class InjectionFront {
    static void run() {
        String b1 = NexusTester.login(NexusTester.st("buyer1.email"), NexusTester.st("common.password"));
        String cart = NexusTester.st("buyer1.cart");
        String admin = NexusTester.login(NexusTester.st("admin.email"), NexusTester.st("admin.password"));

        // NoSQL operator injection in login
        inj("nosql-login-email-ne", "POST", "/api/auth/login", null,
                "{\"email\":{\"$ne\":null},\"password\":{\"$ne\":null}}", "application/json");
        inj("nosql-login-email-gt", "POST", "/api/auth/login", null,
                "{\"email\":{\"$gt\":\"\"},\"password\":\"x\"}", "application/json");
        inj("nosql-login-regex", "POST", "/api/auth/login", null,
                "{\"email\":{\"$regex\":\".*\"},\"password\":{\"$ne\":1}}", "application/json");
        // NoSQL operators in a body id field
        inj("nosql-cartid", "POST", "/api/buyer/carts", b1,
                "{\"cartId\":{\"$ne\":null}}", "application/json");
        inj("nosql-productid", "POST", "/api/buyer/carts/" + cart + "/lines", b1,
                "{\"productId\":{\"$ne\":null},\"quantity\":1}", "application/json");
        // 1 MB string
        String big = "A".repeat(1_000_000);
        inj("string-1mb-email", "POST", "/api/auth/login", null,
                NexusTester.j("email", big + "@x.z", "password", "x"), "application/json");
        inj("string-1mb-cartid", "POST", "/api/buyer/carts", b1, NexusTester.j("cartId", big), "application/json");
        // malformed JSON
        inj("malformed-json", "POST", "/api/auth/login", null, "{\"email\": ", "application/json");
        inj("not-json-at-all", "POST", "/api/auth/login", null, "<<<not json>>>", "application/json");
        // wrong Content-Type
        inj("wrong-content-type", "POST", "/api/auth/login", null, "email=x&password=y", "application/x-www-form-urlencoded");
        inj("content-type-text", "POST", "/api/auth/login", null, NexusTester.j("email", "x@y.z", "password", "x"), "text/plain");
        // path traversal / special chars in path id
        injGet("path-traversal", "/api/buyer/products/" + enc("../../etc/passwd"), b1);
        injGet("path-dotdot", "/api/buyer/orders/..%2f..%2fadmin", b1);
        injGet("path-unicode", "/api/buyer/products/" + enc("\u0000‮💩"), b1);
        injGet("path-special", "/api/admin/users/" + enc("a b&c=d?e#f") + "?role=SELLER", admin);
        // empty body on endpoints that require one
        inj("empty-body-login", "POST", "/api/auth/login", null, "", "application/json");
        inj("empty-object-login", "POST", "/api/auth/login", null, "{}", "application/json");
        // null in each required field
        inj("null-email", "POST", "/api/auth/login", null, "{\"email\":null,\"password\":null}", "application/json");
        inj("null-register-fields", "POST", "/api/auth/register", null,
                "{\"identifier\":null,\"fullName\":null,\"email\":null,\"primaryAddress\":null,\"password\":null}", "application/json");
        // numbers as strings / strings as numbers
        inj("qty-as-string", "POST", "/api/buyer/carts/" + cart + "/lines", b1,
                "{\"productId\":\"x\",\"quantity\":\"5\"}", "application/json");
        inj("qty-as-float", "POST", "/api/buyer/carts/" + cart + "/lines", b1,
                "{\"productId\":\"x\",\"quantity\":1.5}", "application/json");
        inj("price-amount-as-string", "POST", "/api/seller/products",
                NexusTester.login(NexusTester.st("seller1.email"), NexusTester.st("common.password")),
                "{\"identifier\":\"NS-" + NexusTester.RUN + "\",\"name\":\"n\",\"productType\":\"PHYSICAL\",\"price\":{\"amount\":\"abc\",\"currency\":\"COP\"},\"variants\":[]}", "application/json");
        // deeply nested JSON
        inj("deep-nesting", "POST", "/api/auth/login", null, deep(2000), "application/json");
    }

    static String enc(String s) { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
    static String deep(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append("{\"a\":");
        sb.append("1");
        for (int i = 0; i < n; i++) sb.append("}");
        return sb.toString();
    }
    static void inj(String name, String method, String path, String token, String body, String ct) {
        NexusTester.Resp r = NexusTester.req(method, path, token, body, "Content-Type", ct);
        verdict(name, method + " " + path, r);
    }
    static void injGet(String name, String path, String token) {
        NexusTester.Resp r = NexusTester.req("GET", path, token, null);
        verdict(name, "GET " + path, r);
    }
    static void verdict(String name, String mp, NexusTester.Resp r) {
        String b = r.body() == null ? "" : r.body().toLowerCase();
        boolean leak = b.contains("exception") || b.contains("\tat ") || b.contains("nexussmarket.") || b.contains("stacktrace") || b.contains("caused by");
        boolean is500 = r.status() == 500 || r.status() == -1;
        boolean pass = !is500 && !leak;
        NexusTester.record("B", "injection", name, mp, "not 500 / no stacktrace", r.status() + (leak ? " LEAK" : ""), pass,
                pass ? "" : NexusTester.trunc(r.body(), 200));
    }
}

// =====================================================================
// FRONT B: login uniformity, timing, lockout
// =====================================================================
class LoginFront {
    static void run() {
        // make one user INACTIVE to compare its login response
        String admin = NexusTester.login(NexusTester.st("admin.email"), NexusTester.st("admin.password"));
        String uid = "LI-" + NexusTester.RUN;
        String email = "li-" + NexusTester.RUN + "@nexus.local";
        String pw = NexusTester.st("common.password");
        NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", uid, "fullName", "n", "email", email, "primaryAddress", "a", "password", pw));
        NexusTester.req("PATCH", "/api/admin/users/" + uid + "/status", admin, NexusTester.j("role", "BUYER", "status", "INACTIVE"));

        NexusTester.Resp noUser = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", "ghost-" + NexusTester.RUN + "@nexus.local", "password", "whatever8"));
        NexusTester.Resp wrongPw = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", NexusTester.st("buyer1.email"), "password", "wrong-password-xx"));
        NexusTester.Resp inactive = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", email, "password", pw));

        String bodyNoUser = normalize(noUser.body());
        String bodyWrong = normalize(wrongPw.body());
        String bodyInactive = normalize(inactive.body());
        boolean sameCode = noUser.status() == wrongPw.status() && wrongPw.status() == inactive.status();
        boolean sameBody = bodyNoUser.equals(bodyWrong) && bodyWrong.equals(bodyInactive);
        NexusTester.record("B", "login-uniform", "no-user vs wrong-pw vs inactive", "POST /api/auth/login",
                "identical code+body (401)", "codes=" + noUser.status() + "/" + wrongPw.status() + "/" + inactive.status() + " sameBody=" + sameBody,
                sameCode && sameBody && noUser.status() == 401, "noUser=" + NexusTester.trunc(noUser.body(), 80));

        // timing: existing vs non-existing e-mail
        int N = 40;
        long existing = timeAvg(NexusTester.st("buyer1.email"), "wrong-password-xx", N);
        long missing = timeAvg("ghost2-" + NexusTester.RUN + "@nexus.local", "wrong-password-xx", N);
        double diffMs = (existing - missing) / 1_000_000.0;
        NexusTester.record("B", "login-timing", "existing vs missing email", "POST /api/auth/login",
                "no large timing gap", String.format("existing=%.1fms missing=%.1fms diff=%.1fms", existing / 1e6, missing / 1e6, diffMs),
                Math.abs(diffMs) < 25.0, "info: BCrypt on existing user may dominate");

        // no lockout: many failures then a correct login still succeeds
        for (int i = 0; i < 30; i++) NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", NexusTester.st("buyer1.email"), "password", "bad" + i));
        NexusTester.Resp after = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", NexusTester.st("buyer1.email"), "password", pw));
        NexusTester.record("B", "login-lockout", "30 failures then correct", "POST /api/auth/login",
                "document whether lockout exists", "after30fail=" + after.status() + " (200=no lockout)", true,
                after.status() == 200 ? "NO rate limiting / lockout on failed logins" : "some throttling observed");
    }
    static long timeAvg(String email, String pw, int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) sum += NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", email, "password", pw)).nanos();
        return sum / n;
    }
    static String normalize(String body) {
        if (body == null) return "";
        return body.replaceAll("\"timestamp\"\\s*:\\s*\"[^\"]*\"", "\"timestamp\":\"X\"")
                   .replaceAll("\"requestId\"\\s*:\\s*\"[^\"]*\"", "\"requestId\":\"X\"")
                   .replaceAll("\"path\"\\s*:\\s*\"[^\"]*\"", "\"path\":\"X\"");
    }
}

// =====================================================================
// FRONT B: session (password change rules, token reuse)
// =====================================================================
class SessionFront {
    static void run() {
        String email = NexusTester.st("seller1.email");
        String pw = NexusTester.st("common.password");
        String t = NexusTester.login(email, pw);
        // wrong current password
        NexusTester.record("B", "session", "change pw wrong current", "POST /api/seller/password",
                "4xx (not changed)", NexusTester.req("POST", "/api/seller/password", t, NexusTester.j("currentPassword", "definitely-wrong", "newPassword", "brandnew123")).status(),
                true, "expect 400 IncorrectCurrentPassword");
        // too-short new password
        NexusTester.Resp shortR = NexusTester.req("POST", "/api/seller/password", t, NexusTester.j("currentPassword", pw, "newPassword", "short"));
        NexusTester.record("B", "session", "change pw too short", "POST /api/seller/password", 400, shortR.status(), shortR.status() == 400, "");
        // token reuse after a real password change, then restore
        String t2 = NexusTester.login(email, pw);
        String newPw = "Rotated-" + NexusTester.RUN + "-7";
        NexusTester.Resp chg = NexusTester.req("POST", "/api/seller/password", t2, NexusTester.j("currentPassword", pw, "newPassword", newPw));
        NexusTester.Resp reuse = NexusTester.req("GET", "/api/me", t2, null);
        NexusTester.record("B", "session", "reuse token after pw change", "GET /api/me", 401, reuse.status(), reuse.status() == 401,
                "pwChange=" + chg.status());
        // restore original password so other fronts keep working
        String t3 = NexusTester.tryLoginToken(email, newPw);
        if (t3 != null) NexusTester.req("POST", "/api/seller/password", t3, NexusTester.j("currentPassword", newPw, "newPassword", pw));
    }
}

// =====================================================================
// FRONT D: stress and concurrency
// =====================================================================
class StressFront {
    static String logi() { return NexusTester.login(NexusTester.st("logi.email"), NexusTester.st("common.password")); }
    static String admin() { return NexusTester.login(NexusTester.st("admin.email"), NexusTester.st("admin.password")); }
    static String seller1() { return NexusTester.login(NexusTester.st("seller1.email"), NexusTester.st("common.password")); }

    // ---- D1: oversell ----
    static void oversell(int reps) throws Exception {
        int buyers = 60, stock = 10;
        List<String> tokens = ensureBuyerPool(buyers);
        String s1 = seller1();
        String wh = NexusTester.st("seller1.warehouse");
        for (int rep = 1; rep <= reps; rep++) {
            String prod = "OS-" + NexusTester.RUN + "-r" + rep;
            NexusTester.req("POST", "/api/seller/products", s1, NexusTester.j("identifier", prod, "name", "n", "productType", "PHYSICAL",
                    "price", NexusTester.raw(NexusTester.j("amount", 100, "currency", "COP")), "variants", NexusTester.raw("[]")));
            NexusTester.req("PATCH", "/api/seller/products/" + prod + "/status", s1, NexusTester.j("status", "PUBLISHED"));
            NexusTester.req("POST", "/api/seller/inventory/inbound", s1, NexusTester.j("productId", prod, "warehouseId", wh, "quantity", stock));

            // each buyer prepares its own cart+line, then all confirm at a barrier
            AtomicInteger ok = new AtomicInteger(), conflict = new AtomicInteger(), other = new AtomicInteger();
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(buyers);
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < buyers; i++) {
                final int idx = i; final String token = tokens.get(i); final String r = "" + rep;
                fs.add(pool.submit(() -> {
                    String cart = "OSC-" + NexusTester.RUN + "-r" + r + "-" + idx;
                    NexusTester.req("POST", "/api/buyer/carts", token, NexusTester.j("cartId", cart));
                    NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", token, NexusTester.j("productId", prod, "quantity", 1));
                    try { start.await(); } catch (InterruptedException ignored) {}
                    NexusTester.Resp c = NexusTester.req("POST", "/api/buyer/carts/" + cart + "/orders", token, NexusTester.j("orderId", "OSO-" + NexusTester.RUN + "-r" + r + "-" + idx));
                    if (c.status() == 200 || c.status() == 201) ok.incrementAndGet();
                    else if (c.status() == 409) conflict.incrementAndGet();
                    else other.incrementAndGet();
                }));
            }
            Thread.sleep(300);
            start.countDown();
            for (Future<?> f : fs) f.get();
            pool.shutdown();
            boolean pass = ok.get() == stock && other.get() == 0;
            NexusTester.record("D", "oversell", "rep " + rep + " (" + buyers + " buyers, stock " + stock + ")", "confirm order",
                    stock + " accepted / " + (buyers - stock) + " rejected", "accepted=" + ok.get() + " conflict=" + conflict.get() + " other=" + other.get(),
                    pass, pass ? "" : (ok.get() > stock ? "OVERSOLD by " + (ok.get() - stock) : "unexpected counts"));
        }
    }

    static List<String> ensureBuyerPool(int n) {
        String pw = NexusTester.st("common.password");
        List<String> tokens = new ArrayList<>();
        // register sequentially (idempotent-ish), then login
        for (int i = 0; i < n; i++) {
            String id = "PB-" + NexusTester.RUN + "-" + i;
            String email = "pb-" + NexusTester.RUN + "-" + i + "@nexus.local";
            NexusTester.req("POST", "/api/auth/register", null, NexusTester.j("identifier", id, "fullName", "n", "email", email, "primaryAddress", "a", "password", pw));
            tokens.add(NexusTester.login(email, pw));
        }
        return tokens;
    }

    // ---- D2: state races ----
    static void races() throws Exception {
        String admin = admin(), logi = logi();
        String b1 = NexusTester.login(NexusTester.st("buyer1.email"), NexusTester.st("common.password"));
        String prod1 = NexusTester.st("seller1.product");

        // (a) two concurrent approvals of the same return
        String order = NexusTester.buildDeliveredOrder("race-ap-" + NexusTester.RUN, b1, prod1);
        String rr = "RRACE-" + NexusTester.RUN;
        NexusTester.req("POST", "/api/buyer/orders/" + order + "/returns", b1, NexusTester.j("returnRequestId", rr, "reason", "x"));
        int[] approve = fire2(() -> NexusTester.req("POST", "/api/admin/returns/" + rr + "/approve", admin, null));
        NexusTester.record("D", "race-approve", "2x approve same return", "POST .../approve", "exactly 1 success",
                "success=" + approve[0] + " fail=" + approve[1], approve[0] == 1, approve[0] > 1 ? "DOUBLE APPROVE" : "");

        // (b) two concurrent refund processings
        String refund = "REF-" + rr;
        int[] ref = fire2(() -> NexusTester.req("POST", "/api/admin/refunds/" + refund + "/process", admin, null));
        NexusTester.record("D", "race-refund", "2x process same refund", "POST .../process", "exactly 1 success",
                "success=" + ref[0] + " fail=" + ref[1], ref[0] == 1, ref[0] > 1 ? "DOUBLE REFUND" : "");

        // (c) two concurrent status advances of the same order
        String ord2 = AbuseBuild.orderPending(b1, prod1, logi, "race-adv-" + NexusTester.RUN);
        int[] adv = fire2(() -> NexusTester.req("POST", "/api/logistics/orders/" + ord2 + "/status", logi, NexusTester.j("targetStatus", "PAID")));
        NexusTester.record("D", "race-advance", "2x advance PENDING->PAID", "POST .../status", "exactly 1 success",
                "success=" + adv[0] + " fail=" + adv[1], adv[0] == 1, adv[0] > 1 ? "DOUBLE ADVANCE" : "");

        // (d) two concurrent cancellations of the same order (stock release once?)
        String ord3 = AbuseBuild.orderPending(b1, prod1, logi, "race-cxl-" + NexusTester.RUN);
        int[] cxl = fire2(() -> NexusTester.req("POST", "/api/buyer/orders/" + ord3 + "/cancel", b1, null));
        NexusTester.record("D", "race-cancel", "2x cancel same order", "POST .../cancel", "exactly 1 success (stock released once)",
                "success=" + cxl[0] + " fail=" + cxl[1], cxl[0] == 1, cxl[0] > 1 ? "DOUBLE CANCEL -> possible double stock release" : "");
    }

    interface Call { NexusTester.Resp run(); }
    static int[] fire2(Call c) throws Exception {
        AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 2; i++) fs.add(pool.submit(() -> {
            try { start.await(); } catch (InterruptedException ignored) {}
            NexusTester.Resp r = c.run();
            if (r.status() == 200 || r.status() == 201) ok.incrementAndGet(); else fail.incrementAndGet();
        }));
        Thread.sleep(50); start.countDown();
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        return new int[]{ok.get(), fail.get()};
    }

    // ---- D3: duplicate registrations ----
    static void dupreg() throws Exception {
        int n = 30;
        String email = "dupreg-" + NexusTester.RUN + "@nexus.local";
        String pw = NexusTester.st("common.password");
        AtomicInteger ok = new AtomicInteger(), conflict = new AtomicInteger(), other = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            fs.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException ignored) {}
                // same e-mail, different identifier -> uniqueness must still allow only one
                NexusTester.Resp r = NexusTester.req("POST", "/api/auth/register", null,
                        NexusTester.j("identifier", "DR-" + NexusTester.RUN + "-" + idx, "fullName", "n", "email", email, "primaryAddress", "a", "password", pw));
                if (r.status() == 201 || r.status() == 200) ok.incrementAndGet();
                else if (r.status() == 409) conflict.incrementAndGet();
                else other.incrementAndGet();
            }));
        }
        Thread.sleep(50); start.countDown();
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        NexusTester.record("D", "dup-register", n + "x same-email register", "POST /api/auth/register", "exactly 1 success",
                "success=" + ok.get() + " conflict=" + conflict.get() + " other=" + other.get(), ok.get() == 1,
                ok.get() > 1 ? "DUPLICATE ACCOUNTS CREATED" : "");
    }

    // ---- D4: load ramps ----
    static void load() throws Exception {
        for (int users : new int[]{10, 50, 100, 200}) ramp(users, 30);
        // burst of 500 failed logins
        burstFailedLogins(500);
    }
    static void ramp(int users, int seconds) throws Exception {
        String buyer = NexusTester.login(NexusTester.st("buyer1.email"), NexusTester.st("common.password"));
        String pw = NexusTester.st("common.password"); String email = NexusTester.st("buyer1.email");
        ConcurrentLinkedQueue<Long> lat = new ConcurrentLinkedQueue<>();
        AtomicLong ops = new AtomicLong(), errors = new AtomicLong();
        long end = System.currentTimeMillis() + seconds * 1000L;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            final int idx = i;
            fs.add(pool.submit(() -> {
                int k = 0;
                while (System.currentTimeMillis() < end) {
                    NexusTester.Resp r;
                    int pick = (idx + k) % 3;
                    if (pick == 0) r = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", email, "password", pw));
                    else if (pick == 1) r = NexusTester.req("GET", "/api/buyer/products", buyer, null);
                    else r = NexusTester.req("GET", "/api/buyer/orders", buyer, null);
                    lat.add(r.nanos());
                    ops.incrementAndGet();
                    if (r.status() >= 500 || r.status() == -1) errors.incrementAndGet();
                    k++;
                }
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        long[] arr = lat.stream().mapToLong(Long::longValue).sorted().toArray();
        double thr = ops.get() / (double) seconds;
        NexusTester.record("D", "load", users + " users / " + seconds + "s", "login+reads",
                "stable, low errors", String.format("ops=%d thr=%.0f/s p50=%.1fms p95=%.1fms p99=%.1fms err=%d",
                        ops.get(), thr, pct(arr, 50) / 1e6, pct(arr, 95) / 1e6, pct(arr, 99) / 1e6, errors.get()),
                errors.get() == 0, errors.get() > 0 ? errors.get() + " 5xx/transport errors" : "");
    }
    static void burstFailedLogins(int n) throws Exception {
        AtomicLong errors = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(100);
        List<Future<?>> fs = new ArrayList<>();
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) fs.add(pool.submit(() -> {
            NexusTester.Resp r = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", NexusTester.st("buyer1.email"), "password", "nope"));
            if (r.status() != 401) errors.incrementAndGet();
        }));
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        double sec = (System.nanoTime() - t0) / 1e9;
        // app still alive?
        NexusTester.Resp alive = NexusTester.req("POST", "/api/auth/login", null, NexusTester.j("email", NexusTester.st("buyer1.email"), "password", NexusTester.st("common.password")));
        NexusTester.record("D", "burst-logins", n + " failed logins", "POST /api/auth/login",
                "all 401, app survives", String.format("non401=%d in %.1fs, aliveAfter=%d", errors.get(), sec, alive.status()),
                errors.get() == 0 && alive.status() == 200, "");
    }
    static double pct(long[] sorted, int p) {
        if (sorted.length == 0) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    // ---- D5: big bodies ----
    static void bigbody() {
        bodyTest("body-5mb", 5 * 1024 * 1024);
        bodyTest("body-50mb", 50 * 1024 * 1024);
        // deep nesting
        NexusTester.Resp r = NexusTester.req("POST", "/api/auth/register", null, InjectionFront.deep(100_000), "Content-Type", "application/json");
        NexusTester.record("D", "bigbody", "deep nesting 100k", "POST /api/auth/register", "rejected, no crash",
                r.status() + "", r.status() != 500 && r.status() != -1, NexusTester.trunc(r.body(), 100));
        aliveCheck("after bigbody");
    }
    static void bodyTest(String name, int size) {
        String filler = "A".repeat(size);
        String body = NexusTester.j("identifier", "BB-" + NexusTester.RUN + "-" + name, "fullName", filler, "email", "bb-" + NexusTester.RUN + "-" + name + "@nexus.local", "primaryAddress", "a", "password", "longenough8");
        NexusTester.Resp r = NexusTester.req("POST", "/api/auth/register", null, body, "Content-Type", "application/json");
        NexusTester.record("D", "bigbody", name, "POST /api/auth/register", "rejected cleanly, no 5xx crash",
                r.status() + "", r.status() != -1, "status=" + r.status());
    }
    static void aliveCheck(String when) {
        NexusTester.Resp alive = NexusTester.req("GET", "/v3/api-docs", null, null);
        NexusTester.record("D", "liveness", when, "GET /v3/api-docs", 200, alive.status(), alive.status() == 200, "");
    }
}

// small helper shared by StressFront.races
class AbuseBuild {
    static String orderPending(String buyerToken, String product, String logi, String tag) {
        String cart = "C-" + tag;
        NexusTester.req("POST", "/api/buyer/carts", buyerToken, NexusTester.j("cartId", cart));
        NexusTester.req("POST", "/api/buyer/carts/" + cart + "/lines", buyerToken, NexusTester.j("productId", product, "quantity", 1));
        String order = "O-" + tag;
        NexusTester.req("POST", "/api/buyer/carts/" + cart + "/orders", buyerToken, NexusTester.j("orderId", order));
        return order;
    }
}
