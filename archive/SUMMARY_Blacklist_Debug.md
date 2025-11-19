# Ride & Toss 黑名单功能调试总结

**日期**: 2025-11-19  
**问题**: Ride 和 Toss 功能的黑名单不起作用，即使生物在黑名单内也可以举起或骑乘

---

## 问题描述

用户报告即使将某些生物（如 WITHER、ENDER_DRAGON 等）添加到黑名单，玩家仍然可以：
- 骑乘这些生物（Ride 功能）
- 举起这些生物（Toss 功能）

## 代码审查结果

经过审查，黑名单检查的代码逻辑**完全正确**：

### RideListener.kt（第 54-62 行）
```kotlin
// 检查黑名单
if (manager.isEntityBlacklisted(entity.type)) {
    // 检查是否有绕过黑名单的权限
    if (!player.hasPermission("tsl.ride.bypass")) {
        // 静默返回，不显示任何提示消息
        return
    }
}
```

### TossListener.kt（第 66-72 行）
```kotlin
// 检查实体是否在黑名单中
if (manager.isEntityBlacklisted(entity.type) && !player.hasPermission("tsl.toss.bypass")) {
    if (manager.isShowMessages()) {
        player.sendMessage(serializer.deserialize(manager.getMessage("entity_blacklisted")))
    }
    return
}
```

### 黑名单检查方法
```kotlin
fun isEntityBlacklisted(entityType: EntityType): Boolean {
    return blacklist.contains(entityType)
}
```

## 可能的原因

由于代码逻辑正确，问题可能出在以下几个方面：

### 1. **配置文件格式问题**
- 实体类型名称拼写错误
- YAML 缩进错误
- 实体类型不匹配 Bukkit 的 EntityType 枚举

### 2. **权限问题**
- 玩家可能拥有 `tsl.ride.bypass` 或 `tsl.toss.bypass` 权限
- 这些权限允许玩家绕过黑名单限制

### 3. **配置未正确加载**
- 插件初始化时配置文件可能未正确读取
- 黑名单集合可能为空

## 添加的调试功能

为了帮助诊断问题，已在代码中添加了详细的调试日志：

### 1. 配置加载时的日志

**RideManager.loadConfig()**（第 41-49 行）
```kotlin
val blacklistStrings = config.getStringList("ride.blacklist")
blacklist.clear()
plugin.logger.info("[Ride] 配置文件中的黑名单字符串: $blacklistStrings")
blacklistStrings.forEach { entityName ->
    try {
        val entityType = EntityType.valueOf(entityName.uppercase())
        blacklist.add(entityType)
        plugin.logger.info("[Ride] 成功添加黑名单: $entityName -> $entityType")
    } catch (e: IllegalArgumentException) {
        plugin.logger.warning("[Ride] 无效的实体类型: $entityName")
    }
}
```

### 2. 运行时黑名单检查日志

**RideManager.isEntityBlacklisted()**（第 78-82 行）
```kotlin
fun isEntityBlacklisted(entityType: EntityType): Boolean {
    val result = blacklist.contains(entityType)
    plugin.logger.info("[Ride] 黑名单检查: $entityType -> ${if (result) "已禁止" else "允许"} (黑名单: $blacklist)")
    return result
}
```

相同的日志也添加到了 **TossManager** 中。

## 使用调试日志诊断问题

### 步骤 1: 重新编译并启动插件
1. 构建插件 JAR 文件
2. 将 JAR 放入服务器的 `plugins` 文件夹
3. 启动或重载服务器

### 步骤 2: 查看启动日志
在服务器启动时，检查日志中是否有以下输出：
```
[INFO] [Ride] 配置文件中的黑名单字符串: [WITHER, ENDER_DRAGON, WARDEN, GHAST, ELDER_GUARDIAN]
[INFO] [Ride] 成功添加黑名单: WITHER -> WITHER
[INFO] [Ride] 成功添加黑名单: ENDER_DRAGON -> ENDER_DRAGON
...
[INFO] [Ride] 已加载配置 - 默认状态: 启用, 黑名单: 5 种生物
```

**关键检查点：**
- 黑名单字符串列表是否正确读取？
- 每个实体是否成功转换为 EntityType？
- 最终黑名单数量是否正确？

### 步骤 3: 测试功能并查看运行时日志
当玩家尝试骑乘或举起生物时，日志会显示：
```
[INFO] [Ride] 黑名单检查: WITHER -> 已禁止 (黑名单: [WITHER, ENDER_DRAGON, ...])
```
或
```
[INFO] [Ride] 黑名单检查: COW -> 允许 (黑名单: [WITHER, ENDER_DRAGON, ...])
```

**关键检查点：**
- 黑名单检查是否被调用？
- 检查的实体类型是否正确？
- 黑名单集合内容是否正确？
- 检查结果是否符合预期？

### 步骤 4: 检查玩家权限
如果黑名单检查显示"已禁止"但玩家仍能操作，检查：
```
/lp user <玩家名> permission check tsl.ride.bypass
/lp user <玩家名> permission check tsl.toss.bypass
```

## 常见问题及解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 日志显示黑名单列表为空 `[]` | 配置文件路径错误或格式错误 | 检查 config.yml 的 `ride.blacklist` 和 `toss.blacklist` 配置 |
| 日志显示"无效的实体类型" | 实体名称拼写错误或不存在 | 参考 Bukkit EntityType 枚举，使用正确的大写名称 |
| 黑名单检查显示"已禁止"但仍可操作 | 玩家拥有 bypass 权限 | 移除玩家的 `tsl.ride.bypass` 或 `tsl.toss.bypass` 权限 |
| 没有任何黑名单检查日志 | 监听器未触发或功能未启用 | 检查 `ride.enabled` 和 `toss.enabled` 配置，确保玩家有 `tsl.ride.use` 权限 |

## 配置文件示例

### 正确的配置格式

```yaml
ride:
  enabled: true
  default_enabled: true
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
    - GHAST
    - ELDER_GUARDIAN

toss:
  enabled: true
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
```

### ⚠️ 错误示例

```yaml
# ❌ 错误：缩进不正确
ride:
enabled: true
blacklist:
- WITHER

# ❌ 错误：拼写错误
ride:
  blacklist:
    - WITHERSKELELETON  # 应该是 WITHER_SKELETON
    - ENDERDRAGON       # 应该是 ENDER_DRAGON（带下划线）

# ❌ 错误：使用小写
ride:
  blacklist:
    - wither  # 虽然代码会转换为大写，但最好使用正确格式
```

## 下一步行动

1. **编译并部署**带有调试日志的新版本
2. **重启服务器**或使用 `/tsl reload` 重载配置
3. **查看启动日志**确认黑名单加载情况
4. **测试功能**并观察运行时日志
5. **根据日志输出**定位具体问题

## 生产环境注意事项

调试日志可能会产生大量输出。问题解决后，可以考虑：
- 移除或注释掉详细的调试日志
- 或者使用条件日志（仅在配置中启用 debug 模式时输出）

---

## 修改的文件

- `src/main/kotlin/org/tsl/tSLplugins/Ride/RideManager.kt`
  - 第 41-49 行：配置加载时的详细日志
  - 第 78-82 行：运行时黑名单检查日志
  
- `src/main/kotlin/org/tsl/tSLplugins/Toss/TossManager.kt`
  - 第 58-66 行：配置加载时的详细日志
  - 第 106-110 行：运行时黑名单检查日志

## 相关权限

- `tsl.ride.use` - 使用骑乘功能的基础权限
- `tsl.ride.bypass` - **绕过骑乘黑名单限制**（管理员权限）
- `tsl.toss.use` - 使用举起功能的基础权限
- `tsl.toss.bypass` - **绕过举起黑名单限制**（管理员权限）

---

**状态**: 🔍 调试工具已添加  
**下一步**: 测试并分析日志输出  
**优先级**: 高

