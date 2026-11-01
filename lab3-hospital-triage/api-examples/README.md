# REST API examples

Mock API base URL: `http://localhost:8103`. These are real responses from the running mock API.

## Normal request

```text
GET /api/scheduling/slots?department=CARDIOLOGY&date=2026-11-20
```

```text
HTTP 200
{"slots":[{"slotId":"SLOT-101","time":"2026-11-20T09:30","session":"MORNING"},{"slotId":"SLOT-102","time":"2026-11-20T14:00","session":"AFTERNOON"}],"date":"2026-11-20","department":"CARDIOLOGY"}
```

## Alternative outcome

```text
GET /api/scheduling/slots?department=DERMATOLOGY&date=2026-11-20
```

```text
HTTP 200
{"slots":[],"date":"2026-11-20","department":"DERMATOLOGY"}
```

## Simulating an outage

Turn an outage on, and off again after inspecting the incident:

```bash
curl -X POST "http://localhost:8103/admin/fault?name=scheduling&down=true"
```

```bash
curl -X POST "http://localhost:8103/admin/fault?name=scheduling&down=false"
```
