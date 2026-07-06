# 代码复用思考指南

> **用途**：写新代码前先停一下，确认仓库里是否已经有可复用的实现。

---

## 问题

**重复代码是最常见的一致性 bug 来源。**

复制、粘贴或重写已有逻辑会带来：

- bug 修复无法自动传播。
- 行为随着时间分叉。
- 代码库越来越难理解。

---

## 写新代码前

### 第一步：先搜索

```bash
# 搜索相似函数名
rg "functionName" .

# 搜索相似业务关键词
rg "keyword" .
```

### 第二步：问这些问题

| 问题 | 如果答案是“是” |
| --- | --- |
| 是否已有相似函数？ | 复用或扩展它 |
| 是否已有同类模式？ | 跟随现有模式 |
| 是否应该成为共享工具？ | 放到正确的共享位置 |
| 是否正在从另一个文件复制代码？ | 停下，考虑抽成共享实现 |

---

## 常见重复模式

### 模式 1：复制函数

**错误**：把校验函数复制到另一个文件。

**正确**：抽到共享工具，并在需要的地方 import。

### 模式 2：相似组件

**错误**：创建一个和现有组件 80% 相似的新组件。

**正确**：用 props 或 variant 扩展现有组件。

### 模式 3：重复常量

**错误**：在多个文件里定义同一个常量。

**正确**：单一事实源，所有地方 import。

### 模式 4：重复读取 payload 字段

**错误**：多个消费者在本地 cast 同一份 JSON/event 字段：

```typescript
const description = (ev as { description?: string }).description;
const context = (ev as { context?: ContextEntry[] }).context;
```

这看起来只是两行代码，但它让每个消费者都维护了一份私有 payload 契约。字段变化时，很容易只改一处漏另一处。

**正确**：把 decoder、type guard 或 projection 放在数据所有者附近：

```typescript
if (isThreadEvent(ev)) {
  renderThreadEvent(ev);
}
```

**规则**：同一个未类型化 payload 字段被 2 个以上地方读取时，在出现第三个读取点之前，先创建共享 type guard、normalizer 或 projection。

---

## 什么时候抽象

应该抽象：

- 同一代码出现 3 次以上。
- 逻辑复杂到容易出 bug。
- 多个人或多个模块会需要它。

不应该抽象：

- 只使用一次。
- 只是普通一行代码。
- 抽象本身比重复更复杂。

---

## 批量修改之后

做完多文件相似修改后：

1. **复核**：是否抓全所有实例？
2. **搜索**：用 `rg` 找漏网之鱼。
3. **判断**：是否应该抽成共享实现？

### Reducer 应该集中处理动作分支

当状态由 `action`、`kind`、`status`、`phase` 这类值派生时，优先使用一个 reducer 和一个 `switch`，不要把 `if/else` 分散在多个地方。

```typescript
// 错误：状态转移分散，难审查
if (action === "opened") { ... }
else if (action === "comment") { ... }
else if (action === "status") { ... }

// 正确：一个 reducer 拥有转移表
switch (event.action) {
  case "opened":
    ...
    return;
  case "comment":
    ...
    return;
}
```

事件日志是事实源时，这一点尤其重要。Reducer 是被记录下来的 replay model；展示代码和命令代码不应该重复实现 replay 逻辑碎片。

---

## 提交前检查

- [ ] 已搜索现有相似代码。
- [ ] 没有应该共享的复制逻辑。
- [ ] 没有在共享 decoder 之外重复读取未类型化 payload 字段。
- [ ] 常量只有一个事实源。
- [ ] 相似模式保持同样结构。
- [ ] reducer/action 状态转移集中在一个 reducer 或命令分发器里。

---

## 易错点：Python if/elif/else 不会做穷尽检查

**问题**：Python 的 `if/elif/else` 没有编译期穷尽检查。给 `Literal` 类型新增值时，旧分支可能静默落到 `else`，返回错误默认值。

**症状**：新平台只部分可用，一些方法返回 Claude 默认值而不是平台专属值，且不会报错。

**示例**：

```python
# 错误："gemini" 会落到 else，返回 "claude"
@property
def cli_name(self) -> str:
    if self.platform == "opencode":
        return "opencode"
    else:
        return "claude"

# 正确：每个平台显式分支
@property
def cli_name(self) -> str:
    if self.platform == "opencode":
        return "opencode"
    elif self.platform == "gemini":
        return "gemini"
    else:
        return "claude"
```

**预防**：给 Python `Literal` 类型新增值时，搜索所有基于该类型的 `if/elif/else` 链，并补显式分支。不要假设 `else` 对新值仍然正确。

---

## 易错点：两个机制产出同一文件集

**问题**：如果两个机制要产出同一组文件，例如 init 用递归复制，update 用手写 `files.set()`，目录结构变化只会自动影响其中一个机制，另一个会悄悄漂移。

**症状**：init 正常，update 却创建错误路径或漏文件。

**预防**：

- 最好消除不对称，让手动路径调用自动收集逻辑，例如 `collectTemplateFiles()` 调用 `getAllScripts()`。
- 如果不对称无法避免，加回归测试比较两个机制输出。
- 迁移目录结构时，搜索所有引用旧结构的代码路径。

真实案例：`trellis update` 曾维护一份 11 个脚本的手写 `files.set()` 列表，而 `getAllScripts()` 已经跟踪这些脚本。修复方式是删除手写列表，改成遍历 `getAllScripts()`。

---

## Trellis 模板文件登记

新增 `src/templates/trellis/scripts/` 下的文件时：

**唯一登记点**：`src/templates/trellis/index.ts`

1. 增加 `export const xxxScript = readTemplate("scripts/path/file.py");`
2. 加入 `getAllScripts()` Map。

这样就够了。`commands/update.ts` 会直接使用 `getAllScripts()`，不需要再手动同步列表。

**为什么重要**：没有登记到 `getAllScripts()`，`trellis update` 就不会把文件同步到用户项目，修复和功能也不会传播。

### 新脚本快速检查

```bash
# 新增 .py 文件后，确认它在 getAllScripts() 中
rg "newFileName" src/templates/trellis/index.ts
```

### 模板同步约定

`.trellis/scripts/`（dogfooded）和 `packages/cli/src/templates/trellis/scripts/`（template）必须保持一致。编辑 `.trellis/scripts/` 后同步：

```bash
rsync -av --delete --exclude='__pycache__' .trellis/scripts/ packages/cli/src/templates/trellis/scripts/
```

**注意**：`rsync` 源/目标写错会创建嵌套垃圾目录，例如 `.trellis/scripts/packages/cli/...`。运行前必须检查路径。
