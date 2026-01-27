# Change: 添加地标导航指南针

## Why
玩家在广阔的世界中探索时，缺乏直观的方式找到未解锁的地标。当前只能通过GUI查看地标列表，但无法获得实时的方向引导。添加专属导航指南针可以让玩家以更沉浸的方式发现和探索地标。

## What Changes
- 添加地标导航指南针物品，由管理员给予玩家
- 玩家可通过左键/右键在未解锁地标间切换（左键上一个，右键下一个）
- 手持指南针时显示粒子引导线指向目标地标
- 粒子效果仅对持有者可见，频率可配置
- 指南针lore显示当前解锁进度（如 1/30）
- 到达目标地标区域时收到提示
- 目标在其他维度时不显示粒子，提示玩家前往对应维度

## Impact
- Affected specs: `landmark`
- Affected code:
  - `Landmark/LandmarkManager.kt` - 添加指南针管理逻辑
  - `Landmark/LandmarkListener.kt` - 添加物品交互监听
  - `Landmark/LandmarkCommand.kt` - 添加给予指南针命令
  - 新增 `Landmark/LandmarkCompass.kt` - 指南针物品和粒子效果逻辑
  - `config.yml` - 添加指南针相关配置
  - `messages.yml` - 添加指南针相关消息
