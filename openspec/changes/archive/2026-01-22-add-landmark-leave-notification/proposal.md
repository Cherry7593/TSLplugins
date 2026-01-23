# Change: 新增地标离开提示

## Why

目前玩家进入地标时有丰富的提示（Title、音效、ActionBar、聊天消息），但离开地标时没有任何提示。添加离开提示可以让玩家明确知道自己已离开地标区域，增强用户体验的完整性。同时需要防止重复触发（如玩家在边界频繁进出），与进入提示一样使用冷却机制。

## What Changes

- 玩家离开地标区域时显示离开提示（ActionBar + 聊天消息，较进入提示轻量）
- 添加离开提示冷却机制，复用进入提示冷却配置 `enter-message-cooldown-seconds`
- 新增消息配置 `leave`

## Impact

- Affected specs: `landmark`
- Affected code:
  - `src/main/kotlin/org/tsl/tSLplugins/Landmark/LandmarkListener.kt` - 离开地标逻辑添加提示
  - `src/main/kotlin/org/tsl/tSLplugins/Landmark/LandmarkManager.kt` - 添加离开提示冷却检查方法
  - `src/main/resources/messages.yml` - 新增 `leave` 消息
