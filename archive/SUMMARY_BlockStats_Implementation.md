# BlockStats 方块统计功能实现总结

**日期**: 2025-11-25  
**功能类型**: PlaceholderAPI 变量 - 轻量级统计

---

## 功能概述

实现了一个轻量级的方块统计功能，提供 PlaceholderAPI 变量 `%tsl_blocks_placed_total%`，用于查询玩家放置方块的总数。

该功能设计用于配合 **Topper 排行榜插件**，为服务器提供玩家方块放置数量的排行榜支持。

---

## 核心特性

### 1. 轻量级设计

- ✅ **无事件监听** - 不监听 BlockPlaceEvent，零性能开销
- ✅ **无数据持久化** - 不使用数据库、不写入文件
- ✅ **实时计算** - 每次查询时实时从原版统计系统读取
- ✅ **零配置需求** - 使用 Minecraft 原版统计数据

### 2. 基于原版统计

使用 Bukkit API 的 `Statistic.USE_ITEM` 统计：
```kotlin
player.getStatistic(Statistic.USE_ITEM, material)
```

**原理**：
- Minecraft 原版会自动统计每个玩家使用（放置）每种物品/方块的次数
- 数据保存在玩家的 `.dat` 文件中（`stats` 部分）
- 服务器重启后数据不丢失
- 跨服同步玩家数据时统计也会同步

### 3. 智能过滤

只统计 `isBlock()` 为 true 的材料：
```kotlin
for (material in Material.entries) {
    if (material.isBlock) {
        // 统计该方块
    }
}
```

**过滤逻辑**：
- ✅ 统计：石头、泥土、木板、玻璃等真正的方块
- ❌ 不统计：物品（如剑、食物）、技术性方块（如 AIR）

---

## 技术实现

### 文件结构

```
BlockStats/
└── BlockStatsManager.kt    # 管理器：配置管理、统计计算
```

**为什么没有 Command 和 Listener？**
- 不需要命令（只提供 PAPI 变量）
- 不需要监听器（使用原版统计，不监听事件）

### BlockStatsManager.kt

```kotlin
class BlockStatsManager(private val plugin: JavaPlugin) {
    
    private var enabled: Boolean = true
    
    /**
     * 计算玩家放置方块总数
     */
    fun getTotalBlocksPlaced(player: Player): Long {
        var total = 0L
        
        // 遍历所有材料
        for (material in Material.entries) {
            // 只统计方块类物品
            if (material.isBlock) {
                try {
                    // 获取该材料的使用次数（即放置次数）
                    val count = player.getStatistic(Statistic.USE_ITEM, material)
                    total += count
                } catch (e: Exception) {
                    // 忽略异常（某些材料可能不支持统计）
                }
            }
        }
        
        return total
    }
    
    /**
     * 获取玩家放置方块总数（字符串格式）
     * 用于 PlaceholderAPI 返回
     */
    fun getTotalBlocksPlacedString(player: Player?): String {
        if (player == null) return "0"
        if (!isEnabled()) return "0"
        
        return getTotalBlocksPlaced(player).toString()
    }
}
```

---

## PlaceholderAPI 变量

### 变量名

```
%tsl_blocks_placed_total%
```

### 使用示例

#### 在 Topper 排行榜中使用

**配置 Topper**（`toppers/blocks_placed.yml`）：
```yaml
name: "方块放置排行榜"
placeholder: "%tsl_blocks_placed_total%"
update-interval: 300  # 5 分钟更新一次
display:
  format: "&e{rank}. &f{player} &7- &a{value} 个方块"
```

#### 在记分板中使用

```yaml
scoreboard:
  lines:
    - "&e你放置的方块总数:"
    - "&a%tsl_blocks_placed_total%"
```

#### 在全息图中使用

```yaml
hologram:
  - "&e&l服务器统计"
  - "&f你放置了 &a%tsl_blocks_placed_total% &f个方块"
```

---

## 集成到核心系统

### 1. TSLplugins.kt

```kotlin
// 导入
import org.tsl.tSLplugins.BlockStats.BlockStatsManager

class TSLplugins : JavaPlugin() {
    // 声明
    private lateinit var blockStatsManager: BlockStatsManager
    
    override fun onEnable() {
        // ...existing code...
        
        // 初始化 BlockStats 系统
        blockStatsManager = BlockStatsManager(this)
        
        // ...existing code...
    }
    
    // 重载方法
    fun reloadBlockStatsManager() {
        blockStatsManager.loadConfig()
    }
}
```

### 2. TSLPlaceholderExpansion.kt

```kotlin
// 导入
import org.tsl.tSLplugins.BlockStats.BlockStatsManager

/**
 * 支持的变量：
 * ...
 * - %tsl_blocks_placed_total% - 玩家放置方块总数
 */
class TSLPlaceholderExpansion(
    // ...existing params...
    private val blockStatsManager: BlockStatsManager?
) : PlaceholderExpansion() {
    
    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        // ...existing code...
        
        // === BlockStats 变量 ===
        if (blockStatsManager != null && params.equals("blocks_placed_total", ignoreCase = true)) {
            val onlinePlayer = Bukkit.getPlayer(player.uniqueId)
            return blockStatsManager.getTotalBlocksPlacedString(onlinePlayer)
        }
        
        return null
    }
}
```

### 3. ReloadCommand.kt

```kotlin
// 重新加载 BlockStats 功能
plugin.reloadBlockStatsManager()
```

### 4. ConfigUpdateManager.kt

```kotlin
companion object {
    const val CURRENT_CONFIG_VERSION = 11  // 从 10 递增到 11
}
```

### 5. config.yml

```yaml
# ========================================
# BlockStats 方块统计功能配置
# ========================================
blockstats:
  # 是否启用 BlockStats 功能
  enabled: true
```

---

## 性能分析

### 计算开销

**遍历所有材料**：
- Minecraft 1.21 约有 1200+ 种材料
- 其中约 800+ 种是方块
- 每个方块调用一次 `getStatistic()`

**预估性能**：
- 单次查询耗时：< 1ms（现代服务器）
- 推荐更新间隔：5 分钟（Topper 默认）

### 优化策略

1. **Topper 自带缓存**
   - Topper 插件会缓存排行榜结果
   - 不会每秒都查询所有玩家
   - 按配置的间隔更新（如 5 分钟）

2. **按需计算**
   - 只在查询时计算
   - 不会持续消耗资源
   - 离线玩家不计算

3. **原版统计系统**
   - 使用 Minecraft 内置统计
   - 无需额外监听事件
   - 无需额外存储数据

---

## 使用场景

### 1. 排行榜系统

**Topper 插件示例**：
```yaml
# toppers/blocks_placed.yml
name: "建筑大师排行榜"
placeholder: "%tsl_blocks_placed_total%"
update-interval: 300
display:
  format: "&e#{rank} &f{player} &7- &a{value} 方块"
  hologram:
    - "&e&l建筑大师排行榜"
    - "&f前 10 名最勤劳的建设者"
```

### 2. 成就系统

```yaml
achievements:
  builder_novice:
    name: "建筑新手"
    requirement: "%tsl_blocks_placed_total% >= 1000"
  builder_expert:
    name: "建筑专家"
    requirement: "%tsl_blocks_placed_total% >= 10000"
  builder_master:
    name: "建筑大师"
    requirement: "%tsl_blocks_placed_total% >= 100000"
```

### 3. 记分板显示

```yaml
scoreboard:
  title: "&e&l服务器统计"
  lines:
    - "&f你的建筑统计:"
    - "&a方块放置: &e%tsl_blocks_placed_total%"
    - ""
    - "&7排名更新: 每 5 分钟"
```

---

## 限制和注意事项

### 1. 统计准确性

✅ **准确的情况**：
- 玩家手动放置方块
- 使用 `/give` 后放置方块
- 创造模式放置方块

❌ **不准确的情况**：
- 使用 WorldEdit 等插件批量放置
- 使用建筑插件（如 Builder）
- 管理员使用指令生成建筑

**原因**：这些插件通常绕过原版统计系统

### 2. 数据来源

- **数据保存位置**：`world/playerdata/<UUID>.dat` 中的 `stats` NBT 标签
- **数据持久化**：由 Minecraft 原版管理，服务器重启不丢失
- **跨服同步**：如果使用共享玩家数据，统计会同步

### 3. 计算开销

- **查询一次约需遍历 800+ 种方块**
- **建议配合 Topper 的缓存机制使用**
- **不推荐高频率查询（如每秒）**
- **适合低频更新的排行榜（如 5 分钟一次）**

---

## 与其他功能对比

| 功能 | 事件监听 | 数据存储 | 计算方式 | 准确性 |
|------|---------|---------|---------|-------|
| **BlockStats** | ❌ 无 | ❌ 无（原版） | 实时计算 | 高（原版统计） |
| Advancement | ✅ 有 | ✅ 内存缓存 | 实时读取 | 高 |
| Kiss | ✅ 有 | ✅ PDC 持久化 | 累加统计 | 完美 |
| Freeze | ✅ 有 | ✅ 内存映射 | 状态管理 | 完美 |

**BlockStats 的独特性**：
- 最轻量（无监听器、无存储）
- 依赖原版系统
- 适合简单的统计需求

---

## 开发流程回顾

遵循了开发者指南中的标准流程：

1. ✅ 创建模块包结构
2. ✅ 实现 Manager（配置缓存、loadConfig 方法）
3. ⚪ 跳过 Command（不需要命令）
4. ⚪ 跳过 Listener（不需要监听器）
5. ✅ 修改 TSLplugins.kt（初始化、注册、添加 reload 方法）
6. ✅ 修改 ReloadCommand.kt（添加重载调用）
7. ✅ 添加配置到 config.yml
8. ✅ 更新 ConfigUpdateManager.kt（递增版本号到 11）
9. ⚪ 跳过 PlayerDataManager.kt（不需要 PDC）
10. ✅ 修改 TSLPlaceholderExpansion.kt（添加 PAPI 变量）

---

## 测试建议

### 1. 基础测试

```bash
# 1. 启动服务器，检查日志
[BlockStats] 配置已加载 - 启用: true

# 2. 测试 PAPI 变量
/papi parse me %tsl_blocks_placed_total%
# 应该返回数字，如：1234

# 3. 测试重载
/tsl reload
# 应该显示所有模块已重载
```

### 2. 功能测试

```bash
# 1. 放置一些方块（如石头、木板）
# 2. 查询统计
/papi parse me %tsl_blocks_placed_total%
# 数字应该增加

# 3. 退出并重新登录
# 4. 再次查询
/papi parse me %tsl_blocks_placed_total%
# 数字应该保持（数据持久化）
```

### 3. Topper 集成测试

```bash
# 1. 配置 Topper 排行榜
# 2. 等待更新（5 分钟）
# 3. 查看排行榜
/topper view blocks_placed
# 应该显示玩家排名
```

---

## 未来扩展可能

### 可选功能（如有需求）

1. **添加命令查询**
   ```kotlin
   // BlockStatsCommand.kt
   /tsl blockstats [玩家]  // 查询方块统计
   ```

2. **添加分类统计**
   ```kotlin
   fun getStoneBlocksPlaced(player: Player): Long
   fun getWoodBlocksPlaced(player: Player): Long
   ```

3. **添加排行榜命令**
   ```kotlin
   /tsl blockstats top  // 显示前 10 名
   ```

但根据需求文档，当前实现（仅 PAPI 变量）已经满足需求。

---

## 总结

### 实现成果

- ✅ 新增 BlockStats 模块（轻量级设计）
- ✅ 提供 PAPI 变量 `%tsl_blocks_placed_total%`
- ✅ 基于原版统计系统（零开销）
- ✅ 完整集成到核心系统
- ✅ 配置版本更新到 11

### 设计亮点

1. **极简设计** - 只有一个 Manager 类，无 Command、Listener
2. **零性能开销** - 不监听事件、不存储数据
3. **即插即用** - 依赖原版统计，无需额外配置
4. **符合规范** - 完全遵循项目的架构模式

### 适用场景

- ✅ Topper 排行榜插件
- ✅ 简单的统计展示
- ✅ 成就系统需求
- ❌ 不适合实时监控（计算开销）
- ❌ 不适合需要 100% 准确的场景（WorldEdit 等插件会绕过）

---

**实现完成时间**: 2025-11-25  
**配置版本**: 11  
**新增功能**: BlockStats 方块统计  
**新增变量**: `%tsl_blocks_placed_total%`

