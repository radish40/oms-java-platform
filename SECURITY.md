# Security

This repository is prepared for public release. It must not contain real
deployment secrets, private infrastructure details, personal credentials, or
organization-specific brand references.

## Secret Handling

All sensitive configuration is injected through environment variables:

```bash
export DB_PASSWORD=<set-via-env>
export REDIS_PASSWORD=<set-via-env>
export PLATFORM_AUTH_SECRET=<random-32-byte-secret>
export PLATFORM_DEFAULT_PASSWORD=<set-via-env>
export PLATFORM_SUPPORT_PASSWORD=<set-via-env>
```

Never commit `.env`, `.env.local`, private certificates, tokens, database dumps,
or deployment-specific hostnames.

## Release Checklist

- Run `mvn -s maven-central-settings.xml verify`.
- Run `./pre-push-check.sh`.
- Confirm `git status --short --untracked-files=all` is clean.
- Confirm `git log --all --format='%H %an <%ae> %s'` contains no private
  authors, private email domains, secrets, or internal brand terms.
- Confirm `git ls-files | grep -E '__pycache__|\.pyc$'` returns no tracked
  cache files.

## Reporting

Report security issues through a private security advisory or a private contact
channel maintained by the repository owner. Do not disclose vulnerabilities
publicly before a fix is available.
