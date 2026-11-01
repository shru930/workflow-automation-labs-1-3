# REST API examples

Mock API base URL: `http://localhost:8101`. These are real responses from the running mock API.

## Normal request

```text
{"applicantId":"APP-GOOD","monthlyIncome":80000,"loanAmount":500000}
```

```text
HTTP 200
{"risk":"LOW","score":780,"applicantId":"APP-GOOD","assessedAt":"2026-11-01T12:04:01.045227Z"}
```

## Alternative outcome

```text
{"applicantId":"APP-503","monthlyIncome":80000,"loanAmount":500000}
```

```text
HTTP 503
{"error":"Credit bureau unavailable"}
```

## Simulating an outage

Turn an outage on, and off again after inspecting the incident:

```bash
curl -X POST "http://localhost:8101/admin/fault?name=validator&down=true"
```

```bash
curl -X POST "http://localhost:8101/admin/fault?name=validator&down=false"
```
