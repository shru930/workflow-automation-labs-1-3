package edu.srmist.workflow.lab3;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** External job workers for the hospital appointment process. All patient data is synthetic. */
@Component
public class HospitalWorkers {

    private static final Logger log = LoggerFactory.getLogger(HospitalWorkers.class);
    private static final Set<String> PREFERENCES = Set.of("MORNING", "AFTERNOON");

    @JobWorker(type = "lab3.validate-registration")
    public Map<String, Object> validateRegistration(ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        List<String> errors = new ArrayList<>();

        if (!text(v, "patientRef").matches("PT-\\d{4}")) {
            errors.add("patientRef must look like PT-1234");
        }
        if (text(v, "department").isBlank()) {
            errors.add("department is required");
        }
        if (!PREFERENCES.contains(text(v, "appointmentPreference"))) {
            errors.add("appointmentPreference must be MORNING or AFTERNOON");
        }
        if (!isDate(text(v, "requestedDate"))) {
            errors.add("requestedDate must be a date such as 2026-11-20");
        }
        if (text(v, "symptomsSummary").isBlank()) {
            errors.add("symptomsSummary is required");
        }
        if (!(v.get("painLevel") instanceof Number pain) || pain.intValue() < 0 || pain.intValue() > 10) {
            errors.add("painLevel must be between 0 and 10");
        }

        log.info("Validated registration {}: errors={}", v.get("patientRef"), errors);
        return Map.of("registrationValid", errors.isEmpty(), "registrationErrors", errors);
    }

    /** Prepares the availability request that the REST Connector sends to the scheduling API. */
    @JobWorker(type = "lab3.request-availability")
    public Map<String, Object> requestAvailability(ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        return Map.of(
                "department", text(v, "department").toUpperCase(),
                "availabilityRequestId", "AVL-" + job.getProcessInstanceKey());
    }

    /** Books the first slot matching the patient's preference, or the first slot if none match. */
    @JobWorker(type = "lab3.book-appointment")
    @SuppressWarnings("unchecked")
    public Map<String, Object> bookAppointment(ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        List<Map<String, Object>> slots = (List<Map<String, Object>>) v.get("availableSlots");
        String preference = text(v, "appointmentPreference");

        Map<String, Object> chosen = slots.stream()
                .filter(slot -> preference.equals(slot.get("session")))
                .findFirst()
                .orElse(slots.get(0));

        return Map.of(
                "bookingStatus", "CONFIRMED",
                "bookedSlot", chosen.get("slotId"),
                "bookedTime", chosen.get("time"));
    }

    private static boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String text(Map<String, Object> vars, String name) {
        Object value = vars.get(name);
        return value == null ? "" : value.toString();
    }
}
