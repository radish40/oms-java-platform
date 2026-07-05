# OMS Java Platform

这是 OMS 平台 API 的 Spring Boot 3.2 / Java 21 实现。

该服务提供认证、RBAC、诊断管理、会话管理、评测审核、模型配置代理、运行时健康检查和运维接口。服务边界切到 Java 后，仍保持与 legacy TypeScript Platform 的 HTTP 兼容。

## 验证

使用 Java 21 和 Maven 3.9+：

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
```

Maven 验证会运行单元测试、集成冒烟测试、API 契约检查、打包和 JaCoCo 覆盖率门禁。

## 必需配置

所有敏感值必须通过环境变量提供。不要提交 `.env` 文件或真实部署值。

| 变量 | 用途 | 示例 |
| --- | --- | --- |
| `DB_HOST` | MySQL 主机 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_NAME` | MySQL 数据库 | `oms_db` |
| `DB_USER` | MySQL 用户 | `oms_user` |
| `DB_PASSWORD` | MySQL 密码 | `<set-via-env>` |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `<set-via-env>` |
| `PLATFORM_AUTH_SECRET` | HMAC token 签名密钥 | `<random-32-byte-secret>` |
| `PLATFORM_DEFAULT_PASSWORD` | 初始管理员密码 | `<set-via-env>` |
| `PLATFORM_SUPPORT_PASSWORD` | 初始客服账号密码 | `<set-via-env>` |
| `MODEL_RUNTIME_URL` | Runtime 服务基准地址 | `http://localhost:18010` |

`PYTHON_RUNTIME_URL` 仍作为旧本地环境的兼容别名被接受。新部署优先使用 `MODEL_RUNTIME_URL`。

## API 分组

- 认证和 RBAC：`/auth/*`、`/admin/rbac`、`/admin/users`
- 诊断：`/diagnosis/runs`、`/diagnosis/feedback`、`/diagnosis/judge-reports`
- 会话和评测：`/sessions`、`/eval/candidates`
- 管理端模型配置：`/admin/model-configs`、`/admin/model-bindings`、`/model-options/chat`
- 运行时和运维：`/health`、`/ops/debug`

详细端点覆盖见 `docs/API_CONTRACT.md`。业务能力注册见 `docs/BUSINESS_FUNCTIONS.md`。技术实现注册见 `docs/TECHNICAL_IMPLEMENTATION.md`。跨服务核心 DTO 见 `docs/CORE_SCHEMAS.md`。后续可观测性命名见 `docs/OBSERVABILITY_NAMING.md`。

## 本地运行

```bash
export DB_HOST=localhost
export DB_PASSWORD=<set-via-env>
export PLATFORM_AUTH_SECRET=<random-32-byte-secret>
export PLATFORM_DEFAULT_PASSWORD=<set-via-env>
export PLATFORM_SUPPORT_PASSWORD=<set-via-env>
export MODEL_RUNTIME_URL=http://localhost:18010
mvn spring-boot:run
```

服务运行后，Swagger UI 地址为 `http://localhost:18020/doc.html`。

## 项目结构

```text
src/main/java/com/example/oms/platform/
  client/       Runtime HTTP 客户端
  config/       Spring 配置
  controller/   REST 控制器
  dto/          请求和响应 DTO
  entity/       内部领域实体
  exception/    异常处理
  repository/   持久化适配器
  security/     认证和权限检查
  service/      业务服务
```

## 安全说明

- 真实 secret、密码、主机名和 token 必须留在 Git 之外。
- 部署环境使用最小权限数据库用户。
- 如果 token 签名密钥曾经暴露，需要轮换 `PLATFORM_AUTH_SECRET`。
- 发布前运行 `./pre-push-check.sh`。

## 许可证

MIT。见 `LICENSE`。
