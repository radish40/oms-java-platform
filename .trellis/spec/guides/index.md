# 思考指南索引

这些指南用于编码前扩展检查面，减少跨层遗漏和技术债。

| 指南 | 何时使用 |
| --- | --- |
| [代码复用思考指南](./code-reuse-thinking-guide.md) | 新增工具类、DTO、Controller 分支、测试夹具前 |
| [跨层思考指南](./cross-layer-thinking-guide.md) | 变更前端到平台、平台到 Runtime、平台到知识库的数据流前 |
| [提交信息规范](./commit-message-guidelines.md) | 拆分提交、合并提交、推送前 |
| [文档治理规范](./documentation-governance.md) | 新增或移动长期文档、任务总结、实现说明前 |

## 交叉审查规则

- 任何 CRITICAL / HIGH 结论必须指向真实代码、测试或文档位置。
- AI 审查结论默认需要二次核验，不允许只凭推断修改核心契约。
- 反例和失败案例要进入测试、黄金案例或 Trellis 任务记录，不能只留在对话里。
- 若发现新型误判、遗漏或脱敏风险，应补充到对应 spec，而不是只写阶段总结。

