# 核心 DTO 和 Schema 契约

本文定义 frontend、Python Runtime 和 Java Platform 共享的稳定核心实体。Java Platform API 契约仍是 HTTP 路径和响应信封的权威来源；本文记录跨服务必须保持一致的实体形状。

## 版本规则

- 公开字段使用 `snake_case`。
- 只新增可选字段时，不需要提升 schema 版本。
- 删除字段、改变字段类型或改变枚举语义时，必须提升版本并补充迁移说明。
- 客户端可以忽略未知字段；服务端不能依赖未记录的内部字典形状。
- 反馈在后续审核流程把它提升为知识、技能或策略变更之前，一律保持 `record_only`。

当前 schema 版本：`1`。

## 实体清单

| 实体 | 生产方 | 消费方 | 说明 |
| --- | --- | --- | --- |
| `diagnosis_run` | Python Runtime | Java Platform、frontend、evaluation | 一次用户诊断执行。 |
| `diagnosis_step` | Python Runtime | 前端 Inspector、judge/eval | 一次 run 内的一个工具或模型步骤。 |
| `evidence` | Python Runtime | 前端 Inspector、judge/eval | 诊断总结引用的证据。 |
| `feedback` | Frontend、Java Platform proxy | Python Runtime、evaluation review | 用户对 run 的人工反馈。 |
| `case` | Python Runtime/evaluation review | replay 脚本、evaluation review | 可回放的诊断种子 case 或候选 case。 |
| `skill` | Python Runtime/evaluation review | 技能生命周期和降级审核 | 诊断能力生命周期实体。 |
| `auth_user` | Java Platform | Frontend、Python proxy context | 已认证用户和权限快照。 |

## `diagnosis_run`

```json
{
  "schema_version": 1,
  "run_id": "run_...",
  "session_id": "ses_...",
  "question": "订单 JO... 为什么卡住？",
  "status": "completed",
  "latency_ms": 1234,
  "error": "",
  "started_at": "2026-07-05T00:00:00",
  "ended_at": "2026-07-05T00:00:01"
}
```

必需字段：`run_id`、`session_id`、`question`、`status`、`latency_ms`、`error`、`started_at`、`ended_at`。

允许的 `status` 值：`pending`、`running`、`completed`、`failed`、`cancelled`、`error`。当前 Python Runtime 将失败 run 存为 `error`；`failed` 保留给后续归一化值。客户端必须安全渲染未知值。

## `diagnosis_step`

```json
{
  "schema_version": 1,
  "seq": 1,
  "type": "tool_done",
  "status": "ok",
  "name": "query_order",
  "input": {},
  "output": {},
  "latency_ms": 42,
  "error": "",
  "created_at": "2026-07-05T00:00:00"
}
```

必需字段：`seq`、`type`、`status`、`name`、`input`、`output`、`latency_ms`、`error`、`created_at`。

当前 runtime 的 `type` 值是事件导向的，可能包括 `start`、`status`、`thinking`、`reasoning`、`token`、`tool_start`、`tool_done`、`tool_error`、`diagnosis`、`context_snapshot`、`done` 和 `error`。后续可观测性可以把这些值归一化为 `model`、`tool`、`retrieval`、`summary` 或 `system`，但 API 消费方必须接受当前事件值。

当前 runtime 的 `status` 值为 `ok` 和 `error`。后续归一化值可以包括 `success`、`empty`、`failed` 和 `skipped`。

## `evidence`

```json
{
  "schema_version": 1,
  "source": "query_order",
  "kind": "field",
  "field": "orders.status",
  "value": "PROCESSING",
  "record_key": "JO20260601001",
  "step_seq": 1,
  "subject": "JO20260601001",
  "evidence_level": "runtime_data",
  "citation_path": "diagnosis_steps[1].output.orders[0].status"
}
```

必需字段：`source`、`kind`、`field`、`value`、`record_key`。

允许的 `kind` 值：`field`、`absence`、`knowledge`、`tool_error`、`source_code`。

允许的 `evidence_level` 值：`runtime_data`、`source_code`、`verified_doc`、`case_reviewed`、`unverified_summary`。

## `feedback`

```json
{
  "schema_version": 1,
  "id": 1,
  "run_id": "run_...",
  "session_id": "ses_...",
  "rating": "wrong",
  "root_cause_correct": false,
  "comment": "根因遗漏了 WMS callback 错误。",
  "created_at": "2026-07-05T00:00:00"
}
```

必需字段：`id`、`run_id`、`session_id`、`rating`、`root_cause_correct`、`comment`、`created_at`。

允许的 `rating` 值：`useful`、`incomplete`、`wrong`。

反馈影响契约：

- `POST /diagnosis/feedback` 记录反馈历史。
- `GET /diagnosis/feedback` 列出反馈，可按 `run_id` 过滤。
- `GET /diagnosis/feedback/summary` 返回聚合计数和影响说明。
- 反馈不能自动改变订单状态、客户通知、知识条目、提示词策略或技能状态。
- 在显式审核流程改变该行为之前，负反馈只能成为评测候选或审核输入。

## `case`

```json
{
  "schema_version": 1,
  "case_id": "case_001",
  "title": "ERP 推送后订单卡住",
  "subject_type": "oms_order",
  "subject_id": "JO20260601001",
  "channel": "jd",
  "scenario": "shipping_callback",
  "question": "JO20260601001 为什么没有发货？",
  "enabled": true,
  "tags": ["feedback", "shipping"],
  "source": {
    "type": "diagnosis_feedback",
    "feedback_id": 7,
    "run_id": "run_...",
    "session_id": "ses_..."
  },
  "expectations": {
    "required_tools": ["query_consignment"],
    "required_evidence": [
      {"source": "query_consignment", "field": "status"}
    ],
    "confidence": {"labels": ["medium", "high"]},
    "coverage": {},
    "root_cause_terms": ["ERP callback"],
    "forbidden_answer_terms": ["无证据重试"]
  },
  "review_note": "进入 evals/golden_cases 前需要补齐 expectations。"
}
```

必需字段：`case_id`、`title`、`question`、`expectations`。

`expectations` 必须是对象。当前 replay 校验支持 `required_tools`、`required_evidence`、`confidence`、`coverage`、`root_cause_terms`、`root_cause_category`、`required_missing_data`、`next_questions` 和 `forbidden_answer_terms`。

`input_question` 可能出现在较早的规划材料中，但 `question` 才是 runtime replay 字段。

## `skill`

```json
{
  "schema_version": 1,
  "skill_id": "official_web_order_status",
  "name": "官网订单状态诊断",
  "version": 1,
  "status": "draft",
  "owner": "support-team",
  "scenario": "官网订单状态",
  "definition_json": {},
  "tool_status_json": {},
  "activation_blockers": [],
  "source_artifact_refs": ["official-web-channel-artifacts.jsonl"],
  "created_at": "2026-07-05T00:00:00",
  "updated_at": "2026-07-05T00:00:00"
}
```

必需字段：`skill_id`、`name`、`status`、`version`、`owner`、`scenario`、`definition_json`、`created_at`、`updated_at`。

允许的 `status` 值：`draft`、`reviewing`、`shadow`、`active`、`degraded`、`disabled`、`archived`。

候选技能在通过人工审核和 replay 评测之前，不能参与生产诊断。

## `auth_user`

```json
{
  "schema_version": 1,
  "username": "support",
  "display_name": "客服用户",
  "role": "support",
  "role_label": "客服",
  "permissions": ["diagnosis:read"],
  "auth_source": "backend"
}
```

必需字段：`username`、`display_name`、`role`、`role_label`、`permissions`。

允许的 `auth_source` 值：`backend`、`frontend_debug`。

## 跨服务归属

| 字段组 | 归属方 | 规则 |
| --- | --- | --- |
| Auth 和 RBAC 字段 | Java Platform | Frontend 只读使用。 |
| Diagnosis run/step/evidence 字段 | Python Runtime | Java Platform 透传，不重塑。 |
| Feedback 字段 | Python Runtime 表、Java proxy、frontend 入口 | Java 保持 payload 形状；feedback 保持 record-only。 |
| Case 和 skill 生命周期字段 | Python Runtime/evaluation review | 候选项需要审核后才能产生生产影响。 |
