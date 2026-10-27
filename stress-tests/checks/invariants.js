// Data-integrity invariant checks for the Nexus Market stress database.
// Usage:
//   mongosh "mongodb://localhost:27017/nexusmarket_stress" checks/invariants.js
// Read-only: it never writes. Every section prints PASS/FAIL and the offending data.
// These invariants come from FRONT D (point 6) plus general correctness rules.

function hr(t) { print("\n==================== " + t + " ===================="); }
let failures = 0;
function assert(name, ok, detail) {
    print((ok ? "[PASS] " : "[FAIL] ") + name + (detail ? "  " + detail : ""));
    if (!ok) failures++;
}

// 1) No negative inventory quantities anywhere.
hr("1. inventory quantities are never negative");
let neg = db.inventory_items.find({ $or: [
    { availableQuantity: { $lt: 0 } }, { reservedQuantity: { $lt: 0 } }, { damagedQuantity: { $lt: 0 } }
] }).toArray();
neg.forEach(i => print("   NEG " + i._id + " avail=" + i.availableQuantity + " reserved=" + i.reservedQuantity + " damaged=" + i.damagedQuantity));
assert("no negative inventory quantities", neg.length === 0, "negatives=" + neg.length);

// 2) reserved quantity == sum of line quantities of OPEN orders (PENDING_PAYMENT, PAID) per (product, warehouse).
//    Once an order is SHIPPED the reservation is converted to an outbound, so only PENDING_PAYMENT/PAID count.
hr("2. reservedQuantity matches open-order demand per (product,warehouse)");
let demand = {}; // key "productId|warehouseId" -> units
db.orders.find({ status: { $in: ["PENDING_PAYMENT", "PAID"] } }).forEach(o => {
    (o.lines || []).forEach(l => {
        let k = l.productId + "|" + l.warehouseId;
        demand[k] = (demand[k] || 0) + l.quantity;
    });
});
let mismatches = 0, checked = 0;
db.inventory_items.find({}).forEach(it => {
    let k = it.productId + "|" + it.warehouseId;
    let expected = demand[k] || 0;
    checked++;
    if (it.reservedQuantity !== expected) {
        mismatches++;
        if (mismatches <= 20) print("   MISMATCH " + it._id + " reserved=" + it.reservedQuantity + " openOrderDemand=" + expected);
    }
});
assert("reserved == open-order demand", mismatches === 0, "mismatches=" + mismatches + "/" + checked +
    "  (a large number here is the fingerprint of the overselling / lost-update race)");

// 3) No duplicate e-mail or identifier among users.
hr("3. users have unique e-mail and identifier");
let dupEmail = db.users.aggregate([
    { $group: { _id: "$email", n: { $sum: 1 } } }, { $match: { n: { $gt: 1 } } }
]).toArray();
dupEmail.forEach(d => print("   DUP email " + d._id + " x" + d.n));
assert("unique e-mail", dupEmail.length === 0, "duplicates=" + dupEmail.length);
// identifier is the _id, so it is unique by construction; verify no null id slipped in.
assert("no null user identifier", db.users.countDocuments({ _id: null }) === 0);

// 4) Each invoice total equals the sum of its order's lines (unitPrice.amount * quantity).
hr("4. invoice total == sum of order line amounts");
let badInvoice = 0, invChecked = 0;
db.invoices.find({}).forEach(inv => {
    let o = db.orders.findOne({ _id: inv.orderId });
    if (!o) { print("   invoice " + inv._id + " references missing order " + inv.orderId); badInvoice++; return; }
    let sum = 0;
    (o.lines || []).forEach(l => { sum += parseFloat(l.unitPrice.amount.toString()) * l.quantity; });
    let total = parseFloat(inv.totalAmount.amount.toString());
    invChecked++;
    if (Math.abs(sum - total) > 0.0001) { badInvoice++; print("   MISMATCH invoice " + inv._id + " total=" + total + " lineSum=" + sum); }
});
assert("invoice totals consistent", badInvoice === 0, "bad=" + badInvoice + "/" + invChecked);

// 5) At most one refund per return request.
hr("5. one refund per return request");
let dupRefund = db.refunds.aggregate([
    { $group: { _id: "$returnRequestId", n: { $sum: 1 } } }, { $match: { n: { $gt: 1 } } }
]).toArray();
dupRefund.forEach(d => print("   return " + d._id + " has " + d.n + " refunds"));
assert("<=1 refund per return", dupRefund.length === 0, "returns-with-multiple-refunds=" + dupRefund.length);

// 6) Every Operation has at least one matching AuditLog (same type + performer + affected entity).
hr("6. every Operation has a matching AuditLog");
let opCount = db.operations.countDocuments();
let auditCount = db.audit_logs.countDocuments();
print("   operations=" + opCount + " audit_logs=" + auditCount);
let orphan = 0, sampled = 0;
db.operations.find({}).limit(5000).forEach(op => {
    sampled++;
    let m = db.audit_logs.countDocuments({
        operationType: op.operationType, performedById: op.performedById, affectedEntityId: op.affectedEntityId
    });
    if (m === 0) { orphan++; if (orphan <= 10) print("   ORPHAN op " + op.operationType + " by " + op.performedById + " on " + op.affectedEntityId); }
});
assert("no orphan operations (sampled)", orphan === 0, "orphans=" + orphan + "/" + sampled);

// 7) No password / hash / token material in audit details or operation records.
hr("7. no secrets in audit logs or operations");
let secretRe = /pass|hash|secret|token|bearer|\$2[aby]\$/i; // $2a$/$2b$ = BCrypt prefix
let leaks = 0;
function scan(coll) {
    db[coll].find({}).forEach(d => {
        let s = JSON.stringify(d.details || d);
        // ignore the field names operationType/affectedEntityType etc.; look at values and detail keys
        if (d.details) {
            for (let k in d.details) {
                if (secretRe.test(k) || secretRe.test(String(d.details[k]))) { leaks++; if (leaks <= 10) print("   LEAK " + coll + " " + d._id + " detail " + k + "=" + d.details[k]); }
            }
        }
        if (/\$2[aby]\$/.test(s)) { leaks++; print("   BCRYPT-LIKE in " + coll + " " + d._id); }
    });
}
scan("audit_logs"); scan("operations");
assert("no secrets in audit trail", leaks === 0, "leaks=" + leaks);

hr("SUMMARY");
print(failures === 0 ? "ALL INVARIANTS PASS" : (failures + " INVARIANT(S) FAILED"));
