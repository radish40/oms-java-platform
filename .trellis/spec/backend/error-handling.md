# 错误处理规范

## API 错误格式

Platform 应返回结构化错误：

```json
{
  "error": {
    "code": "RUN_NOT_FOUND",
    "message": "Diagnosis run not found",
    "details": {}
  }
}
```

Python Runtime 旧接口可能仍返回：

```json
{ "error": "Run not found" }
```

迁移期前端必须兼容两种格式。Platform 代理旧接口时，应尽量包装成统一结构；如果透传旧格式，调用方必须显式处理。

## HTTP 状态

- `401`：未认证或 token 无效。
- `403`：已认证但缺少权限，响应中应包含缺失权限。
- `404`：资源不存在。
- `413`：请求体超过 `MAX_BODY`。
- `502`：Platform 代理或下游 Runtime 不可用。
- `5xx`：服务自身异常。

## 降级规则

- Platform 自身可用但依赖异常时，健康检查优先返回 `DEGRADED`，不要直接伪装成 `UP`。
- Redis 断连时允许降级到 MySQL 或无缓存路径，但必须保留诊断主链路可解释性。
- LLM 调用必须有超时和 fallback 机制，不能让请求无限等待。
- RAG 或工具查询失败时，应在诊断步骤中记录失败原因，并降低置信度。

## SSE 错误

- `/diagnose` 是 SSE 主链路，前端发送下一条消息前必须等待 `done` 或错误结束。
- SSE 中的错误应作为事件输出，避免连接静默断开。
- 工具调用错误应产生 `tool_error` 或等价步骤记录，不能只写日志。

## 权限错误

- `eval:review` 控制评测候选审核。
- `admin:rbac` 控制 RBAC 管理。
- `menu:ops` 控制 ops/debug 访问。
- 不应禁用当前登录用户，不应移除当前登录用户的 `admin:rbac` 权限。

## 禁止事项

- 不吞掉异常后返回成功。
- 不把内部 traceback 返回给前端。
- 不在错误信息里输出密码、token、生产凭据。
- 不把低质量或缺数据诊断包装成高置信结论。
