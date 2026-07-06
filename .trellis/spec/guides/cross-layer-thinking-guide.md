# 跨层思考指南

> **用途**：实现前先梳理数据如何跨层流动。

---

## 问题

**大多数 bug 发生在层与层之间，而不是单层内部。**

常见跨层 bug：

- API 返回格式 A，前端期待格式 B。
- 数据库存 X，服务层转成 Y 时丢了数据。
- 多层各自实现同一逻辑，最后行为不一致。

---

## 实现跨层功能前

### 第一步：画出数据流

写清数据如何移动：

```text
来源 → 转换 → 存储 → 读取 → 转换 → 展示
```

对每个箭头都问：

- 当前数据格式是什么？
- 这里可能出什么错？
- 谁负责校验？

### 第二步：识别边界

| 边界 | 常见问题 |
| --- | --- |
| API ↔ Service | 类型不匹配、字段缺失 |
| Service ↔ Database | 格式转换、null 处理 |
| Backend ↔ Frontend | 序列化、日期格式 |
| Component ↔ Component | props 形状变化 |

### 第三步：定义契约

对每个边界说明：

- 精确输入格式是什么？
- 精确输出格式是什么？
- 可能出现哪些错误？

---

## 常见跨层错误

### 错误 1：隐式格式假设

**错误**：不检查就假设日期格式。

**正确**：在边界处做显式格式转换。

### 错误 2：校验分散

**错误**：多个层重复校验同一件事。

**正确**：在入口处完成一次权威校验。

### 错误 3：抽象泄漏

**错误**：组件知道数据库 schema。

**正确**：每一层只知道相邻层。

### 错误 4：每个消费者都解析同一份 payload

**错误**：命令读取 JSONL 事件并在本地 cast 字段：

```typescript
const thread = (ev as { thread?: string }).thread;
const labels = (ev as { labels?: string[] }).labels;
```

这看起来是局部代码，但意味着每个消费者都维护一份私有事件契约。下次字段变更时，很容易只改一个命令而漏掉另一个。

**正确**：在事件边界统一解码，再导出类型化 projection：

```typescript
if (!isThreadEvent(ev)) return false;
return ev.thread === filter.thread;
```

**规则**：对 append-only log、JSON stream、RPC payload 或配置文件，要有唯一所有者负责：

- event / payload 类型定义。
- 从 `unknown` 进入系统的 type guard 和 normalization。
- UI/命令使用的 metadata projection。
- 从事实源 replay 状态的 reducer。

展示代码可以格式化字段，但不能重新定义 payload 契约。

---

## 跨层功能检查清单

实现前：

- [ ] 已画出完整数据流。
- [ ] 已识别所有层边界。
- [ ] 已定义每个边界的数据格式。
- [ ] 已决定校验发生在哪一层。

实现后：

- [ ] 已用边界值测试，例如 null、空值、非法值。
- [ ] 已验证每个边界的错误处理。
- [ ] 已检查数据能完整往返。
- [ ] 已确认消费者 import 共享 decoder / projection，而不是本地 cast payload 字段。
- [ ] 已确认派生状态能追溯到源事件标识，例如 `seq`、`id`、`version`，没有发明第二套游标。

---

## 跨平台模板一致性

在 Trellis 中，命令模板（例如 `record-session.md`）可能在多个平台中存在相同或近似内容。这本身就是一个跨层边界。

### 修改任意命令模板后

- [ ] 找到所有平台上的同名命令：`find src/templates/*/commands/trellis/ -name "<command>.*"`。
- [ ] 更新所有平台副本，包括 Markdown `.md` 和 TOML `.toml`。
- [ ] Gemini TOML 需要适配换行续写（`\\` vs `\`）和三引号字符串。
- [ ] 运行 `/trellis:check-cross-layer`，确认没有漏改。

真实案例：曾只更新 Claude 的 `record-session.md`，让它使用 `--mode record`，但漏掉 iFlow、Kilo、OpenCode 和 Gemini，最后被跨层检查发现。

---

## 生成型运行时模板升级一致性

有些生成文件既是文档，也是运行时输入。Trellis 中的 `.trellis/workflow.md` 会被 `get_context.py`、`workflow_phase.py`、SessionStart filter 和 per-turn hook 解析。模板变更必须同时验证新 init 和旧项目 update 路径。

### 修改运行时会解析的模板后

- [ ] 找出所有读取该模板的运行时 parser，而不只是写入该文件的安装器。
- [ ] 检查相关语法是否位于显眼 managed region 之外，例如 tag block 外。
- [ ] 验证全新 `init` 输出，以及带旧 `.trellis/.version` 的 versioned `update` 场景。
- [ ] 使用旧版 pristine template fixture 增加升级回归，断言安装后的文件达到当前 packaged 形状。
- [ ] 更新拥有该运行时契约的 backend spec。

真实案例：Codex inline mode 把 workflow 平台标记从 `[Codex]` / `[Kilo, Antigravity, Windsurf]` 改成 `[codex-sub-agent]` / `[codex-inline, Kilo, Antigravity, Windsurf]`。新 init 正确，但 `trellis update` 只合并 `[workflow-state:*]` block，保留了 block 外的旧标记。结果升级项目拿到了新 hook 脚本，却仍用旧 workflow routing，`get_context.py --mode phase --platform codex` 可能返回空 Phase 2.1 详情。

---

## 版本化文档边界

版本化文档也是跨层边界：源码路径、`docs.json` 版本路由和渲染后的版本选择器必须描述同一条 release line。

### 编辑版本化文档前

- [ ] 识别目标 release line：stable、beta 或 RC。
- [ ] 确认编辑的 MDX 路径匹配该 release line：
  - stable：`docs-site/{start,advanced,...}` 和 `docs-site/zh/{start,advanced,...}`
  - beta：`docs-site/beta/**` 和 `docs-site/zh/beta/**`
  - RC：`docs-site/rc/**` 和 `docs-site/zh/rc/**`
- [ ] 确认 `docs.json` navigation 把版本标签指向同一组路径。
- [ ] 提交前 grep 对侧目录，查找 release-line-specific 术语。
- [ ] root release 路径中出现 beta 内容时，应视为 source-path bug，而不是渲染 bug。

真实案例：一个 beta-only 任务工作流变更把 `prd.md` + `design.md` + `implement.md`、任务创建同意和 Codex mode banner 写进 root `start/` 和 `advanced/` 路径。文档站随后在 Release selector 下展示了 0.6 beta 行为。修复方式是恢复 root release 文档，把 0.6 内容移到 `beta/` 和 `zh/beta/`，并增加针对 root release tree 的 beta marker grep audit。

---

## 模式探测检查清单

当 CLI 通过探测远端资源自动判断模式时，例如检查 `index.json` 是否存在来区分 marketplace 和 direct download：

### 实现前

- [ ] 探测逻辑在所有会使用结果的路径中运行，包括 interactive、`-y` 和各种 `--flag` 组合。
- [ ] 区分 404 和瞬时错误，不要把两者都当作 “not found”。
- [ ] 瞬时错误应该中止或重试，不能静默切换模式。
- [ ] 上下文变化时重置共享状态，例如 cache 或 prefetched data。
- [ ] shortcut path（例如 `--template` 跳过 picker）必须具备与探测路径相同质量的错误处理；检查下游函数是否仍调用 catch-all wrapper。

### 实现后

- [ ] 追踪从探测结果到模式分支的每条路径，没有 fallthrough。
- [ ] 外部格式契约（giget URI、raw URL）已有测试，或至少用注释文档化。
- [ ] metadata 读取必须消费完整响应或使用 streaming parser，不能只解析固定长度前缀。
- [ ] 从解析片段重建复合标识符时，确认所有字段都包含且位置正确，例如 `provider:repo/path#ref`，不是 `provider:repo#ref/path`。
- [ ] shortcut 后调用的 action function 不应内部使用旧 catch-all fetch；当错误区分很重要时，必须用探测同等级质量的函数。

真实案例：Custom registry flow 在 3 轮审查里发现 8 个 bug：探测只在交互模式跑、瞬时错误落到错误模式、giget URI 的 `#ref` 位置错误、切换来源后 prefetched templates 泄漏、`--template` shortcut 绕过探测但 `downloadTemplateById` 内部仍使用 catch-all `fetchTemplateIndex`，把 timeout 变成 “Template not found”。

真实案例：Agent-session update hints 用 `response.read(4096)` 读取 npm `latest` metadata，然后按完整 JSON 解析。`@mindfoldhq/trellis` 包 metadata 超过 4 KB，JSON 被截断，解析静默失败，第一次 session injection 就没有 update hint。修复方式是完整读取响应再解析，并增加 `version` 后跟 8 KB metadata tail 的回归。

---

## 什么时候创建流程文档

满足以下任一条件时，创建详细流程文档：

- 功能跨 3 层以上。
- 多个团队参与。
- 数据格式复杂。
- 这个功能之前造成过 bug。

---

## 事件日志 / Projection 边界

Append-only log 是跨层契约。一条事件会经过：

```text
CLI input → event writer → events.jsonl → reader → filter → reducer → display
```

### 新增事件类型或字段后

- [ ] 把 event kind 加入中心事件分类。
- [ ] 在事件层增加类型化 event variant 或 type guard。
- [ ] 给来自用户输入或 JSON 的数组/对象字段增加 normalization helper。
- [ ] `seq` / `id` 只能由 event writer 分配。
- [ ] filter 和 reducer 消费 typed event guard，不做本地 cast。
- [ ] display code 消费 reducer 输出或 typed event，不直接消费 raw JSON。
- [ ] 至少增加一个回归，证明 history replay 和 live filtering 使用同一套 filter model。

真实案例：Thread channels 增加了 `kind: "thread"`、`description`、`context`、labels 和 `lastSeq`。第一版能正确 replay thread state，但多个命令仍用本地 cast 重新解析事件 payload 字段。修复方式是让核心事件层拥有 `ThreadChannelEvent` 和 `isThreadEvent`，让 `reduceChannelMetadata` 成为唯一 channel metadata projection，让 `reduceThreads` 成为唯一 thread replay reducer。
