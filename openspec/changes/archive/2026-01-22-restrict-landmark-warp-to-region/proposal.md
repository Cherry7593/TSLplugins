# Change: 限制地标传送点必须在地标区域范围内

## Why

目前设置地标传送点（warpPoint）时，系统没有验证传送点位置是否在该地标的 AABB 区域范围内。这导致管理员或维护者可能意外地将传送点设置到地标区域外，与地标的概念设计不符。地标传送点应作为进入该地标区域后的落脚点，逻辑上应该位于区域内。

## What Changes

- 设置传送点时增加位置验证，确保传送点在地标的 X/Z 范围内
- 命令 `/lm setwarp <name>` 执行时检查玩家当前位置是否在目标地标区域内
- GUI 编辑页面中设置传送点时同样检查玩家位置是否在目标地标区域内
- 如果位置不在范围内，拒绝设置并提示用户

## Impact

- Affected specs: `landmark`
- Affected code:
  - `src/main/kotlin/org/tsl/tSLplugins/Landmark/LandmarkCommand.kt` - `handleSetWarp` 方法
  - `src/main/kotlin/org/tsl/tSLplugins/Landmark/LandmarkGUI.kt` - 编辑菜单中 slot 13 的传送点设置逻辑
  - `src/main/resources/messages.yml` - 新增错误提示消息
