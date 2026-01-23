# Tasks: Add XConomy Balance Trigger

## 1. Infrastructure

- [x] 1.1 添加 XConomy API 依赖到 `build.gradle.kts` (compileOnly) - 使用反射调用，无需硬依赖
- [x] 1.2 创建 `XconomyTrigger/` 模块目录结构
- [x] 1.3 在 `config.yml` 添加 `xconomy-trigger` 配置节
- [x] 1.4 递增 `CURRENT_CONFIG_VERSION` (37 → 38)

## 2. Core Implementation

- [x] 2.1 实现 `XconomyApi.kt` - XConomy/Vault API 封装
  - 检测 XConomy 是否可用
  - 获取玩家余额方法
  - 反射调用避免硬依赖
- [x] 2.2 实现 `XconomyTriggerManager.kt`
  - 配置加载/重载
  - 玩家状态 Map 管理 (ConcurrentHashMap)
  - 异步轮询调度 (AsyncScheduler.runAtFixedRate)
  - 阈值检测 + 回差逻辑
  - 命令占位符替换 (%player%, %uuid%, %balance%)
  - 命令执行 (GlobalRegionScheduler)
- [x] 2.3 实现冷却机制
  - 全局扫描间隔 (scan-interval-seconds)
  - 玩家触发冷却 (player-cooldown-seconds)

## 3. Integration

- [x] 3.1 在 `TSLplugins.kt` 注册模块
- [x] 3.2 在 `ReloadCommand.kt` 添加热重载支持
- [x] 3.3 添加启用/禁用日志

## 4. Safety & Error Handling

- [x] 4.1 XConomy 未安装时的降级处理 (自动禁用模块并警告)
- [x] 4.2 API 调用异常捕获 (try-catch in checkPlayerBalance)
- [x] 4.3 命令执行异常处理 (try-catch in executeCommands)

## 5. Testing

- [x] 5.1 编写手动测试步骤 (见下方)
- [ ] 5.2 测试 XConomy 未安装场景
- [ ] 5.3 测试阈值边缘触发防抖
- [ ] 5.4 测试配置热重载

## 6. Documentation

- [x] 6.1 更新 README 或 docs - 配置节已添加注释
- [x] 6.2 添加配置示例注释

---

## 手动测试步骤

1. **XConomy 未安装测试**
   - 在没有 XConomy 的服务器上启动插件
   - 预期: 控制台显示 `[XconomyTrigger] XConomy 未安装或不可用，模块已禁用`

2. **基本功能测试**
   - 安装 XConomy，设置 `xconomy-trigger.enabled: true`
   - 设置低余额阈值为当前余额 + 100
   - 等待扫描间隔后观察命令执行

3. **阈值边缘防抖测试**
   - 将余额设置为略低于阈值
   - 等待触发后，将余额设置为略高于阈值（但低于阈值 + hysteresis）
   - 预期: 状态保持 LOW_FIRED，不会重复触发

4. **热重载测试**
   - 修改配置文件中的阈值
   - 执行 `/tsl reload`
   - 预期: 新阈值立即生效
