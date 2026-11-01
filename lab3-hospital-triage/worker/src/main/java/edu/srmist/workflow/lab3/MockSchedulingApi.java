package edu.srmist.workflow.lab3;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mock hospital scheduling API. The DERMATOLOGY department has no free slots, for testing. */
@RestController
public class MockSchedulingApi {

    private final FaultSwitch faults;

    public MockSchedulingApi(FaultSwitch faults) {
        this.faults = faults;
    }

    @GetMapping("/api/scheduling/slots")
    public ResponseEntity<Map<String, Object>> slots(@RequestParam String department, @RequestParam String date) {
        if (faults.isDown("scheduling")) {
            return ResponseEntity.status(503).body(Map.of("error", "Scheduling service unavailable"));
        }

        List<Map<String, String>> slots = "DERMATOLOGY".equalsIgnoreCase(department)
                ? List.of()
                : List.of(
                        Map.of("slotId", "SLOT-101", "time", date + "T09:30", "session", "MORNING"),
                        Map.of("slotId", "SLOT-102", "time", date + "T14:00", "session", "AFTERNOON"));

        return ResponseEntity.ok(Map.of("department", department, "date", date, "slots", slots));
    }
}
