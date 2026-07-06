# 任务清单同步与远端部署方案

## 任务清单同步状态

当前环境未发现可直接调用的 TickTick/滴答清单接口，也未发现本地导出的滴答任务文件。因此，本轮先把可交接任务状态同步到仓库内 Trellis，作为新会话和后续代理的权威任务源。

| 任务 | 优先级 | 状态 | 记录位置 | 说明 |
| --- | --- | --- | --- | --- |
| 补齐 Java Platform Trellis 任务管理 | P1 | 已完成 | `.trellis/tasks/07-06-buqi-trellis-guanli/` | 已补齐 workflow、spec、tasks、workspace，并修正 `.gitignore` |
| 补齐知识库 Trellis 任务管理 | P1 | 已完成 | 私有知识库仓 `.trellis/tasks/07-06-buqi-trellis-guanli/` | 已补齐知识库工作流、主题/黄金案例/反例管理规则 |
| 二阶段交叉审查收尾 | P1 | 已完成 | Python Runtime `docs/PHASE2_CLOSURE_REPORT.md` | P1-C 门禁、前端代理、Java 契约、知识库资产已完成本地验收并推送 |
| P1-C report hash/commit 绑定 | P1 | 后续增强 | Python Runtime `docs/PHASE2_CLOSURE_REPORT.md` | 已记录为后续项，不阻断当前二阶段验收 |
| 评测与知识闭环体验优化 | P2 | 后续增强 | Python Runtime / Frontend Trellis 后续任务 | 当前接口契约已对齐，后续可继续做体验和可观测性增强 |

后续如果接入外部滴答清单，应以本文件和各仓 `.trellis/tasks/` 为同步源，不要从对话记忆反推任务状态。

## 当前远端部署方案

当前部署方案是 Docker Compose 多服务部署，平台服务已切到 Java Platform，不再关注旧 TypeScript Platform。

### 服务拓扑

```text
Browser
  |
  v
oms-frontend:5173
  |
  v
oms-python-agent:18010
  |                         |
  | /platform/* proxy       | AI runtime / RAG / SSE
  v                         |
oms-java-platform:18020 <---+
  |
  +-- MySQL 8.4
  +-- Redis 7.4
```

### 容器与端口

| 服务 | 容器名 | 端口 | 职责 |
| --- | --- | --- | --- |
| MySQL | `oms-mysql` | `3306` | 业务数据与运行态存储 |
| Redis | `oms-redis` | `6379` | 缓存、会话与运行态辅助数据 |
| Java Platform | `oms-platform` | `18020` | 认证、RBAC、审计、会话、评测、模型配置、平台代理 |
| Python Agent | `oms-python-agent` | `18010` | AI 诊断 Runtime、RAG、SSE、公网站点/API 网关 |
| Frontend | `oms-frontend` | `5173 -> 80` | React/Vite 前端静态站点 |

### 服务边界

- Java Platform 是当前唯一平台实现，Compose 从 `./oms-java-platform` 构建 `oms-platform`。
- Python Agent 继续保留为 AI Runtime 和公共 API 网关，通过 `PLATFORM_API_URL` / `PLATFORM_PROXY_URL` 指向 Java Platform。
- Frontend 调用路径保持兼容，评测和知识条目相关请求走 `/platform` 边界，不直接绕过 Java Platform。
- 旧 TypeScript Platform 不再作为部署目标，也不再作为新功能集成边界。

### 必需环境变量

部署前 `.env` 必须提供以下值，不能提交真实值：

```bash
MYSQL_ROOT_PASSWORD=
MYSQL_PASSWORD=
PLATFORM_AUTH_SECRET=
PLATFORM_DEFAULT_PASSWORD=
PLATFORM_SUPPORT_PASSWORD=
DEEPSEEK_API_KEY=
```

### 发布命令

```bash
cp .env.example .env
# 填充 .env 中所有空 secret
docker compose up -d --build
```

### 健康检查

```bash
docker compose ps
curl http://localhost:5173/
curl http://localhost:18010/health
curl http://localhost:18020/health
docker compose logs -f oms-platform
docker compose logs -f oms-python-agent
docker compose logs -f oms-frontend
```

### 清理旧服务

旧 TypeScript Platform 不再参与部署。若远端仍存在旧平台容器或进程，应停止并删除，只保留上表五个 Compose 服务。

```bash
docker compose down
docker compose up -d --build
```

只有确认数据库和缓存可丢弃时，才允许执行：

```bash
docker compose down -v
```
