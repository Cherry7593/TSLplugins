# Tasks: Add Death Gold Penalty

## 1. XConomy API Extension
- [x] 1.1 在 `XconomyApi.kt` 中添加 `withdraw(player, amount)` 方法（反射调用 XConomy 的 changePlayerBalance 或 takeBalance）
- [ ] 1.2 添加单元测试场景验证 withdraw 方法的正确性

## 2. Death Penalty Module Structure
- [x] 2.1 创建 `DeathPenalty/DeathPenaltyManager.kt`（配置加载、状态管理）
- [x] 2.2 创建 `DeathPenalty/DeathPenaltyListener.kt`（PlayerDeathEvent 监听器）

## 3. Configuration
- [x] 3.1 在 `config.yml` 中添加 `death-penalty` 配置节（enabled, amount, messages）
- [x] 3.2 在 `messages.yml` 中添加相关消息键（扣费成功、余额不足）
- [x] 3.3 递增 `config-version` 版本号

## 4. Plugin Integration
- [x] 4.1 在 `TSLplugins.kt` 中注册 DeathPenaltyManager 和 DeathPenaltyListener
- [x] 4.2 在 `ReloadCommand.kt` 中添加热重载支持

## 5. Validation
- [ ] 5.1 手动测试：XConomy 已安装且玩家余额充足时死亡扣费
- [ ] 5.2 手动测试：玩家余额不足时免除惩罚并提示
- [ ] 5.3 手动测试：XConomy 未安装时模块优雅禁用
- [ ] 5.4 手动测试：配置热重载生效
