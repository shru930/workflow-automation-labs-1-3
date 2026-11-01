# REST API examples

Mock API base URL: `http://localhost:8102`. These are real responses from the running mock API.

## Normal request

```text
{"orderId":"ORD-1001","amount":3000,"paymentToken":"tok_test_ok"}
```

```text
HTTP 200
{"transactionId":"TXN-c694252a","orderId":"ORD-1001","status":"APPROVED"}
```

## Alternative outcome

```text
{"orderId":"ORD-1003","amount":3000,"paymentToken":"tok_declined"}
```

```text
HTTP 200
{"transactionId":"NONE","orderId":"ORD-1003","status":"DECLINED"}
```

## Simulating an outage

Turn an outage on, and off again after inspecting the incident:

```bash
curl -X POST "http://localhost:8102/admin/fault?name=payment&down=true"
```

```bash
curl -X POST "http://localhost:8102/admin/fault?name=payment&down=false"
```
