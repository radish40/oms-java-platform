# 提交信息规范

> **核心规则**：所有提交信息必须中文优先；英文只用于 Conventional Commit 前缀和必要专业名词。

## 必须遵守

- 测试、自测和必要观测记录通过后，可以直接提交；提交前先确认只包含本次任务相关改动。
- Commit subject 和 body 必须使用中文描述变更意图。
- Conventional Commit 前缀可以保留英文，例如 `feat:` / `fix:` / `docs:` / `chore:` / `refactor:` / `test:` / `ci:` / `build:`。
- 必要专业名词可以保留英文，尤其是翻译后更不清晰的词，例如 `API`、`Runtime`、`Platform`、`Frontend`、`SSE`、`RAG`、`CI/CD`、`RBAC`、`Trellis`。
- 中文优先，英文只作为前缀或专业名词点缀，不使用整句英文提交说明。

## 好例子

```text
feat: 新增雾青主题切换
docs: 补充阶段总结行动计划
fix: 修复 Platform 模型绑定校验
chore: 归档主题任务记录
```

## 坏例子

```text
feat: add mist theme switcher
docs: add stage summary action plan
fix: repair platform model binding validation
```

## 提交前检查

- [ ] 前缀之后的主要说明是中文。
- [ ] 英文仅用于 Conventional Commit 前缀或必要专业名词。
- [ ] 提交信息能让中文读者快速理解这次变更做了什么。
