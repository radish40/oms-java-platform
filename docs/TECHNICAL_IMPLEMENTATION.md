# Technical Implementation Registry

This document records the Java Platform service implementation structure for
maintainers and release reviewers.

## Runtime Baseline

| Component | Implementation |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| HTTP client | OkHttp |
| JSON serialization | Jackson |
| Persistence access | Spring JDBC and mapper adapters |
| Security | Bearer token auth, HMAC filter, RBAC aspect |
| Testing | JUnit 5, Spring MVC tests, integration smoke tests |
| Coverage | JaCoCo check and report in `mvn verify` |

## Package Responsibilities

| Package | Responsibility |
| --- | --- |
| `controller` | TypeScript-compatible REST endpoints and HTTP status behavior. |
| `service` | Business flow, validation, runtime orchestration, and audit writes. |
| `repository` | Database access and runtime-backed data adapters. |
| `client` | HTTP adapters for runtime integration. |
| `dto` | Stable public request and response records. |
| `entity` | Internal persistence-oriented records. |
| `security` | Token handling, HMAC authentication, password hashing, and permission checks. |
| `exception` | Structured error envelopes and global exception mapping. |
| `config` | Spring configuration and externalized runtime settings. |

## Security Design

- Secrets and passwords are provided through environment variables.
- Main configuration does not embed production passwords or signing keys.
- Permission checks use `@RequiresPermission` and `PermissionAspect` to protect
  controller/service behavior.
- Authentication and authorization failures preserve client-compatible status
  codes and error payloads.
- `pre-push-check.sh` runs public-release checks and can layer in a local
  `.git/info/sensitive-denylist`. The local denylist is not versioned, and the
  script does not print its content.

## Runtime Integration

- `MODEL_RUNTIME_URL` is the preferred runtime base URL.
- A legacy environment-variable alias remains accepted for local compatibility.
- Runtime failures are mapped to structured upstream error responses.
- Health checks report both platform status and runtime reachability where
  applicable.
- Runtime contract links include `/eval/case-bank*` and the frontend-facing
  `/knowledge/entries*` proxy. The latter maps to Python Runtime
  `/eval/knowledge-entry*` to keep the web client on the Java Platform boundary.
- Runtime JSON proxy paths return `JsonNode`/text payloads without DTO narrowing,
  so `snake_case` contract fields such as `prompt_version` and
  `tool_schema_version` are preserved end to end.

## Data and API Compatibility

- Public DTOs preserve stable field names and response envelopes.
- Controller mappings preserve the platform path layout.
- Error responses are centralized through `GlobalExceptionHandler`.
- Upstream runtime errors continue to be normalized to
  `{"error":{"code","message","details"}}` by `BusinessException` handling.
- Contract coverage is documented in `docs/API_CONTRACT.md`.
- Cross-service core DTOs are documented in `docs/CORE_SCHEMAS.md`.
- Trace, span, metric, and structured log naming rules are documented in
  `docs/OBSERVABILITY_NAMING.md`.

## Release Verification

Run before release:

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
git status --short --untracked-files=all
```

Expected release criteria:

- Tests pass with no failures or errors.
- JaCoCo coverage checks pass.
- Sanitization checks pass for current tree and reachable Git history.
- The working tree is clean.
- The release tag resolves to the intended release commit.
