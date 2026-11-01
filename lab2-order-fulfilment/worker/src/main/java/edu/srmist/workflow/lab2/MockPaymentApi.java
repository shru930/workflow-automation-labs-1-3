package edu.srmist.workflow.lab2;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Mock payment gateway. It accepts test tokens only and never handles real card details. */
@RestController
public class MockPaymentApi {

    private final FaultSwitch faults;

    public MockPaymentApi(FaultSwitch faults) {
        this.faults = faults;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<Map<String, Object>> pay(@RequestBody Map<String, Object> request) {
        if (faults.isDown("payment")) {
            return ResponseEntity.status(503).body(Map.of("error", "Payment service unavailable"));
        }

        String orderId = String.valueOf(request.get("orderId"));
        if ("tok_declined".equals(request.get("paymentToken"))) {
            return ResponseEntity.ok(Map.of("orderId", orderId, "status", "DECLINED", "transactionId", "NONE"));
        }
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(Map.of("orderId", orderId, "status", "APPROVED", "transactionId", transactionId));
    }
}
