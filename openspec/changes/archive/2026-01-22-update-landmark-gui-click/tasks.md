## 1. Implementation

- [x] 1.1 修改 `handleMainMenuClick` 方法，获取 `InventoryClickEvent` 的点击类型并传递
- [x] 1.2 修改 `handleLandmarkClick` 方法签名，新增 `isRightClick` 参数
- [x] 1.3 实现右键点击逻辑：检查权限（admin 或 maintainer），满足则打开编辑菜单，否则静默忽略
- [x] 1.4 更新 `createLandmarkItem` 中的 lore 提示文本：
  - 已解锁：显示"左键传送 | 右键编辑"（编辑提示仅对有权限者显示）
  - 未解锁：保持"需要先解锁"
- [ ] 1.5 游戏内测试验证左右键行为及权限检查
