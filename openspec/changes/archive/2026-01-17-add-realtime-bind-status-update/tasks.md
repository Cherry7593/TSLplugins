# Tasks

## 1. Implementation

- [x] 1.1 在 `WebBridgeClient.handleEventMessage()` 中添加 `BIND_STATUS_UPDATE` 事件分支
- [x] 1.2 新增 `handleBindStatusUpdateEvent()` 方法解析事件数据
- [x] 1.3 在 `QQBindManager` 中新增 `handleBindStatusUpdateEvent()` 方法处理逻辑
- [x] 1.4 实现在线玩家查找和缓存刷新
- [x] 1.5 添加可选的玩家提示消息
- [x] 1.6 添加绑定/解绑时执行命令功能

## 2. Validation

- [ ] 2.1 手动测试：网页绑定后游戏内变量立即更新
- [ ] 2.2 手动测试：QQ 群绑定后游戏内变量立即更新
- [ ] 2.3 手动测试：解绑后游戏内变量立即清空
- [ ] 2.4 手动测试：玩家不在线时不报错
- [ ] 2.5 手动测试：绑定后权限组实时变更
