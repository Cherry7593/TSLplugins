# Change: Add XConomy-based Death Gold Penalty

## Why

服务器需要一种简单的经济惩罚机制来增加游戏难度和经济消耗。当玩家死亡时自动扣除金币是最常见的惩罚方式之一，可以：
- 增加死亡的代价感，提升生存紧迫性
- 促进服务器经济消耗，防止通货膨胀
- 提供可配置的惩罚力度，适应不同服务器风格

## What Changes

- 新增 `death-penalty` 模块，监听玩家死亡事件并扣除金币
- 扩展现有 `XconomyApi` 类，添加余额扣除方法
- 支持配置文件中自定义扣费金额和提示消息
- 异步处理扣费逻辑，避免主线程阻塞
- 余额不足时免除惩罚并发送特定提示

## Impact

- **Affected specs**: 新增 `death-penalty` capability
- **Affected code**:
  - `src/main/kotlin/org/tsl/tSLplugins/DeathPenalty/` (新目录)
  - `src/main/kotlin/org/tsl/tSLplugins/XconomyTrigger/XconomyApi.kt` (扩展 withdraw 方法)
  - `src/main/resources/config.yml` (新增配置节)
  - `src/main/resources/messages.yml` (新增消息键)
  - `TSLplugins.kt` (模块注册)
  - `ReloadCommand.kt` (热重载支持)

## Dependencies

- **XConomy 插件**: 必须安装，提供余额查询和扣除 API

## Risks

- XConomy 未安装或未加载时需优雅降级
- 玩家数据未初始化时需防止 NPE
- 死亡事件处理需异步，避免阻塞主线程
