# Proposal: Fix Landmark GUI Item Theft Bug

## Summary

修复地标编辑 GUI 中物品可被拿出的 bug。当使用 `/tsl landmark edit <name>` 命令打开编辑界面后，玩家可以从 GUI 中拿走物品。

## Problem

**根本原因**：`LandmarkGUI.onInventoryClose` 事件处理器无条件清除玩家的菜单状态，但 `player.openInventory()` 调用会先触发当前视图的关闭事件。

**事件顺序**：
1. `openEditMenu` 设置 `playerMenuType[uuid] = MenuType.EDIT`
2. `player.openInventory(inventory)` 触发当前视图的 `InventoryCloseEvent`
3. `onInventoryClose` 清除 `playerMenuType[uuid]`
4. 新 GUI 打开，但 `playerMenuType` 已为 null
5. 点击时 `onInventoryClick` 检测到 null 直接 return，不取消事件
6. 物品可被拿出

## Solution

修改 `onInventoryClose` 事件处理器，仅在关闭的确实是地标 GUI 时才清除状态。可通过检查 inventory title 或使用自定义 InventoryHolder 来识别。

## Scope

- **Files affected**: `LandmarkGUI.kt`
- **Risk**: Low - 仅修改事件处理逻辑
- **Breaking changes**: None

## Status

- [x] Proposal
- [x] Approved
- [x] Implemented
- [ ] Tested
