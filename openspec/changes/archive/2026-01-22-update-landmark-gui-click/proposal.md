# Change: 地标 GUI 左右键分离操作

## Why
当前地标列表 GUI 仅支持单一点击操作（传送），管理员或维护者需要通过命令 `/lm edit` 才能编辑地标。为了提升用户体验，需要在 GUI 中区分左右键操作：左键传送、右键编辑。

## What Changes
- **修改** `LandmarkGUI.handleLandmarkClick`：区分左键（传送）和右键（打开编辑菜单）
- **修改** `LandmarkGUI.handleMainMenuClick`：传递点击类型给 `handleLandmarkClick`
- **修改** `LandmarkGUI.createLandmarkItem`：更新 lore 提示，显示左键传送、右键编辑
- **新增** 权限检查：右键编辑仅对管理员（`tsl.landmark.admin`）或地标维护者生效，其他玩家右键无反应

## Impact
- Affected specs: `landmark`
- Affected code: `LandmarkGUI.kt`
