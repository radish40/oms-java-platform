# Business Functions Registry

This document records the user-visible business capabilities implemented by
the Java Platform service. It is intentionally product-neutral and contains no
private deployment details.

## Scope

The Java Platform exposes the HTTP API surface used by the web client and by
service-to-service integrations. It replaces the legacy platform boundary while
keeping request paths, methods, response shapes, permission behavior, and error
envelopes compatible.

## Capability Inventory

| Area | Capability | Public endpoints | Notes |
| --- | --- | --- | --- |
| Authentication | Login and current-user lookup | `POST /auth/login`, `GET /auth/me` | Returns bearer token and user permission data. |
| RBAC | Role, user, and permission administration | `GET /admin/rbac`, `POST /admin/users` | Admin-only operations guarded by permission checks. |
| Diagnosis | Diagnosis run listing and detail lookup | `GET /diagnosis/runs`, `GET /diagnosis/runs/{run_id}` | Proxies runtime-backed diagnosis data. |
| Feedback | Feedback capture, listing, and summary | `POST /diagnosis/feedback`, `GET /diagnosis/feedback`, `GET /diagnosis/feedback/summary` | Supports review workflows and aggregate visibility. |
| Judge reports | Judge report listing, summaries, and execution | `GET /diagnosis/judge-reports`, `GET /diagnosis/judge-reports/summary`, `POST /diagnosis/judge-reports/run`, `POST /diagnosis/judge-reports/batch-run` | Keeps report operations behind review permissions. |
| Sessions | Conversation/session listing, detail, and deletion | `GET /sessions`, `GET /sessions/{session_id}`, `DELETE /sessions/{session_id}` | Preserves client-facing session response fields. |
| Evaluation | Candidate review and export | `GET /eval/candidates`, `POST /eval/candidates/review`, `GET /eval/candidates/export` | Export remains NDJSON-compatible. |
| Model administration | Model config, binding, cache refresh, and test calls | `/admin/model-configs*`, `/admin/model-bindings*` | Admin-only configuration and runtime validation flow. |
| Model options | Chat model option discovery | `GET /model-options/chat` | Reads available model options through the runtime boundary. |
| Operations | Health and debug visibility | `GET /health`, `GET /ops/debug` | Health is public; debug data requires operations permission. |

## Compatibility Commitments

- Keep HTTP methods and paths stable for the registered API groups.
- Keep request and response field names in `snake_case` where exposed publicly.
- Keep error responses in the `{"error":{"code","message","details"}}` shape.
- Return `401 UNAUTHORIZED` for missing authentication.
- Return `403 FORBIDDEN` for missing permissions with the missing permission in
  `error.details.permission`.
- Preserve NDJSON export behavior for evaluation candidates.

## Verification

Business behavior is covered by controller and integration tests under
`src/test/java/com/example/oms/platform/controller`. The release verification
command is:

```bash
mvn -s maven-central-settings.xml verify
```

