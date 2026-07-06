# Java Platform 后端规范索引

本目录记录 Java Platform 的长期开发规范。新会话在修改代码前，应先读取当前 Trellis 任务，再读取本索引指向的规范文件。

## 适用范围

- Spring Boot Controller、Service、Repository、Security、DTO。
- `/platform/*` 代理契约、Runtime 代理契约、鉴权和审计。
- Java Platform 与前端、Python Runtime、私有知识库之间的接口兼容。
- 发布前测试、脱敏、文档同步和提交检查。

## 规范文件

| 文件 | 用途 |
| --- | --- |
| [API 契约](./api-contracts.md) | 路由、DTO、错误响应、代理兼容性 |
| [目录结构](./directory-structure.md) | Java 模块边界、测试目录、文档归属 |
| [数据库规范](./database-guidelines.md) | H2/MySQL、诊断记录、RBAC 数据访问 |
| [错误处理](./error-handling.md) | 统一错误、鉴权失败、Runtime 降级 |
| [质量规范](./quality-guidelines.md) | 单测、覆盖率、交叉审查、脱敏检查 |
| [日志规范](./logging-guidelines.md) | 日志级别、证据链、敏感数据遮蔽 |
| [发布规范](./release-guidelines.md) | 本地验收、推送、版本发布、回滚说明 |

## 任务前检查

- [ ] 已读取 `.trellis/tasks/<task>/prd.md`。
- [ ] 变更 API 前已对照 `docs/API_CONTRACT.md`。
- [ ] 变更业务能力前已对照 `docs/BUSINESS_FUNCTIONS.md`。
- [ ] 变更实现细节前已对照 `docs/TECHNICAL_IMPLEMENTATION.md`。
- [ ] 已明确测试命令和脱敏检查命令。
- [ ] 已确认是否需要同步前端、Python Runtime 或知识库仓库。

