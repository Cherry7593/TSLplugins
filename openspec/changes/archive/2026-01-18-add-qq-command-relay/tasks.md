# Tasks: Add QQ Command Relay

## 1. WebSocket Message Handling

- [x] 1.1 在 `WebBridgeClient.kt` 中添加 `type: "request"` 消息处理分支
- [x] 1.2 添加 `handleQQCommandExecute()` 方法解析请求参数
- [x] 1.3 实现 serverId 过滤逻辑（null 表示广播，否则匹配本服务器）

## 2. Command Execution

- [x] 2.1 在 `WebBridgeManager.kt` 中添加 `executeQQCommand()` 方法
- [x] 2.2 使用 Folia 调度器在主线程执行命令 (`Bukkit.getGlobalRegionScheduler()`)
- [x] 2.3 使用 `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)` 执行命令

## 3. Response Handling

- [x] 3.1 在 `WebBridgeManager.kt` 中添加 `sendQQCommandResult()` 方法
- [x] 3.2 构建符合协议的 JSON 响应消息
- [x] 3.3 通过 `WebBridgeClient.enqueue()` 发送响应

## 4. Output Capturing (Optional Enhancement)

- [x] 4.1 创建 `OutputCapturingCommandSender` 类实现 `CommandSender` 接口
- [x] 4.2 捕获命令输出并包含在响应中

## 5. Error Handling

- [x] 5.1 处理命令执行异常，返回 `success: false` 和错误信息
- [x] 5.2 添加日志记录所有执行的命令（用于审计）

## 6. Testing

- [ ] 6.1 在游戏内测试命令执行和结果返回
- [ ] 6.2 测试 serverId 过滤逻辑
- [ ] 6.3 测试错误场景（命令语法错误、权限不足等）
