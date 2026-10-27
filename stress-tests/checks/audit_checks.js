// Audit-trail coverage checks for the Nexus Market stress database (FRONT C).
// Usage:
//   mongosh "mongodb://localhost:27017/nexusmarket_stress" checks/audit_checks.js
// Read-only. Reports operation/audit counts, per-type distribution, and a sensitive-data scan.

function hr(t) { print("\n==================== " + t + " ===================="); }

hr("operation vs audit counts");
print("operations = " + db.operations.countDocuments());
print("audit_logs = " + db.audit_logs.countDocuments());

hr("operation type distribution (operations)");
db.operations.aggregate([
    { $group: { _id: "$operationType", n: { $sum: 1 } } }, { $sort: { n: -1 } }
]).forEach(r => print("  " + String(r._id).padEnd(26) + r.n));

hr("audit userRole distribution");
db.audit_logs.aggregate([
    { $group: { _id: "$userRole", n: { $sum: 1 } } }, { $sort: { n: -1 } }
]).forEach(r => print("  " + String(r._id).padEnd(20) + r.n));

hr("login auditing: USER_LOGIN present, no failed-login records");
print("  USER_LOGIN operations = " + db.operations.countDocuments({ operationType: "USER_LOGIN" }));
print("  (failed logins must NOT appear; only successful logins are audited)");

hr("state-changing sample: each has previousStatus/newStatus where expected");
["RETURN_APPROVAL", "REFUND_PROCESSING", "ORDER_STATUS_ADVANCE", "ORDER_CANCELLATION", "PRODUCT_STATUS_CHANGE"].forEach(t => {
    let a = db.audit_logs.findOne({ operationType: t });
    print("  " + t.padEnd(24) + (a ? JSON.stringify(a.details) : "(none recorded)"));
});

hr("sensitive-data scan across audit details");
let re = /pass|hash|secret|token|bearer|\$2[aby]\$/i;
let hits = 0;
db.audit_logs.find({}).forEach(d => {
    if (d.details) for (let k in d.details) {
        if (re.test(k) || re.test(String(d.details[k]))) { hits++; if (hits <= 20) print("  LEAK " + d._id + " " + k + "=" + d.details[k]); }
    }
});
print(hits === 0 ? "  no password/hash/token material found in audit details" : ("  " + hits + " POTENTIAL LEAK(S)"));

hr("done");
