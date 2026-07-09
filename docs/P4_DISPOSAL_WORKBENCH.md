# P4 Disposal Workbench Contract

Phase 4 adds a diagnosis-to-disposal closure path. The API creates or reuses a human-reviewed disposal workflow for a diagnosis run, links a placeholder or external ticket, generates a handling-note draft, stores suggested actions, creates rollback guidance, and returns audit history.

## RBAC

- `disposal:view`: may list/read workflows and read an existing diagnosis workbench.
- `disposal:handle`: may create workbenches, create/confirm records, manage drafts, update status, and approve rollback plans.
- Existing granular permissions (`disposal:create`, `disposal:review`, `disposal:update`, `disposal:draft`, `disposal:rollback`, `disposal:approve`) remain in the permission dictionary for backward compatibility.

## Endpoints

### GET `/disposal/workbench/by-diagnosis/{runId}`

Requires `disposal:view`. Returns an existing workbench for a diagnosis run. Returns 404 when no workflow is linked yet.

### POST `/disposal/workbench/from-diagnosis`

Requires `disposal:handle`. Request:

```json
{
  "run_id": "run_20260708_001",
  "order_id": "JO20260607003",
  "ticket_id": "",
  "create_ticket": true
}
```

`diagnosis_detail` may be provided as a fixture fallback with the same shape as `/diagnosis/runs/{runId}`. If omitted, Platform fetches the diagnosis detail through `DiagnosisService`.

Response:

```json
{
  "run_id": "run_20260708_001",
  "workflow_id": "wf_1234567890abcdef",
  "created": true,
  "workflow": {
    "workflow_id": "wf_1234567890abcdef",
    "order_id": "JO20260607003",
    "diagnosis_run_id": "run_20260708_001",
    "status": "pending",
    "assignee": "admin",
    "priority": "medium",
    "summary": "Diagnosis run_20260708_001: Inventory shortage caused sourcing failure.",
    "created_at": "2026-07-08T20:10:00",
    "updated_at": "2026-07-08T20:10:00",
    "resolved_at": null
  },
  "handling_note_draft": "Diagnosis conclusion for JO20260607003: Inventory shortage caused sourcing failure. Category: sourcing_failed. Confidence: high. Human confirmation is required before any business operation is executed.",
  "suggested_actions": [
    {
      "action_type": "manual_verify_evidence",
      "description": "Review the diagnosis evidence and confirm the affected order/customer context.",
      "risk_level": "low",
      "requires_approval": true
    }
  ],
  "rollback_guidance": [
    {
      "step": "pause_selected_action",
      "description": "Stop the selected manual action and keep the workflow in review."
    }
  ],
  "ticket": {
    "ticket_id": "OMS-AI-JO20260607003",
    "ticket_source": "placeholder",
    "status": "linked",
    "title": "Disposal follow-up for JO20260607003"
  },
  "detail": {
    "workflow_id": "wf_1234567890abcdef",
    "records": [],
    "action_drafts": [],
    "rollback_plans": [],
    "tickets": [],
    "audit_events": []
  }
}
```

### POST `/disposal/records`

Requires `disposal:handle`. Records human confirmation only; it does not execute a business-side status change.

```json
{
  "workflow_id": "wf_1234567890abcdef",
  "step_type": "manual_confirmation",
  "decision": "confirmed",
  "note": "Confirmed suggested action: Review evidence.",
  "draft_action": {
    "id": 1,
    "action_type": "manual_verify_evidence",
    "description": "Review the diagnosis evidence and confirm the affected order/customer context.",
    "risk_level": "low",
    "requires_approval": true
  }
}
```

The returned record includes `actor` and `created_at`, and the action also writes `oms_disposal_audit_events`.
