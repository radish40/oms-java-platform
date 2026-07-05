# 可观测性命名契约

本文定义后续 trace、span、metric 和日志的命名。当前仅作为文档契约，不要求立即改 runtime instrumentation。

## 目标

- 将 `diagnosis_run` 对齐到 OpenTelemetry trace。
- 将 `diagnosis_step` 对齐到 OpenTelemetry span。
- 在添加 dashboard、alert 或 exporter 之前稳定命名。
- 避免把订单 PII、地址、手机号、token 或原始 prompt 泄露到 metric label 或高基数字段中。

## Trace 命名

| Runtime 概念 | OpenTelemetry 概念 | 名称 |
| --- | --- | --- |
| 一次诊断执行 | Trace | `oms_ai.diagnosis.run` |
| 一次 replay case 执行 | Trace | `oms_ai.evaluation.case_replay` |
| 一次批量诊断请求 | Trace | `oms_ai.diagnosis.batch` |

必需 trace 属性：

| 属性 | 来源 | 说明 |
| --- | --- | --- |
| `oms.diagnosis.run_id` | `diagnosis_run.run_id` | 稳定内部标识。 |
| `oms.session.id` | `diagnosis_run.session_id` | 不可用时为空字符串。 |
| `oms.diagnosis.status` | `diagnosis_run.status` | 尽量使用已文档化状态值。 |
| `oms.channel` | 诊断 subject | 可选；不能放账号或地址值。 |
| `oms.subject.type` | 诊断 subject | 示例：`oms_order`、`platform_order`。 |

不要把原始订单号加入 metric label。对于 trace，只有当部署环境的隐私策略允许 trace 级调试访问时，才可以包含订单类标识。

## Span 命名

`diagnosis_step` 映射为 `oms_ai.diagnosis.run` 下的 span。

| Step 类型 | Span 名称 |
| --- | --- |
| 模型调用 | `oms_ai.diagnosis.step.model` |
| 工具调用 | `oms_ai.diagnosis.step.tool` |
| 知识检索 | `oms_ai.diagnosis.step.retrieval` |
| 总结组装 | `oms_ai.diagnosis.step.summary` |
| Runtime/system 步骤 | `oms_ai.diagnosis.step.system` |

必需 span 属性：

| 属性 | 来源 | 说明 |
| --- | --- | --- |
| `oms.diagnosis.step_seq` | `diagnosis_step.seq` | 数字序号。 |
| `oms.diagnosis.step_type` | 从 `diagnosis_step.type` 归一化 | `model`、`tool`、`retrieval`、`summary`、`system`。 |
| `oms.diagnosis.step_name` | `diagnosis_step.name` | 工具、模型或 retriever 名称。 |
| `oms.diagnosis.step_status` | 从 `diagnosis_step.status` 归一化 | `success`、`empty`、`failed`、`skipped`。 |
| `oms.error.type` | 失败分类 | 可选，不能包含原始异常 secret。 |

`diagnosis_step.type` 和 `diagnosis_step.status` 保持 `CORE_SCHEMAS.md` 记录的 runtime API 原始值。Exporter 在发出 span 或 metric 前，应把它们映射成上方低基数可观测性维度：

| Runtime 原始值 | 归一化 step type |
| --- | --- |
| `reasoning`、`thinking`、`token`、`diagnosis` | `model` |
| `tool_start`、`tool_done`、`tool_error` | `tool` |
| 知识或检索工具名 | `retrieval` |
| `done` | `summary` |
| `start`、`status`、`context_snapshot`、`error` | `system` |

| Runtime 原始状态 | 归一化 step status |
| --- | --- |
| `ok` | `success` |
| `error` | `failed` |
| 无行或空工具结果 | `empty` |
| 被跳过的 runtime 分支 | `skipped` |

## Metric 名称

| Metric | 类型 | 单位 | Label |
| --- | --- | --- | --- |
| `oms_ai.diagnosis.runs_total` | counter | `{run}` | `status`、`channel`、`subject_type` |
| `oms_ai.diagnosis.run.duration` | histogram | `ms` | `status`、`channel` |
| `oms_ai.diagnosis.steps_total` | counter | `{step}` | `step_type`、`step_name`、`status` |
| `oms_ai.diagnosis.step.duration` | histogram | `ms` | `step_type`、`step_name`、`status` |
| `oms_ai.diagnosis.feedback_total` | counter | `{feedback}` | `rating`、`root_cause_correct` |
| `oms_ai.diagnosis.coverage.score` | gauge | `1` | `channel`、`subject_type` |
| `oms_ai.runtime.tool.errors_total` | counter | `{error}` | `tool`、`failure_type` |
| `oms_ai.evaluation.case_replays_total` | counter | `{case}` | `status`、`scenario` |

Metric label 规则：

- Label 必须是低基数。
- 不要使用 `run_id`、`session_id`、订单号、客户姓名、手机号、地址、原始问题、原始回答或原始异常文本作为 label。
- 优先使用 `channel`、`subject_type`、`scenario`、`tool`、`status` 和 `failure_type`。

## 日志事件名

结构化日志使用下列事件名：

| 事件 | 触发时机 |
| --- | --- |
| `diagnosis.run.started` | run 被接受。 |
| `diagnosis.run.completed` | run 成功结束。 |
| `diagnosis.run.failed` | run 失败。 |
| `diagnosis.step.started` | step 开始。 |
| `diagnosis.step.completed` | step 完成。 |
| `diagnosis.step.failed` | step 失败。 |
| `diagnosis.feedback.recorded` | 反馈已保存。 |
| `evaluation.case.replay.completed` | replay case 完成。 |
| `skill.lifecycle.changed` | 技能状态经审核后发生变化。 |

必需日志字段：

- `event`
- 可用时包含 `run_id`
- 可用时包含 `session_id`
- step 事件包含 `step_seq`
- `status`
- `latency_ms`

敏感值必须在写日志前脱敏。

## Diagnosis Run 和 Step 对齐

```text
diagnosis_run.run_id      -> trace attribute oms.diagnosis.run_id
diagnosis_run.session_id  -> trace attribute oms.session.id
diagnosis_run.status      -> trace attribute oms.diagnosis.status
diagnosis_run.latency_ms  -> metric oms_ai.diagnosis.run.duration

diagnosis_step.seq        -> span attribute oms.diagnosis.step_seq
diagnosis_step.type       -> span name suffix and attribute step_type
diagnosis_step.name       -> span attribute step_name
diagnosis_step.status     -> span status and metric label status
diagnosis_step.latency_ms -> metric oms_ai.diagnosis.step.duration
```

这个映射让 UI 继续使用 `diagnosis_run` 和 `diagnosis_step`，同时让后续 OpenTelemetry exporter 可以在不重塑领域模型的前提下发出 trace 和 metric。
