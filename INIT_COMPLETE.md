# Initialization Summary

The Java platform project has been initialized and refactored into a Spring Boot
3.2 / Java 21 service.

## Current Scope

- Authentication and RBAC
- Diagnosis API proxying
- Session management
- Evaluation review
- Admin model configuration
- Runtime health and operations endpoints
- Contract verification and coverage gate

## Verification

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```

## Public Release Notes

Before publishing, confirm the repository contains no private deployment
details, personal author metadata, tracked generated caches, real secrets, or
organization-specific references. See `SECURITY.md`.
