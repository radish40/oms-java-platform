# 数据与持久化规范

## 当前组件

- MySQL：会话、诊断运行、诊断步骤、上下文快照、反馈、评测候选、RBAC、审计事件。
- Redis：会话缓存和运行时降级辅助。
- Knowledge Base / RAG：OMS 业务知识和排障经验。

## Python MySQL 连接

Python Runtime 使用 `pymysql`，统一入口是 `src/db.py` 的 `get_db()`：

- 每个线程使用 thread-local 连接。
- `cursorclass=pymysql.cursors.DictCursor`。
- `autocommit=True`。
- 连接参数从 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE` 读取。
- `connect_timeout` 和 `read_timeout` 使用 `config.py` 中的配置。

新增 Python 数据访问逻辑应复用 `get_db()`，不要在业务模块里重复创建连接配置。

## 查询约定

- SQL 必须参数化，禁止字符串拼接用户输入。
- API JSON 字段、数据库列名和前端类型默认使用 `snake_case`。
- 列表接口默认 `limit=50`，建议最大不超过 `100`；需要深分页时再引入 `cursor`。
- 查询缺失数据时优先返回空字符串、空数组或空对象；只有明确表达缺失时才返回 `null`。
- 诊断结论必须尽量关联工具证据；缺失数据要显式呈现，不能伪造确定性。

## 数据所有权

- Platform 拥有 RBAC 用户、角色、权限、绑定关系、审计事件。
- Python Runtime 仍拥有 AI 主链路、诊断运行、诊断步骤、RAG、工具调用和会话主链路。
- Platform 代理会话、诊断历史、反馈、评测候选时，不要暗中改变底层数据语义。
- 数据所有权迁移必须单独设计，补齐兼容路径、回滚策略和测试。

## 迁移与 schema

- 数据库 schema 变更必须说明影响范围、回滚方式和兼容期。
- 回滚当前不包含自动 schema 降级；发布前必须确认 schema 变更不会阻断回滚。
- 新增字段优先后向兼容：服务端可新增字段，调用方必须忽略未知字段。

## 常见错误

- 在 API 边界静默把 `snake_case` 改成 `camelCase`。
- 在 Platform 代理层改变 Python Runtime 的返回语义。
- 让反馈直接改写生产诊断行为。
- 低置信度诊断给出强处置建议。
