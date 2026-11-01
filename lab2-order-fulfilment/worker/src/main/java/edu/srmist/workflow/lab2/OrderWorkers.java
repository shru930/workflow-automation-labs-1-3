package edu.srmist.workflow.lab2;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** External job workers for the order-processing process. */
@Component
public class OrderWorkers {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkers.class);
    private static final Set<String> DELIVERY = Set.of("STANDARD", "EXPRESS");

    /** Simulated catalogue: product ID -> [unit price, units in stock]. */
    private static final Map<String, int[]> CATALOGUE = Map.of(
            "SKU-1001", new int[] {1500, 50},
            "SKU-2002", new int[] {75000, 5},
            "SKU-3003", new int[] {2500, 0},
            "SKU-4004", new int[] {300, 200});

    private final FaultSwitch faults;

    public OrderWorkers(FaultSwitch faults) {
        this.faults = faults;
    }

    @JobWorker(type = "lab2.validate-order")
    public Map<String, Object> validateOrder(ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        List<String> errors = new ArrayList<>();

        if (text(v, "orderId").isBlank()) {
            errors.add("orderId is required");
        }
        if (text(v, "customerRef").isBlank()) {
            errors.add("customerRef is required");
        }
        if (text(v, "productId").isBlank()) {
            errors.add("productId is required");
        }
        if (number(v, "quantity") < 1) {
            errors.add("quantity must be at least 1");
        }
        if (!DELIVERY.contains(text(v, "deliveryPreference"))) {
            errors.add("deliveryPreference must be STANDARD or EXPRESS");
        }
        if (text(v, "paymentToken").isBlank()) {
            errors.add("paymentToken is required");
        }

        log.info("Validated order {}: errors={}", v.get("orderId"), errors);
        return Map.of("orderValid", errors.isEmpty(), "orderErrors", errors);
    }

    /** An outage throws, so Camunda retries the job and then raises an incident. */
    @JobWorker(type = "lab2.check-inventory")
    public Map<String, Object> checkInventory(ActivatedJob job) {
        if (faults.isDown("inventory")) {
            throw new IllegalStateException("Inventory service is unavailable (simulated outage)");
        }

        Map<String, Object> v = job.getVariablesAsMap();
        int[] item = CATALOGUE.get(text(v, "productId"));
        int inStock = item == null ? 0 : item[1];
        boolean enough = inStock >= number(v, "quantity");

        return Map.of(
                "stockStatus", enough ? "AVAILABLE" : "INSUFFICIENT",
                "availableQuantity", inStock,
                "unitPrice", item == null ? 0 : item[0]);
    }

    @JobWorker(type = "lab2.calculate-total")
    public Map<String, Object> calculateTotal(ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        double total = number(v, "unitPrice") * number(v, "quantity");
        return Map.of("orderTotal", total);
    }

    @JobWorker(type = "lab2.arrange-fulfilment")
    public Map<String, Object> arrangeFulfilment(ActivatedJob job) {
        String orderId = text(job.getVariablesAsMap(), "orderId");
        return Map.of("shipmentId", "SHIP-" + orderId, "orderStatus", "FULFILLED");
    }

    private static String text(Map<String, Object> vars, String name) {
        Object value = vars.get(name);
        return value == null ? "" : value.toString();
    }

    private static double number(Map<String, Object> vars, String name) {
        Object value = vars.get(name);
        return value instanceof Number n ? n.doubleValue() : 0;
    }
}
