# Java Platform API Contract

The Java Platform keeps the Platform HTTP surface compatible across
the primary API groups below.

## Endpoint Inventory

| Area | Method | Path |
| --- | --- | --- |
| Auth | POST | `/auth/login` |
| Auth | GET | `/auth/me` |
| RBAC | GET | `/admin/rbac` |
| RBAC | POST | `/admin/users` |
| Diagnosis | GET | `/diagnosis/runs` |
| Diagnosis | GET | `/diagnosis/runs/{run_id}` |
| Feedback | POST | `/diagnosis/feedback` |
| Feedback | GET | `/diagnosis/feedback` |
| Feedback | GET | `/diagnosis/feedback/summary` |
| Judge reports | GET | `/diagnosis/judge-reports` |
| Judge reports | GET | `/diagnosis/judge-reports/summary` |
| Judge reports | POST | `/diagnosis/judge-reports/run` |
| Judge reports | POST | `/diagnosis/judge-reports/batch-run` |
| Sessions | GET | `/sessions` |
| Sessions | GET | `/sessions/{session_id}` |
| Sessions | DELETE | `/sessions/{session_id}` |
| Evaluation | GET | `/eval/candidates` |
| Evaluation | POST | `/eval/candidates/review` |
| Evaluation | GET | `/eval/candidates/export` |
| Admin models | GET | `/admin/model-configs` |
| Admin models | POST | `/admin/model-configs` |
| Admin models | DELETE | `/admin/model-configs/{provider}` |
| Admin models | POST | `/admin/model-configs/test` |
| Admin models | POST | `/admin/model-configs/refresh-cache` |
| Admin models | GET | `/admin/model-bindings` |
| Admin models | POST | `/admin/model-bindings` |
| Model options | GET | `/model-options/chat` |
| Runtime/Ops | GET | `/health` |
| Runtime/Ops | GET | `/ops/debug` |

## Compatibility Rules

- Preserve HTTP methods and paths.
- Preserve snake_case request and response fields.
- Preserve structured error envelopes: `{"error":{"code","message","details"}}`.
- Preserve permission failures as `401` for unauthenticated requests and `403`
  for missing permissions.
- Preserve NDJSON export behavior for evaluation candidates.

## Final Verification

`PlatformContractIntegrationTest` is the final integration smoke test. It
validates representative auth, RBAC, diagnosis, session, evaluation, model
configuration, runtime, and error-envelope behavior.

Run:

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```
