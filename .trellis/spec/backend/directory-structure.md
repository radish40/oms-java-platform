# Directory Structure Guidelines

## Workspace Mainline

```text
oms-split-workspace/
|-- oms-python-agent/   # Python AI runtime, public API, SSE, agent, RAG, tools
|-- oms-frontend/       # React + TypeScript frontend workspace
|-- oms-java-platform/  # Java Platform: RBAC, audit, stable business APIs
|-- tests/              # Python regression tests when scoped to this repo
|-- evals/              # Evaluation golden cases
|-- deploy/             # Docker Compose, deployment, and upload scripts
|-- scripts/            # release, recycle, evaluation, validation scripts
|-- knowledge-base/     # business knowledge base documents
|-- docs/               # human-readable architecture, runtime, product, history docs
`-- .trellis/           # Trellis tasks, specs, workflow, and journal
```

## Python Agent

- `src/api_server.py`: HTTP API, SSE, static frontend hosting, `/platform/*` proxy.
- `src/agent.py`: tool-calling diagnosis loop.
- `src/diagnosis_runs.py`: diagnosis runs, steps, evidence, coverage, confidence.
- `src/sessions.py`: session persistence.
- `src/rag_service.py`: hybrid knowledge retrieval.
- `src/tools.py`: OMS query tool definitions and execution.
- `src/db.py`: shared MySQL connection handling.

Add new Python Agent capabilities inside the closest existing domain module first. Create a new module only when the responsibility is clearly independent.

## Frontend

- Centralize frontend API route rules in `frontend/src/api.ts` or the active frontend API module.
- Keep API response fields in `snake_case`; TypeScript types should mirror boundary fields.
- Reuse existing workbench structure instead of embedding long-lived business rules inside one component.

## Java Platform

- Java Platform is the typed business platform layer. It does not replace the Python AI runtime.
- RBAC, audit, review permissions, admin APIs, sessions, feedback, and stable business APIs belong in Platform.
- New cross-layer capability must state data ownership and API ownership before implementation.

## Documentation Placement

- Current long-lived rules go under `.trellis/spec/`.
- Human-readable engineering docs go under `docs/`.
- Historical summaries, reviews, and audit reports go under `docs/archive/` if kept.
- Business knowledge base content stays in `knowledge-base/` and should not be mixed with engineering docs.

## Prohibited

- Do not commit `.venv/`, caches, build outputs, IDE state, secrets, or generated local runtime files.
- Do not add long-lived standards only to phase summaries without moving them into Trellis specs.
- Do not create top-level folders for small utilities before checking existing `src/`, `scripts/`, `deploy/`, `docs/`, and `.trellis/` boundaries.
