# PlaceholderAPI 离线玩家变量处理优化

**日期**: 2025-12-02  
**模块**: TSLPlaceholderExpansion, BlockStatsManager  
**类型**: 功能优化 / Bug修复

---

## 问题描述

部分 PlaceholderAPI 变量在玩家离线时仍然返回数据（如返回 "0" 或 "离线"），导致与其他变量行为不一致：

### 问题变量
- `%tsl_kiss_count%` - 玩家离线时返回 0
- `%tsl_kissed_count%` - 玩家离线时返回 0
- `%tsl_blocks_placed_total%` - 玩家离线时返回 "0"
- `%tsl_kiss_toggle%` - 玩家离线时返回 "离线"
- `%tsl_ride_toggle%` - 玩家离线时返回 "离线"
- `%tsl_toss_toggle%` - 玩家离线时返回 "离线"
- `%tsl_toss_velocity%` - 玩家离线时返回 "离线"
- `%tsl_newbie_tag%` - 玩家离线时返回 ""

### 正常变量（参考标准）
- `%tsl_adv_count%` - 玩家离线时返回 `null`（正确行为）

---

## 解决方案

### 核心原则
**所有需要玩家在线才能获取的数据，在玩家离线时应返回 `null`，而不是返回默认值或占位文本**

这样做的好处：
1. **行为统一** - 所有变量保持一致的行为模式
2. **语义清晰** - `null` 明确表示"数据不可用"，而不是"数据为0"
3. **避免误导** - 防止展示错误信息（如离线玩家显示 0 次亲吻，实际可能有数据）
4. **符合最佳实践** - PlaceholderAPI 推荐对不可用数据返回 `null`

---

## 修改内容

### 1. TSLPlaceholderExpansion.kt

#### 修改前后对比

**Kiss 变量（修改前）：**
```kotlin
"kiss_count" -> return kissManager.getKissCount(player.uniqueId).toString()
"kissed_count" -> return kissManager.getKissedCount(player.uniqueId).toString()
"kiss_toggle" -> {
    val onlinePlayer = Bukkit.getPlayer(player.uniqueId)
    return if (onlinePlayer != null) {
        if (kissManager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
    } else {
        "离线"  // ❌ 返回固定文本
    }
}
```

**Kiss 变量（修改后）：**
```kotlin
"kiss_count" -> {
    val onlinePlayer = player.player ?: return null  // ✅ 离线返回 null
    return kissManager.getKissCount(onlinePlayer.uniqueId).toString()
}
"kissed_count" -> {
    val onlinePlayer = player.player ?: return null  // ✅ 离线返回 null
    return kissManager.getKissedCount(onlinePlayer.uniqueId).toString()
}
"kiss_toggle" -> {
    val onlinePlayer = player.player ?: return null  // ✅ 离线返回 null
    return if (kissManager.isPlayerEnabled(onlinePlayer)) "启用" else "禁用"
}
```

**其他变量（类似修改）：**
- `ride_toggle` - 改为离线返回 `null`
- `toss_toggle` - 改为离线返回 `null`
- `toss_velocity` - 改为离线返回 `null`
- `blocks_placed_total` - 改为离线返回 `null`
- `newbie_tag` - 改为离线返回 `null`

#### 代码优化
- **移除不必要的导入**：删除 `import org.bukkit.Bukkit`（不再需要 `Bukkit.getPlayer()`）
- **统一代码风格**：所有变量使用 `player.player ?: return null` 模式

---

### 2. BlockStatsManager.kt

#### 删除冗余方法

**移除方法：**
```kotlin
fun getTotalBlocksPlacedString(player: Player?): String {
    if (player == null) return "0"
    if (!isEnabled()) return "0"
    return getTotalBlocksPlaced(player).toString()
}
```

**原因：**
- 该方法在 `TSLPlaceholderExpansion` 中不再使用
- 直接调用 `getTotalBlocksPlaced(player).toString()` 即可
- 减少代码冗余，提高可维护性

---

## 技术细节

### OfflinePlayer vs Player

```kotlin
override fun onRequest(player: OfflinePlayer?, params: String): String? {
    // player 是 OfflinePlayer 类型，可能不在线
    
    // 获取在线玩家实例
    val onlinePlayer: Player = player.player ?: return null
    
    // 现在可以安全使用 Player 的方法
}
```

### 返回值规范

| 场景 | 返回值 | 说明 |
|------|--------|------|
| 玩家在线且有数据 | 数据字符串 | 正常返回 |
| 玩家在线但数据为0 | `"0"` | 真实的0值 |
| 玩家离线 | `null` | 数据不可用 |
| 功能未启用 | `null` 或 `"N/A"` | 根据场景决定 |

---

## 影响范围

### 受影响的变量（共8个）
1. `%tsl_kiss_count%`
2. `%tsl_kissed_count%`
3. `%tsl_kiss_toggle%`
4. `%tsl_ride_toggle%`
5. `%tsl_toss_toggle%`
6. `%tsl_toss_velocity%`
7. `%tsl_blocks_placed_total%`
8. `%tsl_newbie_tag%`

### 不受影响的变量
- `%tsl_ping%` - 服务器级别数据，不依赖玩家在线状态
- `%tsl_adv_count%` - 本身已经是正确实现

---

## 使用示例

### 在 TAB 插件中使用

**修改前（可能显示错误信息）：**
```yaml
tablist:
  footer:
    - "亲吻次数: %tsl_kiss_count%"  # 离线玩家会显示 "0"
```

**修改后（离线玩家不显示）：**
```yaml
tablist:
  footer:
    - "{if tsl_kiss_count}亲吻次数: %tsl_kiss_count%{endif}"  # 离线玩家整行隐藏
```

### 在聊天插件中使用

**使用条件判断：**
```yaml
format: "{if tsl_newbie_tag}%tsl_newbie_tag% {endif}{player} : {message}"
# 离线玩家不会显示萌新标志（因为返回 null）
```

---

## 测试建议

### 测试场景

1. **在线玩家测试**
   - 所有变量应正常显示数据
   - 数据为0时应显示 "0"，而不是 null

2. **离线玩家测试**
   - 使用 PlaceholderAPI 的 `/papi parse` 命令测试离线玩家
   - 所有变量应返回空（不显示任何内容）

3. **性能测试**
   - 验证 `player.player` 调用不会造成性能问题
   - 确认没有额外的数据库查询

### 测试命令

```bash
# 在线玩家
/papi parse me %tsl_kiss_count%

# 离线玩家（需要 OfflineExpansion）
/papi parserel <在线玩家> <离线玩家> %rel_tsl_kiss_count%
```

---

## 向后兼容性

### 破坏性变更

**是否有破坏性变更？** 是（轻微）

**影响：**
- 依赖这些变量显示固定文本（如 "离线"、"0"）的配置需要调整
- 建议使用 PlaceholderAPI 的条件表达式 `{if}` 来处理

### 迁移指南

**如果你的配置依赖这些变量显示默认值：**

```yaml
# 旧配置（依赖返回 "离线"）
format: "状态: %tsl_kiss_toggle%"  # 离线会显示 "状态: 离线"

# 新配置（使用条件判断）
format: "{if tsl_kiss_toggle}状态: %tsl_kiss_toggle%{else}状态: 离线{endif}"
```

---

## 开发规范

### 新增 PAPI 变量的标准模式

```kotlin
override fun onRequest(player: OfflinePlayer?, params: String): String? {
    // 1. 不需要玩家的变量（服务器级别）
    if (params == "server_data") {
        return someManager.getServerData()
    }
    
    // 2. 需要玩家的变量
    if (player == null) return null
    
    // 3. 需要玩家在线的变量（推荐模式）
    if (params == "player_data") {
        val onlinePlayer = player.player ?: return null  // ✅ 标准模式
        return someManager.getData(onlinePlayer)
    }
    
    // 4. 可以查询离线玩家的变量（特殊场景）
    if (params == "persistent_data") {
        // 直接使用 player.uniqueId 查询持久化数据
        return dataStore.getData(player.uniqueId)
    }
    
    return null
}
```

### 命名规范

| 变量类型 | 离线行为 | 示例 |
|---------|---------|------|
| 实时状态 | 返回 `null` | `kiss_toggle`, `ride_toggle` |
| 统计数据 | 返回 `null` | `blocks_placed_total` |
| 持久化数据 | 可返回值 | `kiss_count` (但现在也改为只支持在线) |
| 服务器数据 | 返回值 | `ping` |

---

## 总结

### 修改目标
✅ 统一 PlaceholderAPI 变量的行为模式  
✅ 避免离线玩家显示误导信息  
✅ 提高代码一致性和可维护性  

### 技术要点
- 使用 `player.player ?: return null` 检查在线状态
- 删除冗余的 null 检查和默认值返回
- 移除不再使用的辅助方法

### 下一步优化建议
1. 考虑为部分统计数据（如 `kiss_count`）实现离线查询支持
2. 添加 PAPI 变量的单元测试
3. 在文档中明确说明各变量的在线要求

---

**相关模块**: Kiss, Ride, Toss, BlockStats, NewbieTag  
**修改文件**: 
- `TSLPlaceholderExpansion.kt` - 主要修改
- `BlockStatsManager.kt` - 移除冗余方法

