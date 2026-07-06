# 质量规范

## 项目硬边界

- OMS-AI 是只读诊断助手，不自动修改订单、不自动取消订单、不自动通知客户。
- 诊断结论必须尽量关联工具证据；缺失数据要显式说明。
- 低置信度诊断不能给出强处置建议。
- 人工反馈当前进入审核和评测候选，不直接改写生产诊断行为。

## 开发前检查

- 先读当前 Trellis 任务的 `prd.md`、`design.md`、`implement.md`。
- 修改跨层字段、API、SSE、Platform 代理前，读取 `api-contracts.md`。
- 修改数据访问前，读取 `database-guidelines.md`。
- 修改发布或运维流程前，读取 `release-guidelines.md`。
- 新增文档或移动文档前，读取 `../guides/documentation-governance.md`。

## 测试要求

### 修改后自测与观测记录

- 修改完成后，执行者必须自己验证改动效果，不能只停留在“代码已改”或“单测通过”。
- 自动化测试只是基础门槛；根据改动范围，还必须调用对应工具、构造真实场景数据，并记录关键观测结果。
- 记录内容至少包括：验证入口、mock 或请求参数、期望结果、实际结果、发现的问题或确认无异常。
- 如果某项验证无法执行，必须说明原因、风险和可替代的验证方式。

前端变更必须做浏览器级验证：

- 启动前端或相关本地服务，使用浏览器工具打开真实页面。
- 自己 mock 或准备接近真实业务的数据，覆盖正常态、空态、错误态或权限态中与本次改动相关的状态。
- 观察并记录页面展示、交互结果、接口调用结果、控制台错误和关键截图或文字结论。
- 仅运行 `npm test`、`npm run lint` 或 `npm run build` 不足以完成前端验收。

后端变更必须做请求级验证：

- 使用测试客户端、脚本、curl、HTTP 工具或已有测试夹具，自己构造请求参数调用被改动接口或函数入口。
- 覆盖至少一个成功请求和一个关键异常/边界请求；涉及权限、鉴权、数据库或外部依赖时必须 mock 对应上下文。
- 记录请求方法、路径或函数名、关键参数、响应状态、响应字段、日志/错误表现和结论。
- 仅运行 `python -m unittest`、`npm test` 或编译检查不足以完成后端验收。

根据变更范围选择验证：

```bash
python -m unittest tests.test_all -v
python -m compileall -q src tests scripts deploy
cd frontend && npm test
cd frontend && npm run lint
cd frontend && npm run build
cd platform && npm test
cd platform && npm run lint
cd platform && npm run build
```

文档-only 变更至少运行：

```bash
git diff --check
rg "<moved-or-renamed-doc-name>" .
```

## 安全要求

- SQL 必须参数化。
- LLM 输出进入页面前必须净化或按现有安全路径渲染。
- token、密码、生产凭据不进入仓库、日志和文档。
- RBAC 操作必须检查当前用户权限，不允许移除自己的关键权限。

## 评审重点

- 是否保持只读诊断边界。
- 是否同步更新 API 契约、前端类型和测试。
- 是否保留旧字段或兼容路径。
- 是否把新规范写入 Trellis spec，而不是散落在阶段总结。
- 是否避免提交未识别的工作区改动。

## 提交要求

- 测试、自测和必要观测记录通过后，可以直接提交，不需要额外等待人工确认。
- 提交前必须检查工作区范围，只提交本次任务相关文件，不能混入未识别或无关改动。
- 提交信息必须遵守 `../guides/commit-message-guidelines.md`：中文描述变更意图，保留必要的 Conventional Commit 前缀和专业名词。

## 禁止事项

- 为了“看起来完整”新增未使用抽象。
- 在前端或 Platform 里静默改名 API 字段。
- 把阶段性结论当成当前事实源。
- 使用 `git add .` 混入无关文件。
