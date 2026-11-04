package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import nexussMarket.infrastructure.seed.DemoDataSeeder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end scenarios over real HTTP: the application runs on a random port
 * with the {@code memory} profile (no database) and its demo data, and every
 * step goes through the REST API exactly as a client would use it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nexus.seed.demo.password=" + EndToEndScenariosTest.DEMO_PASSWORD)
@ActiveProfiles("memory")
class EndToEndScenariosTest {

    /** Test-only password of the demo accounts; never used outside this test. */
    static final String DEMO_PASSWORD = "E2e-Demo-Password-1";
    private static final String PASSWORD = "E2e-User-Password-1";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void usersOfEveryRoleAreCreatedAndLogIn() throws Exception {
        String admin = login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD);
        String buyerEmail = registerBuyer();
        Map<String, String> staff = new LinkedHashMap<>();
        staff.put("/api/admin/sellers", "SELLER");
        staff.put("/api/admin/supervisors", "SUPERVISOR");
        staff.put("/api/admin/logistics-operators", "LOGISTICS_OPERATOR");

        assertEquals("BUYER", call("GET", "/api/me", login(buyerEmail, PASSWORD), null).json().get("role").asString());
        for (Map.Entry<String, String> entry : staff.entrySet()) {
            String email = createStaff(admin, entry.getKey());
            Reply me = call("GET", "/api/me", login(email, PASSWORD), null);
            assertEquals(200, me.status());
            assertEquals(entry.getValue(), me.json().get("role").asString());
        }
        assertEquals("ADMINISTRATOR", call("GET", "/api/me", admin, null).json().get("role").asString());
    }

    @Test
    void aProductGoesThroughItsLifecycle() throws Exception {
        String seller = login(createStaff(login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD), "/api/admin/sellers"),
                PASSWORD);
        String buyer = login(registerBuyer(), PASSWORD);
        String warehouseId = call("POST", "/api/seller/warehouses", seller,
                "{\"name\":\"North\",\"address\":\"Calle 1\"}").expect(201).json().get("identifier").asString();
        String productId = publish(seller, "Desk Lamp", 25000);
        call("POST", "/api/seller/inventory/inbound", seller, inbound(productId, warehouseId, 5)).expect(200);

        assertTrue(publishedIds(buyer).contains(productId));
        call("PATCH", "/api/seller/products/" + productId + "/status", seller, "{\"status\":\"SUSPENDED\"}")
                .expect(200);
        assertFalse(publishedIds(buyer).contains(productId));
        call("PATCH", "/api/seller/products/" + productId + "/status", seller, "{\"status\":\"PUBLISHED\"}")
                .expect(200);
        assertTrue(publishedIds(buyer).contains(productId));
        JsonNode stock = call("GET", "/api/seller/products/" + productId + "/inventory", seller, null).expect(200)
                .json().get(0);
        assertEquals(5, stock.get("availableQuantity").asInt());
    }

    @Test
    void aCompleteSaleEndsDeliveredWithAnInvoiceAndCanBeReturnedAndRefunded() throws Exception {
        String admin = login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD);
        String logistics = login(DemoDataSeeder.LOGISTICS_EMAIL, DEMO_PASSWORD);
        String buyer = login(registerBuyer(), PASSWORD);
        String productId = DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(1);

        call("POST", "/api/buyer/cart/items", buyer, "{\"productId\":\"" + productId + "\",\"quantity\":2}")
                .expect(200);
        JsonNode order = call("POST", "/api/buyer/orders", buyer, null).expect(201).json();
        String orderId = order.get("identifier").asString();
        assertEquals("PENDING_PAYMENT", order.get("status").asString());

        call("POST", "/api/admin/orders/" + orderId + "/payment", admin, null).expect(200);
        String shipmentId = "SHP-" + UUID.randomUUID().toString().substring(0, 8);
        call("POST", "/api/logistics/shipments", logistics,
                "{\"shipmentId\":\"" + shipmentId + "\",\"orderId\":\"" + orderId + "\"}").expect(201);
        for (String step : List.of("pack", "dispatch", "deliver")) {
            call("POST", "/api/logistics/shipments/" + shipmentId + "/" + step, logistics, null).expect(200);
        }

        assertEquals("DELIVERED", call("GET", "/api/buyer/orders/" + orderId, buyer, null).expect(200).json()
                .get("status").asString());
        JsonNode invoice = call("GET", "/api/buyer/orders/" + orderId + "/invoice", buyer, null).expect(200).json();
        assertEquals(0, invoice.get("totalAmount").get("amount").decimalValue().compareTo(
                new java.math.BigDecimal("90000")));
        assertEquals("DELIVERED", call("GET", "/api/buyer/orders/" + orderId + "/shipment", buyer, null)
                .expect(200).json().get("status").asString());

        String returnId = "RET-" + UUID.randomUUID().toString().substring(0, 8);
        call("POST", "/api/buyer/orders/" + orderId + "/returns", buyer,
                "{\"returnRequestId\":\"" + returnId + "\",\"reason\":\"Wrong colour\"}").expect(201);
        assertEquals("APPROVED", call("POST", "/api/admin/returns/" + returnId + "/approve", admin, null).expect(200)
                .json().get("status").asString());
        assertEquals("PROCESSED", call("POST", "/api/admin/refunds/REF-" + returnId + "/process", admin, null)
                .expect(200).json().get("status").asString());
        call("POST", "/api/admin/refunds/REF-" + returnId + "/process", admin, null).expect(409);
    }

    @Test
    void aDeclinedPaymentIs402AndTheBuyerCanPayAgainOnlyOnce() throws Exception {
        String buyer = login(registerBuyer(), PASSWORD);
        call("POST", "/api/buyer/cart/items", buyer,
                "{\"productId\":\"" + DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(0) + "\",\"quantity\":1}").expect(200);
        String orderId = call("POST", "/api/buyer/orders", buyer, null).expect(201).json().get("identifier")
                .asString();

        call("POST", "/api/buyer/orders/" + orderId + "/payment", buyer, "{\"paymentToken\":\"DECLINED\"}")
                .expect(402);
        assertEquals("PENDING_PAYMENT", call("GET", "/api/buyer/orders/" + orderId, buyer, null).json()
                .get("status").asString());
        call("GET", "/api/buyer/orders/" + orderId + "/invoice", buyer, null).expect(404);
        boolean declinedAudited = false;
        for (JsonNode entry : call("GET", "/api/supervisor/audit-logs",
                login(DemoDataSeeder.SUPERVISOR_EMAIL, DEMO_PASSWORD), null).expect(200).json()) {
            declinedAudited |= "PAYMENT_DECLINED".equals(entry.get("operationType").asString())
                    && orderId.equals(entry.get("affectedEntityId").asString());
        }
        assertTrue(declinedAudited, "the declined attempt is audited");

        assertEquals("PAID", call("POST", "/api/buyer/orders/" + orderId + "/payment", buyer,
                "{\"paymentToken\":\"tok_test_visa\"}").expect(200).json().get("status").asString());
        call("GET", "/api/buyer/orders/" + orderId + "/invoice", buyer, null).expect(200);
        call("POST", "/api/buyer/orders/" + orderId + "/payment", buyer, "{\"paymentToken\":\"tok_test_visa\"}")
                .expect(409);
    }

    @Test
    void aDigitalOnlyOrderIsDeliveredWhenPaid() throws Exception {
        String buyer = login(registerBuyer(), PASSWORD);
        call("POST", "/api/buyer/cart/items", buyer,
                "{\"productId\":\"" + DemoDataSeeder.DIGITAL_PRODUCT_ID + "\",\"quantity\":1}").expect(200);
        String orderId = call("POST", "/api/buyer/orders", buyer, null).expect(201).json().get("identifier")
                .asString();

        assertEquals("DELIVERED", call("POST", "/api/buyer/orders/" + orderId + "/payment", buyer,
                "{\"paymentToken\":\"tok_test_visa\"}").expect(200).json().get("status").asString());
        call("GET", "/api/buyer/orders/" + orderId + "/invoice", buyer, null).expect(200);
    }

    @Test
    void aUserEditsTheirProfileAndABuyerEmptiesTheCart() throws Exception {
        String email = registerBuyer();
        String buyer = login(email, PASSWORD);

        JsonNode updated = call("PUT", "/api/me", buyer, "{\"fullName\":\"Renamed Buyer\",\"primaryAddress\":"
                + "\"Calle 50\",\"email\":\"other@e2e.test\"}").expect(200).json();
        assertEquals("Renamed Buyer", updated.get("fullName").asString());
        assertEquals("Calle 50", updated.get("primaryAddress").asString());
        assertEquals(email, call("GET", "/api/me", buyer, null).expect(200).json().get("email").asString());

        call("POST", "/api/buyer/cart/items", buyer,
                "{\"productId\":\"" + DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(0) + "\",\"quantity\":1}").expect(200);
        assertTrue(call("DELETE", "/api/buyer/cart/items", buyer, null).expect(200).json().get("lines").isEmpty());
        assertTrue(call("GET", "/api/buyer/cart", buyer, null).expect(200).json().get("lines").isEmpty());
    }

    @Test
    void reportsAndListsReflectASale() throws Exception {
        String admin = login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD);
        String seller = login(DemoDataSeeder.SELLER_EMAIL, DEMO_PASSWORD);
        String buyer = login(registerBuyer(), PASSWORD);
        long unitsBefore = call("GET", "/api/seller/reports/performance", seller, null).expect(200).json()
                .get("unitsSold").asLong();

        call("POST", "/api/buyer/cart/items", buyer,
                "{\"productId\":\"" + DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(2) + "\",\"quantity\":1}").expect(200);
        String orderId = call("POST", "/api/buyer/orders", buyer, null).expect(201).json().get("identifier")
                .asString();
        call("POST", "/api/buyer/orders/" + orderId + "/payment", buyer, "{\"paymentToken\":\"tok_test_visa\"}")
                .expect(200);

        assertEquals(unitsBefore + 1, call("GET", "/api/seller/reports/performance", seller, null).expect(200)
                .json().get("unitsSold").asLong());
        assertTrue(call("GET", "/api/admin/reports/commercial", admin, null).expect(200).json()
                .get("ordersByStatus").get("PAID").asLong() >= 1);
        assertEquals(orderId, call("GET", "/api/seller/orders?size=1", seller, null).expect(200).json()
                .get("items").get(0).get("identifier").asString(), "most recent first");
        call("GET", "/api/admin/reports/inventory?lowStockThreshold=100", admin, null).expect(200);
    }

    @Test
    void everyRoleIsForbiddenOnTheRoutesOfTheOthers() throws Exception {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("BUYER", login(DemoDataSeeder.BUYER_EMAIL, DEMO_PASSWORD));
        tokens.put("SELLER", login(DemoDataSeeder.SELLER_EMAIL, DEMO_PASSWORD));
        tokens.put("ADMINISTRATOR", login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD));
        tokens.put("LOGISTICS_OPERATOR", login(DemoDataSeeder.LOGISTICS_EMAIL, DEMO_PASSWORD));
        tokens.put("SUPERVISOR", login(DemoDataSeeder.SUPERVISOR_EMAIL, DEMO_PASSWORD));
        Map<String, String> routes = Map.of(
                "BUYER", "/api/buyer/orders",
                "SELLER", "/api/seller/products",
                "ADMINISTRATOR", "/api/admin/operations",
                "LOGISTICS_OPERATOR", "/api/logistics/products/PRD-DEMO-1/inventory",
                "SUPERVISOR", "/api/supervisor/products");

        for (Map.Entry<String, String> route : routes.entrySet()) {
            assertEquals(401, call("GET", route.getValue(), null, null).status(), route.getValue());
            for (Map.Entry<String, String> token : tokens.entrySet()) {
                int expected = token.getKey().equals(route.getKey()) ? 200 : 403;
                assertEquals(expected, call("GET", route.getValue(), token.getValue(), null).status(),
                        token.getKey() + " on " + route.getValue());
            }
        }
    }

    @Test
    void aTakenIdentifierIs409AndChangesNothing() throws Exception {
        String intruder = login(createStaff(login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD), "/api/admin/sellers"),
                PASSWORD);
        String existing = DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(0);

        call("POST", "/api/seller/products", intruder, "{\"identifier\":\"" + existing + "\",\"name\":\"Stolen\","
                + "\"productType\":\"PHYSICAL\",\"price\":{\"amount\":1,\"currency\":\"COP\"}}").expect(409);

        JsonNode stored = call("GET", "/api/supervisor/products/" + existing,
                login(DemoDataSeeder.SUPERVISOR_EMAIL, DEMO_PASSWORD), null).expect(200).json();
        assertEquals("Mechanical Keyboard", stored.get("name").asString());
        assertEquals(DemoDataSeeder.SELLER_ID, stored.get("sellerId").asString());
    }

    @Test
    void logoutAndPasswordChangeRevokeEveryEarlierToken() throws Exception {
        String email = registerBuyer();
        String first = login(email, PASSWORD);
        call("POST", "/api/buyer/logout", first, null).expect(204);
        call("GET", "/api/me", first, null).expect(401);

        String second = login(email, PASSWORD);
        call("POST", "/api/buyer/password", second,
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"E2e-New-Password-2\"}").expect(204);
        call("GET", "/api/me", second, null).expect(401);
        assertEquals(401, loginStatus(email, PASSWORD));
        assertEquals(200, loginStatus(email, "E2e-New-Password-2"));
    }

    @Test
    void sixtyBuyersRacingOverHttpForTenUnitsGetExactlyTenOrders() throws Exception {
        String seller = login(createStaff(login(DemoDataSeeder.ADMIN_EMAIL, DEMO_PASSWORD), "/api/admin/sellers"),
                PASSWORD);
        String warehouseId = call("POST", "/api/seller/warehouses", seller,
                "{\"name\":\"Race\",\"address\":\"Calle 2\"}").expect(201).json().get("identifier").asString();
        String productId = publish(seller, "Limited Edition", 99000);
        call("POST", "/api/seller/inventory/inbound", seller, inbound(productId, warehouseId, 10)).expect(200);

        ExecutorService pool = Executors.newFixedThreadPool(60);
        try {
            List<Future<String>> buyers = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                buyers.add(pool.submit(() -> {
                    String token = login(registerBuyer(), PASSWORD);
                    call("POST", "/api/buyer/cart/items", token,
                            "{\"productId\":\"" + productId + "\",\"quantity\":1}").expect(200);
                    return token;
                }));
            }
            List<String> tokens = new ArrayList<>();
            for (Future<String> buyer : buyers) {
                tokens.add(buyer.get(120, TimeUnit.SECONDS));
            }

            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> confirmations = new ArrayList<>();
            for (String token : tokens) {
                confirmations.add(pool.submit(() -> {
                    go.await();
                    return call("POST", "/api/buyer/orders", token, null).status();
                }));
            }
            go.countDown();
            int accepted = 0;
            for (Future<Integer> confirmation : confirmations) {
                int status = confirmation.get(120, TimeUnit.SECONDS);
                if (status == 201) {
                    accepted++;
                } else {
                    assertEquals(409, status, "a rejected confirmation is a conflict, never a 5xx");
                }
            }
            assertEquals(10, accepted);
        } finally {
            pool.shutdownNow();
        }
        JsonNode stock = call("GET", "/api/seller/products/" + productId + "/inventory", seller, null).json().get(0);
        assertEquals(0, stock.get("availableQuantity").asInt());
        assertEquals(10, stock.get("reservedQuantity").asInt());
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private record Reply(int status, String body) {

        JsonNode json() {
            return JSON.readTree(body);
        }

        Reply expect(int expected) {
            assertEquals(expected, status, () -> "unexpected status, body: " + body);
            return this;
        }
    }

    private Reply call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, json != null ? HttpRequest.BodyPublishers.ofString(json)
                        : HttpRequest.BodyPublishers.noBody());
        if (json != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    private String login(String email, String password) throws Exception {
        return call("POST", "/api/auth/login", null, credentials(email, password)).expect(200).json()
                .get("accessToken").asString();
    }

    private int loginStatus(String email, String password) throws Exception {
        return call("POST", "/api/auth/login", null, credentials(email, password)).status();
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    /** Registers a new buyer with {@link #PASSWORD}; returns its email. */
    private String registerBuyer() throws Exception {
        String id = "B-" + UUID.randomUUID().toString().substring(0, 8);
        call("POST", "/api/auth/register", null, "{\"identifier\":\"" + id + "\",\"fullName\":\"Buyer " + id
                + "\",\"email\":\"" + id + "@e2e.test\",\"primaryAddress\":\"Calle 3\",\"password\":\"" + PASSWORD
                + "\"}").expect(201);
        return id.toLowerCase() + "@e2e.test";
    }

    /** Creates a staff user through {@code route}; returns its email. */
    private String createStaff(String adminToken, String route) throws Exception {
        String id = "S-" + UUID.randomUUID().toString().substring(0, 8);
        call("POST", route, adminToken, "{\"identifier\":\"" + id + "\",\"fullName\":\"Staff " + id
                + "\",\"email\":\"" + id + "@e2e.test\",\"password\":\"" + PASSWORD + "\"}").expect(201);
        return id.toLowerCase() + "@e2e.test";
    }

    private String publish(String sellerToken, String name, long price) throws Exception {
        return call("POST", "/api/seller/products", sellerToken, "{\"name\":\"" + name + "\","
                + "\"productType\":\"PHYSICAL\",\"price\":{\"amount\":" + price + ",\"currency\":\"COP\"}}")
                .expect(201).json().get("identifier").asString();
    }

    private static String inbound(String productId, String warehouseId, int quantity) {
        return "{\"productId\":\"" + productId + "\",\"warehouseId\":\"" + warehouseId + "\",\"quantity\":" + quantity
                + "}";
    }

    private List<String> publishedIds(String buyerToken) throws Exception {
        List<String> ids = new ArrayList<>();
        call("GET", "/api/buyer/products", buyerToken, null).expect(200).json()
                .forEach(product -> ids.add(product.get("identifier").asString()));
        return ids;
    }
}
