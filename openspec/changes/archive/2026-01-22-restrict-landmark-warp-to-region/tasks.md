## 1. Implementation

- [x] 1.1 在 `LandmarkCommand.kt` 的 `handleSetWarp` 方法中添加位置验证
- [x] 1.2 在 `LandmarkGUI.kt` 的编辑菜单传送点设置逻辑（slot 13）中添加位置验证
- [x] 1.3 在 `messages.yml` 中添加错误提示消息 `warp_not_in_region`

## 2. Verification

- [ ] 2.1 测试命令设置传送点：站在地标区域内执行 `/lm setwarp <name>` 应成功
- [ ] 2.2 测试命令设置传送点：站在地标区域外执行 `/lm setwarp <name>` 应失败并提示
- [ ] 2.3 测试 GUI 设置传送点：在地标区域内打开编辑菜单点击设置传送点应成功
- [ ] 2.4 测试 GUI 设置传送点：在地标区域外打开编辑菜单点击设置传送点应失败并提示
