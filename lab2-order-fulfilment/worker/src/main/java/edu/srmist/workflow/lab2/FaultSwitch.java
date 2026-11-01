package edu.srmist.workflow.lab2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Turns a simulated outage on or off, so a failure can be caused and then corrected
 * without restarting the worker.
 */
@Component
@RestController
public class FaultSwitch {

    private final Map<String, Boolean> faults = new ConcurrentHashMap<>();

    public boolean isDown(String name) {
        return faults.getOrDefault(name, false);
    }

    @PostMapping("/admin/fault")
    public Map<String, Boolean> set(@RequestParam String name, @RequestParam boolean down) {
        faults.put(name, down);
        return Map.copyOf(faults);
    }

    @GetMapping("/admin/fault")
    public Map<String, Boolean> list() {
        return Map.copyOf(faults);
    }
}
