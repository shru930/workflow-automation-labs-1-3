package edu.srmist.workflow.lab1;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock credit bureau. Scores are synthetic and fixed per test applicant, so results can be repeated.
 * APP-503 returns a server error and APP-SLOW is slower than the connector's read timeout.
 */
@RestController
public class MockCreditApi {

    private static final Map<String, Integer> SCORES = Map.of(
            "APP-GOOD", 780,
            "APP-RECOVER", 790,
            "APP-LOW", 540,
            "APP-MID", 680);

    private final FaultSwitch faults;

    public MockCreditApi(FaultSwitch faults) {
        this.faults = faults;
    }

    @PostMapping("/api/credit/assess")
    public ResponseEntity<Map<String, Object>> assess(@RequestBody Map<String, Object> request) throws InterruptedException {
        String applicantId = String.valueOf(request.get("applicantId"));

        if (faults.isDown("credit") || "APP-503".equals(applicantId)) {
            return ResponseEntity.status(503).body(Map.of("error", "Credit bureau unavailable"));
        }
        if ("APP-SLOW".equals(applicantId)) {
            Thread.sleep(8000);
        }

        int score = SCORES.getOrDefault(applicantId, 650);
        String risk = score >= 720 ? "LOW" : score >= 600 ? "MEDIUM" : "HIGH";
        return ResponseEntity.ok(Map.of(
                "applicantId", applicantId,
                "score", score,
                "risk", risk,
                "assessedAt", Instant.now().toString()));
    }
}
