package edu.srmist.workflow.lab1;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** External job worker for the loan-approval process. */
@Component
public class LoanWorkers {

    private static final Logger log = LoggerFactory.getLogger(LoanWorkers.class);

    private final FaultSwitch faults;

    public LoanWorkers(FaultSwitch faults) {
        this.faults = faults;
    }

    /**
     * Validates the applicant. A bad application is a normal business result, so it returns
     * validationPassed=false. An outage is a technical failure, so it throws and Camunda retries the job.
     */
    @JobWorker(type = "lab1.validate-applicant")
    public Map<String, Object> validateApplicant(ActivatedJob job) {
        if (faults.isDown("validator")) {
            throw new IllegalStateException("Applicant validation service is unavailable (simulated outage)");
        }

        Map<String, Object> v = job.getVariablesAsMap();
        List<String> errors = new ArrayList<>();

        if (text(v, "applicantId").isBlank()) {
            errors.add("applicantId is required");
        }
        if (text(v, "fullName").isBlank()) {
            errors.add("fullName is required");
        }
        double age = number(v, "age");
        if (age < 18 || age > 70) {
            errors.add("age must be between 18 and 70");
        }
        double income = number(v, "monthlyIncome");
        if (income <= 0) {
            errors.add("monthlyIncome must be greater than 0");
        }
        double amount = number(v, "loanAmount");
        if (amount < 10000 || amount > 5000000) {
            errors.add("loanAmount must be between 10000 and 5000000");
        }

        double ratio = income > 0
                ? BigDecimal.valueOf(amount / (income * 12)).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0;

        log.info("Validated {}: passed={} errors={}", v.get("applicantId"), errors.isEmpty(), errors);
        return Map.of(
                "validationPassed", errors.isEmpty(),
                "validationErrors", errors,
                "loanToAnnualIncome", ratio);
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
