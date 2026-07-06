# 补齐 Java Platform Trellis 任务管理

## 背景

Java Platform 已完成平台化重构和二阶段收尾，但仓库中只有 `.trellis/.runtime`，没有可推送的 Trellis 工作流、规范和任务记录。新会话无法仅凭仓库内容恢复工作方式。

## 目标

- 补齐 `.trellis/workflow.md`、`.trellis/spec/`、`.trellis/tasks/`、`.trellis/workspace/`。
- 修正 `.gitignore`，确保 Trellis 规范和任务可入库，本地 runtime 继续忽略。
- 登记当前阶段的 Java 平台任务管理规则。
- 保持文档中文可读，避免模板乱码进入远程。

## 验收标准

- `git status --short --branch` 能看到 Trellis 文件作为待提交内容。
- `.trellis/workflow.md` 和索引文件不包含乱码片段。
- `python ./.trellis/scripts/task.py list` 可以读取任务。
- 推送前完成敏感词与乱码扫描。

