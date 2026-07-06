# Trellis 工作流

## 核心原则

1. 先登记任务，再修改代码。
2. 先读取当前任务的 `prd.md`、`design.md`、`implement.md`，再执行实现或审查。
3. 规范写入 `.trellis/spec/`，阶段总结写入 `docs/`。
4. 每个任务必须包含验收标准、测试命令、脱敏检查和提交记录。
5. 面向新会话时，以本文件、`.trellis/tasks/` 和 `.trellis/spec/` 作为默认上下文。

## 任务生命周期

```bash
python ./.trellis/scripts/task.py create "<中文任务标题>" --slug <短路径名>
python ./.trellis/scripts/task.py start <任务目录名>
python ./.trellis/scripts/task.py current --source
python ./.trellis/scripts/task.py validate <任务目录名>
python ./.trellis/scripts/task.py finish
python ./.trellis/scripts/task.py archive <任务目录名>
```

任务目录位于 `.trellis/tasks/{MM-DD-slug}/`，至少包含：

- `task.json`：任务元数据、状态、分支、范围、验收命令。
- `prd.md`：需求、边界、验收标准。
- `implement.md`：实施记录、验证结果、后续风险。
- `implement.jsonl` / `check.jsonl`：多会话实现和审查上下文。

## Java Platform 必读规则

- API 兼容性优先：变更 Controller、DTO、错误码、鉴权、代理路由前，先读 `.trellis/spec/backend/api-contracts.md` 和 `docs/API_CONTRACT.md`。
- 文档同步：功能、技术实现、接口契约发生变化时，同步更新 `docs/BUSINESS_FUNCTIONS.md`、`docs/TECHNICAL_IMPLEMENTATION.md` 或 `docs/API_CONTRACT.md`。
- 质量门禁：提交前运行 `mvn -s maven-central-settings.xml test`，并确认无 `#` 形式临时标记、真实配置、密钥、品牌名或专有名词泄漏。
- 脱敏边界：`.sensitive-denylist*` 只允许本地存在，不得提交；新增脚本不得内置真实敏感词集合。
- 提交规范：提交信息使用中文，必要时保留 `feat:`、`fix:`、`docs:` 等前缀。

## 当前阶段

Java Platform 已完成第二阶段核心收尾，当前 Trellis 任务用于固化：

- Java 平台代理契约和运行时边界。
- API 兼容性、单测和脱敏门禁。
- 多会话交叉审查后的遗留问题登记。

