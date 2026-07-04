# Technical Implementation Registry

This document records the implementation structure of the Java Platform
service. It is intended for maintainers and release reviewers.

## Runtime Baseline

| Component | Implementation |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| HTTP client | OkHttp |
| JSON serialization | Jackson |
| Persistence access | Spring JDBC and mapper adapters |
| Security | Bearer-token authentication, HMAC filter, RBAC aspect |
| Tests | JUnit 5, Spring MVC tests, integration smoke tests |
| Coverage | JaCoCo check and report in `mvn verify` |

## Package Responsibilities

| Package | Responsibility |
| --- | --- |
| `controller` | TypeScript-compatible REST endpoints and HTTP status behavior. |
| `service` | Business workflows, validation, runtime orchestration, and audit writes. |
| `repository` | Database access and runtime-backed data adapters. |
| `client` | HTTP adapters for runtime integration. |
| `dto` | Stable public request and response records. |
| `entity` | Internal persistence-facing records. |
| `security` | Token handling, HMAC authentication, password hashing, and permission checks. |
| `exception` | Structured error envelope and global exception mapping. |
| `config` | Spring configuration and externalized runtime settings. |

## Security Design

- Secrets and passwords are supplied by environment variables.
- Production defaults for passwords and signing secrets are not embedded in the
  main configuration.
- Permission checks use `@RequiresPermission` and `PermissionAspect` for
  controller/service guardrails.
- Authentication failures and authorization failures keep client-compatible
  status codes and error payloads.
- `pre-push-check.sh` runs generic public-release checks plus an optional local
  denylist from `.git/info/sensitive-denylist`. The local denylist is not
  tracked and is not printed by the script.

## Runtime Integration

- `MODEL_RUNTIME_URL` is the preferred runtime base URL.
- A legacy environment-variable alias remains accepted for local compatibility.
- Runtime failures are mapped to structured upstream error responses.
- Health checks report both platform status and runtime reachability where
  applicable.

## Data and API Compatibility

- Public DTOs preserve stable field names and response envelopes.
- Controller mappings preserve the legacy platform path layout.
- Error responses are centralized through `GlobalExceptionHandler`.
- Contract coverage is documented in `docs/API_CONTRACT.md`.

## Release Verification

Run the following before publishing:

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

