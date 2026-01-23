# Tasks: Fix Landmark GUI Item Theft Bug

## Implementation Tasks

1. [x] **修改 LandmarkGUI 使用自定义 InventoryHolder**
   - 创建内部类实现 InventoryHolder 接口
   - 修改 `openMainMenu` 和 `openEditMenu` 使用自定义 holder 创建 inventory

2. [x] **修改 onInventoryClose 事件处理器**
   - 检查关闭的 inventory 是否使用了 LandmarkGUI 的 holder
   - 仅在确认是地标 GUI 时才清除玩家状态

3. [x] **修改 onInventoryClick 事件处理器**
   - 通过 InventoryHolder 判断是否为地标 GUI
   - 确保所有地标 GUI 点击都被正确取消

4. [ ] **测试验证**
   - 测试 `/tsl landmark edit <name>` 打开 GUI 后无法取出物品
   - 测试主菜单同样无法取出物品
   - 测试关闭 GUI 后状态正确清理
