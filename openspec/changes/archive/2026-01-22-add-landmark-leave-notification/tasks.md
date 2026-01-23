## 1. Implementation

- [x] 1.1 在 `LandmarkManager.kt` 中添加 `canShowLeaveMessage` 和 `setLeaveMessageCooldown` 方法，复用进入冷却时间配置
- [x] 1.2 在 `LandmarkListener.kt` 的离开地标逻辑中添加 `showLeaveNotification` 方法调用
- [x] 1.3 在 `LandmarkListener.kt` 中实现 `showLeaveNotification` 方法（ActionBar + 聊天消息）
- [x] 1.4 在 `messages.yml` 中添加 `leave` 消息

## 2. Verification

- [ ] 2.1 测试离开地标：从地标区域内走出应显示离开提示
- [ ] 2.2 测试冷却机制：快速离开再进入同一地标，离开提示应受冷却限制
- [ ] 2.3 测试不同地标：从地标A直接进入地标B，不应触发地标A的离开提示（直接切换场景）
