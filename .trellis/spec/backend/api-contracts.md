# API And Cross-Layer Contract Guidelines

## Layer Boundaries

OMS-AI currently has three active layers:

- React Frontend: user entry point for diagnosis, reports, feedback, evaluation review, and RBAC management.
- Python Agent: public API gateway and AI execution runtime for `/diagnose`, SSE, LLM calls, RAG, tool calling, sessions, and diagnosis run records.
- Java Platform: typed business platform layer for stable business APIs, RBAC, audit, review permissions, sessions, feedback, model configuration, and operations endpoints.

Production browser traffic should reach Python Agent on `18010`. Platform APIs should be reached through `/platform/*` proxy unless an internal service explicitly calls Java Platform on `18020`.

## Route Ownership

- AI main flow stays in Python Agent: `/diagnose`, `/diagnose_sync`, `/diagnose/batch`, `/rag`.
- Stable business APIs use `/platform/*`: login, current user, sessions, diagnosis runs, feedback, evaluation candidates, RBAC, model config, ops/debug, and Platform health.
- Python Agent keeps `/platform/*` proxy support and forwards to `PLATFORM_PROXY_URL`, falling back to `PLATFORM_API_URL`, then `http://127.0.0.1:18020`.
- Do not move `/diagnose` SSE into Java Platform without a separate compatibility design, route migration plan, and regression tests.

## JSON Contract

- API JSON fields use `snake_case`.
- Services may add fields; callers must ignore unknown fields.
- Do not silently rename fields. Add the new field first, keep the old field for at least one release cycle, then update types, docs, and tests.
- Enum values may be added; frontend must display unknown enum values without a blank screen.
- Time fields use ISO-like strings such as `2026-06-22T12:30:00`.
- Optional strings prefer `""`, arrays prefer `[]`, and objects prefer `{}` unless absence must be explicit with `null`.
- Booleans must be `true` or `false`.

## Error Contract

Java Platform returns structured errors:

```json
{
  "error": {
    "code": "RUN_NOT_FOUND",
    "message": "Diagnosis run not found",
    "details": {}
  }
}
```

Older Python Agent endpoints may still return:

```json
{ "error": "Run not found" }
```

During compatibility work, frontend callers must tolerate both formats. Platform proxy changes should prefer the structured envelope unless an endpoint is intentionally pass-through.

## Change Checklist

When adding or changing a stable API, check:

- Human-readable API docs.
- Frontend types and callers.
- Java Platform controller/service/repository code.
- Python Agent proxy or compatibility endpoints.
- Route-level tests and frontend regression tests.
- Whether Trellis specs need updates.
