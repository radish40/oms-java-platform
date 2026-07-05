# 技术实现注册表

本文记录 Java Platform 服务的实现结构，供维护者和发布审核使用。

## 运行时基线

| 组件 | 实现 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot 3.2 |
| 构建 | Maven |
| HTTP 客户端 | OkHttp |
| JSON 序列化 | Jackson |
| 持久化访问 | Spring JDBC 和 mapper 适配器 |
| 安全 | Bearer token 认证、HMAC filter、RBAC aspect |
| 测试 | JUnit 5、Spring MVC 测试、集成冒烟测试 |
| 覆盖率 | `mvn verify` 中的 JaCoCo check 和 report |

## 包职责

| 包 | 职责 |
| --- | --- |
| `controller` | TypeScript 兼容的 REST 端点和 HTTP 状态行为。 |
| `service` | 业务流程、校验、runtime 编排和审计写入。 |
| `repository` | 数据库访问和 runtime 支撑的数据适配器。 |
| `client` | runtime 集成的 HTTP 适配器。 |
| `dto` | 稳定的公开请求和响应记录。 |
| `entity` | 面向内部持久化的记录。 |
| `security` | token 处理、HMAC 认证、密码哈希和权限检查。 |
| `exception` | 结构化错误信封和全局异常映射。 |
| `config` | Spring 配置和外部化 runtime 设置。 |

## 安全设计

- secret 和密码通过环境变量提供。
- 主配置不内置生产密码或签名密钥默认值。
- 权限检查使用 `@RequiresPermission` 和 `PermissionAspect` 为 controller/service 提供保护。
- 认证失败和授权失败保持客户端兼容的状态码和错误 payload。
- `pre-push-check.sh` 会运行通用公开发布检查，并可叠加 `.git/info/sensitive-denylist` 中的本地 denylist。本地 denylist 不纳入版本管理，脚本也不会打印其内容。

## Runtime 集成

- `MODEL_RUNTIME_URL` 是首选 runtime 基准地址。
- 旧环境变量别名仍被接受，用于本地兼容。
- Runtime 失败会映射为结构化上游错误响应。
- 健康检查在适用时同时报告 platform 状态和 runtime 可达性。

## 数据和 API 兼容

- 公开 DTO 保持稳定字段名和响应信封。
- Controller mapping 保持 legacy platform 路径布局。
- 错误响应由 `GlobalExceptionHandler` 集中处理。
- 契约覆盖见 `docs/API_CONTRACT.md`。
- 跨服务核心 DTO 见 `docs/CORE_SCHEMAS.md`。
- 后续 trace、span、metric 和结构化日志命名见 `docs/OBSERVABILITY_NAMING.md`。

## 发布验证

发布前运行：

```bash
mvn -s maven-central-settings.xml verify
./pre-push-check.sh
git status --short --untracked-files=all
```

预期发布条件：

- 测试通过，无失败或错误。
- JaCoCo 覆盖率检查通过。
- 当前工作树和可达 Git 历史的脱敏检查通过。
- 工作区干净。
- 发布标签解析到预期发布提交。
