# 发布与运维规范

## 当前发布边界

- 当前主要发布路径是容器化发布：`python scripts/release_ops.py container-deploy`。
- 原 `systemd + SFTP` 发布路径保留为回滚方式。
- 生产公网入口是 `18010`，Platform `18020` 仅服务器本机访问。
- 生产凭据通过环境变量传入，不能写入仓库。

## 发布前检查

发布前必须确认：

```powershell
git status --short
$env:OMS_PASS="***"
python scripts/release_ops.py status
```

如果需要提前跑完整门禁：

```powershell
python -m unittest tests.test_all -v
cd frontend
cmd /c npm test
cmd /c npm run lint
cmd /c npm run build
cd ..
python -m compileall -q src tests scripts deploy
```

## 容器发布

标准命令：

```powershell
$env:OMS_PASS="***"
python scripts/release_ops.py container-deploy
```

容器发布会执行前端测试、前端 lint、后端单测、Python compileall、前端构建、上传归档、远端 Docker 构建和健康检查。

## 异步发布协作

Codex 会话不是 CI 控制台。发布命令完成本地门禁并进入远端容器构建/启动阶段后，不应在当前会话里硬等完整 Docker build 日志，除非用户明确要求。

启动发布后只能表述为“发布已启动”或“发布进行中”，不能说成“发布完成”。发布完成必须以稍后验收为准。

稍后验收固定检查：

```powershell
$env:OMS_PASS="***"
python scripts/release_ops.py status
python scripts/release_ops.py smoke
python scripts/release_ops.py smoke --frontend-hash
```

必要时查看发布任务：

```powershell
python scripts/release_ops.py container-jobs --limit 5
```

## 完成定义

一次生产发布只有同时满足以下条件，才算完成：

- 本地工作区干净。
- 代码已提交。
- 发布命令成功启动并完成远端发布。
- `oms-ai` 容器 healthy。
- `oms-ai-platform` 容器 healthy。
- `/health` 返回 `UP`。
- `/version` 返回目标 commit。
- 生产前端资源 hash 与本地构建一致。
- `/opt/oms-ai/deploy-history.jsonl` 存在成功记录。
- 如有功能改动，完成至少一轮业务冒烟。

## 回滚

容器版本回滚：

```powershell
$env:OMS_PASS="***"
python scripts/release_ops.py container-rollback --image-tag <previous-tag>
```

如果容器整体不可用，回退到保留的 systemd 路径：

```powershell
$env:OMS_PASS="***"
python scripts/release_ops.py container-rollback
```

回滚后必须再次执行 status 和 smoke。

## 禁止事项

- 不把密码、token、生产凭据写入仓库。
- 不把“启动发布”说成“发布完成”。
- 不在没有用户要求的情况下持续刷长构建日志。
- 不用紧急参数作为正常发布路径。
- 不在回滚后跳过健康检查和业务冒烟。
