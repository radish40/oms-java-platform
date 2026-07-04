# OMS Java Platform

Spring Boot 3.2 / Java 21 implementation of the OMS platform API surface.

The service provides authentication, RBAC, diagnosis management, session
management, evaluation review, model configuration proxying, runtime health,
and operations endpoints. It is designed to remain HTTP-compatible with the
legacy TypeScript platform while using a Java service boundary.

## Verification

Use Java 21 and Maven 3.9+:

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```

The Maven verification runs unit tests, integration smoke tests, API contract
checks, packaging, and JaCoCo coverage gates.

## Required Configuration

All sensitive values must be provided through environment variables. Do not
commit `.env` files or real deployment values.

| Variable | Purpose | Example |
| --- | --- | --- |
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL database | `oms_db` |
| `DB_USER` | MySQL user | `oms_user` |
| `DB_PASSWORD` | MySQL password | `<set-via-env>` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | `<set-via-env>` |
| `PLATFORM_AUTH_SECRET` | HMAC token signing secret | `<random-32-byte-secret>` |
| `PLATFORM_DEFAULT_PASSWORD` | Bootstrap admin password | `<set-via-env>` |
| `PLATFORM_SUPPORT_PASSWORD` | Bootstrap support password | `<set-via-env>` |
| `MODEL_RUNTIME_URL` | Runtime service base URL | `http://localhost:18010` |

`PYTHON_RUNTIME_URL` is still accepted as a temporary compatibility alias for
older local environments. Prefer `MODEL_RUNTIME_URL` for new deployments.

## API Groups

- Auth and RBAC: `/auth/*`, `/admin/rbac`, `/admin/users`
- Diagnosis: `/diagnosis/runs`, `/diagnosis/feedback`, `/diagnosis/judge-reports`
- Sessions and evaluation: `/sessions`, `/eval/candidates`
- Admin model configuration: `/admin/model-configs`, `/admin/model-bindings`, `/model-options/chat`
- Runtime and operations: `/health`, `/ops/debug`

Detailed endpoint coverage is documented in `docs/API_CONTRACT.md`.
Business capability registration is documented in `docs/BUSINESS_FUNCTIONS.md`.
Technical implementation registration is documented in
`docs/TECHNICAL_IMPLEMENTATION.md`.

## Local Run

```bash
export DB_HOST=localhost
export DB_PASSWORD=<set-via-env>
export PLATFORM_AUTH_SECRET=<random-32-byte-secret>
export PLATFORM_DEFAULT_PASSWORD=<set-via-env>
export PLATFORM_SUPPORT_PASSWORD=<set-via-env>
export MODEL_RUNTIME_URL=http://localhost:18010
mvn spring-boot:run
```

Swagger UI is available at `http://localhost:18020/doc.html` when the service is
running.

## Project Layout

```text
src/main/java/com/example/oms/platform/
  client/       Runtime HTTP clients
  config/       Spring configuration
  controller/   REST controllers
  dto/          Request and response DTOs
  entity/       Domain entities
  exception/    Error handling
  repository/   Persistence adapters
  security/     Authentication and permission checks
  service/      Business services
```

## Security Notes

- Real secrets, passwords, hostnames, and tokens must stay outside Git.
- Use least-privilege database users in deployed environments.
- Rotate `PLATFORM_AUTH_SECRET` if a token signing secret is ever exposed.
- Run `./pre-push-check.sh` before publishing changes.

## License

MIT. See `LICENSE`.
