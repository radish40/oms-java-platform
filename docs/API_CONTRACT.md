# Java Platform API Contract

Java Platform must keep the following public HTTP surface compatible with the
frontend and Python Runtime contracts.

## Endpoint List

| Domain | Method | Path |
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
| Evaluation runtime | GET | `/eval/case-bank` |
| Evaluation runtime | GET | `/eval/case-bank/{case_id}` |
| Evaluation runtime | POST | `/eval/case-bank` |
| Evaluation runtime | GET | `/eval/case-bank/export` |
| Knowledge runtime | GET | `/knowledge/entries` |
| Knowledge runtime | GET | `/knowledge/entries/{entry_id}` |
| Knowledge runtime | POST | `/knowledge/entries` |
| Admin models | GET | `/admin/model-configs` |
| Admin models | POST | `/admin/model-configs` |
| Admin models | DELETE | `/admin/model-configs/{provider}` |
| Admin models | POST | `/admin/model-configs/test` |
| Admin models | POST | `/admin/model-configs/refresh-cache` |
| Admin models | GET | `/admin/model-bindings` |
| Admin models | POST | `/admin/model-bindings` |
| Model options | GET | `/model-options/chat` |
| Observability | GET | `/observability/dashboard` |
| Runtime/Ops | GET | `/health` |
| Runtime/Ops | GET | `/ops/debug` |

## Compatibility Rules

- Preserve HTTP methods and paths.
- Preserve snake_case request and response fields.
- Preserve structured error envelopes: `{"error":{"code","message","details"}}`.
- Preserve permission failures as `401` for unauthenticated requests and `403`
  for missing permissions.
- Preserve NDJSON export behavior for evaluation candidates.
- Runtime-backed proxy responses are pass-through JSON. Java must not drop
  snake_case fields such as `prompt_version` or `tool_schema_version`.
- Diagnosis run detail responses may include `observability.trace` and
  `observability.spans`. Java must pass these fields through unchanged so the
  frontend and later telemetry exporters can use the same trace/span mapping.
- `/observability/dashboard` is the Platform-facing read-only observability
  route. It proxies Runtime `/observability/dashboard` and requires `menu:ops`.
- `/eval/case-bank*` is the Java Platform contract link to the runtime case
  bank. `/knowledge/entries*` is the frontend-facing Java path and proxies to
  the runtime `/eval/knowledge-entry*` contract.

## Core Schemas

Cross-service entity shapes for `diagnosis_run`, `diagnosis_step`, `evidence`,
`feedback`, `case`, `skill`, and `auth_user` are recorded in
`docs/CORE_SCHEMAS.md`. Runtime-backed diagnosis responses may include
additional fields, but consumers should rely only on documented public fields.

Feedback is currently `record_only`: the system stores, lists, and summarizes
feedback and uses it as review input, but feedback cannot automatically change
answers, knowledge, skills, order state, or customer communication.

## Cross-Language Fixtures

The Python Runtime owns the canonical fixture
`../oms-python-agent/contracts/platform-runtime-diagnosis-run.v1.json` for
`GET /diagnosis/runs/{run_id}`. Platform contract tests should load this fixture
when the Java test tree is available again and verify that proxy responses keep
`run`, `steps`, `summary`, `context`, and `observability` fields intact.

## Final Verification

`PlatformContractIntegrationTest` is the final integration smoke test. It
verifies representative auth, RBAC, diagnosis, session, evaluation, model
config, runtime, and structured error envelope behavior.

Run:

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```
