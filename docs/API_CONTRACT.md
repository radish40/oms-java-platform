# Java Platform API 契约

Java Platform 需要在下列核心 API 分组上保持 Platform HTTP 表面兼容。

## 端点清单

| 领域 | 方法 | 路径 |
| --- | --- | --- |
| 认证 | POST | `/auth/login` |
| 认证 | GET | `/auth/me` |
| RBAC | GET | `/admin/rbac` |
| RBAC | POST | `/admin/users` |
| 诊断 | GET | `/diagnosis/runs` |
| 诊断 | GET | `/diagnosis/runs/{run_id}` |
| 反馈 | POST | `/diagnosis/feedback` |
| 反馈 | GET | `/diagnosis/feedback` |
| 反馈 | GET | `/diagnosis/feedback/summary` |
| Judge 报告 | GET | `/diagnosis/judge-reports` |
| Judge 报告 | GET | `/diagnosis/judge-reports/summary` |
| Judge 报告 | POST | `/diagnosis/judge-reports/run` |
| Judge 报告 | POST | `/diagnosis/judge-reports/batch-run` |
| 会话 | GET | `/sessions` |
| 会话 | GET | `/sessions/{session_id}` |
| 会话 | DELETE | `/sessions/{session_id}` |
| 评测 | GET | `/eval/candidates` |
| 评测 | POST | `/eval/candidates/review` |
| 评测 | GET | `/eval/candidates/export` |
| 管理端模型 | GET | `/admin/model-configs` |
| 管理端模型 | POST | `/admin/model-configs` |
| 管理端模型 | DELETE | `/admin/model-configs/{provider}` |
| 管理端模型 | POST | `/admin/model-configs/test` |
| 管理端模型 | POST | `/admin/model-configs/refresh-cache` |
| 管理端模型 | GET | `/admin/model-bindings` |
| 管理端模型 | POST | `/admin/model-bindings` |
| 模型选项 | GET | `/model-options/chat` |
| 运行时/运维 | GET | `/health` |
| 运行时/运维 | GET | `/ops/debug` |

## 兼容规则

- 保持 HTTP 方法和路径不变。
- 保持请求和响应字段使用 `snake_case`。
- 保持结构化错误信封：`{"error":{"code","message","details"}}`。
- 保持权限失败语义：未认证请求返回 `401`，缺少权限返回 `403`。
- 保持评测候选导出的 NDJSON 行为。
- 核心 DTO 形状与 `docs/CORE_SCHEMAS.md` 保持一致。

## 核心 Schema

跨服务实体 `diagnosis_run`、`diagnosis_step`、`evidence`、`feedback`、`case`、`skill` 和 `auth_user` 的形状记录在 `docs/CORE_SCHEMAS.md`。Runtime 支撑的诊断响应可以包含额外字段，但消费者只能依赖公开文档字段。

反馈当前是 `record_only`：系统会保存、列出、汇总反馈，并把它作为审核输入；但反馈不能自动改变答案、知识、技能、订单状态或客户沟通。

## 最终验证

`PlatformContractIntegrationTest` 是最终集成冒烟测试。它验证代表性的认证、RBAC、诊断、会话、评测、模型配置、运行时和错误信封行为。

运行：

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```
