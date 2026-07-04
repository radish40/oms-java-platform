# Project Guide

## Overview

OMS Java Platform is a Spring Boot 3.2 / Java 21 service that provides the
platform API surface for authentication, RBAC, diagnosis management, session
management, evaluation review, model configuration, health, and operations.

## Architecture

- `controller` exposes TypeScript-compatible HTTP endpoints.
- `service` contains business logic and permission-aware workflows.
- `repository` contains persistence adapters.
- `client` contains runtime HTTP adapters.
- `security` contains token handling and permission enforcement.
- `exception` contains the structured error envelope.

## Commands

```bash
mvn test
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```

Use Java 21 for all builds.

## Conventions

- Keep API paths, methods, snake_case fields, and error envelopes compatible
  with `docs/API_CONTRACT.md`.
- Keep sensitive values in environment variables.
- Do not commit generated caches, local runtime files, `.env` files, or private
  deployment details.
- Prefer explicit DTOs for stable public API boundaries.
- Run the release checklist before publishing.
