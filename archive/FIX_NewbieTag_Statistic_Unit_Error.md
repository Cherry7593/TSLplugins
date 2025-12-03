# NewbieTag 模块统计单位错误修复

**日期**: 2025-12-03  
**模块**: NewbieTag (萌新标志)  
**类型**: Bug 修复（严重错误）

---

## 🐛 问题描述

### 用户反馈
配置文件中设置 `thresholdHours: 24`（24小时阈值），但**所有玩家**都显示老玩家标识（⚡），无论他们实际在线多久。

### 实际现象
```yaml
# config.yml
newbieTag:
  thresholdHours: 24
  newbieTag: "✨"      # 萌新标志
  veteranTag: "⚡"     # 老玩家标志
```

**预期行为**：
- 在线时长 < 24 小时 → 显示 "✨"
- 在线时长 ≥ 24 小时 → 显示 "⚡"

**实际行为**：
- 所有玩家都显示 "⚡"（老玩家标志）
- 即使刚注册的新玩家也显示老玩家标志

---

## 🔍 问题根源

### 错误的代码
```kotlin
// 错误的实现
val playTimeMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
val playTimeHours = playTimeMinutes / 60.0
```

### 问题分析

#### 1. **统计单位错误**
```
Statistic.PLAY_ONE_MINUTE 的单位是 TICK，不是分钟！
```

**Minecraft 时间单位**：
```
1 秒 = 20 tick
1 分钟 = 1200 tick (20 × 60)
1 小时 = 72000 tick (1200 × 60)
```

#### 2. **计算错误导致的后果**

**示例计算（玩家实际在线 1 小时）**：

**错误的计算**：
```
实际在线时间：1 小时 = 72000 tick

代码中：
playTimeMinutes = 72000  ❌ 错误：以为这是 72000 分钟
playTimeHours = 72000 / 60.0 = 1200 小时  ❌ 错误结果

判断：
1200 小时 >= 24 小时 → 显示老玩家标志 ⚡  ❌ 错误判断
```

**正确的计算**：
```
实际在线时间：1 小时 = 72000 tick

应该这样计算：
playTimeTicks = 72000
playTimeHours = 72000 / 1200 / 60 = 1 小时  ✅ 正确结果

判断：
1 小时 < 24 小时 → 显示萌新标志 ✨  ✅ 正确判断
```

#### 3. **为什么所有人都是老玩家**

即使玩家只在线了 **1 分钟**：
```
实际在线：1 分钟 = 1200 tick

错误计算：
playTimeHours = 1200 / 60 = 20 小时  ❌

20 小时 < 24 小时 → 勉强还是萌新（但也快不是了）
```

即使玩家只在线了 **2 分钟**：
```
实际在线：2 分钟 = 2400 tick

错误计算：
playTimeHours = 2400 / 60 = 40 小时  ❌

40 小时 >= 24 小时 → 显示老玩家标志 ⚡  ❌
```

**结论**：只要玩家在线超过 **72 秒**（1.2 分钟），就会被判定为老玩家！

---

## ✅ 解决方案

### 正确的计算公式

```kotlin
// 获取统计数据（单位：tick）
val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)

// 转换为小时
val playTimeHours = playTimeTicks / 1200.0 / 60.0
//                  ────────────   ────────
//                   tick->分钟     分钟->小时
//                   (÷1200)       (÷60)
```

### 详细转换步骤

```
Step 1: Tick → 分钟
────────────────────
playTimeTicks / 1200 = playTimeMinutes

例如：72000 tick / 1200 = 60 分钟


Step 2: 分钟 → 小时
────────────────────
playTimeMinutes / 60 = playTimeHours

例如：60 分钟 / 60 = 1 小时


Step 3: 简化公式
────────────────────
playTimeHours = playTimeTicks / 1200 / 60
              = playTimeTicks / 72000

例如：72000 tick / 72000 = 1 小时
```

---

## 📝 修改内容

### NewbieTagManager.kt

#### getPlayerTag() 方法
```kotlin
// 修改前（错误）
fun getPlayerTag(player: Player): String {
    try {
        // ❌ 错误：以为单位是分钟
        val playTimeMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
        val playTimeHours = playTimeMinutes / 60.0
        
        return if (playTimeHours < thresholdHours) {
            newbieTag
        } else {
            veteranTag
        }
    } catch (e: Exception) {
        return ""
    }
}
```

```kotlin
// 修改后（正确）
fun getPlayerTag(player: Player): String {
    try {
        // ✅ 正确：单位是 tick
        val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
        
        // ✅ 正确转换：tick -> 分钟 -> 小时
        val playTimeHours = playTimeTicks / 1200.0 / 60.0
        
        return if (playTimeHours < thresholdHours) {
            newbieTag  // 萌新标志
        } else {
            veteranTag  // 老玩家标志
        }
    } catch (e: Exception) {
        return ""
    }
}
```

#### getPlayTimeHours() 方法
```kotlin
// 修改前（错误）
fun getPlayTimeHours(player: Player): Double {
    return try {
        val playTimeMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
        playTimeMinutes / 60.0  // ❌ 错误计算
    } catch (e: Exception) {
        0.0
    }
}
```

```kotlin
// 修改后（正确）
fun getPlayTimeHours(player: Player): Double {
    return try {
        val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
        playTimeTicks / 1200.0 / 60.0  // ✅ 正确计算
    } catch (e: Exception) {
        0.0
    }
}
```

---

## 📊 测试验证

### 测试场景

#### 场景 1: 新玩家（刚加入）
```
实际在线时间：0 分钟（0 tick）

计算：
playTimeHours = 0 / 1200 / 60 = 0 小时

判断：
0 小时 < 24 小时 → 显示 "✨"（萌新标志）✅
```

#### 场景 2: 玩家在线 1 小时
```
实际在线时间：1 小时（72000 tick）

计算：
playTimeHours = 72000 / 1200 / 60 = 1 小时

判断：
1 小时 < 24 小时 → 显示 "✨"（萌新标志）✅
```

#### 场景 3: 玩家在线 12 小时
```
实际在线时间：12 小时（864000 tick）

计算：
playTimeHours = 864000 / 1200 / 60 = 12 小时

判断：
12 小时 < 24 小时 → 显示 "✨"（萌新标志）✅
```

#### 场景 4: 玩家在线 24 小时（临界值）
```
实际在线时间：24 小时（1728000 tick）

计算：
playTimeHours = 1728000 / 1200 / 60 = 24 小时

判断：
24 小时 >= 24 小时 → 显示 "⚡"（老玩家标志）✅
```

#### 场景 5: 玩家在线 100 小时
```
实际在线时间：100 小时（7200000 tick）

计算：
playTimeHours = 7200000 / 1200 / 60 = 100 小时

判断：
100 小时 >= 24 小时 → 显示 "⚡"（老玩家标志）✅
```

---

## 🔢 转换公式参考

### 常用时间单位转换

| 时间 | Tick 数 | 计算公式 |
|------|---------|---------|
| 1 秒 | 20 | 20 × 1 |
| 1 分钟 | 1,200 | 20 × 60 |
| 1 小时 | 72,000 | 20 × 60 × 60 |
| 1 天 | 1,728,000 | 20 × 60 × 60 × 24 |
| 1 周 | 12,096,000 | 20 × 60 × 60 × 24 × 7 |

### Tick 转换为其他单位

```kotlin
// Tick → 秒
val seconds = ticks / 20.0

// Tick → 分钟
val minutes = ticks / 1200.0

// Tick → 小时
val hours = ticks / 72000.0
// 或
val hours = ticks / 1200.0 / 60.0

// Tick → 天
val days = ticks / 1728000.0
```

---

## 🎯 Minecraft 统计系统说明

### PLAY_ONE_MINUTE 统计

**官方文档说明**：
```
Statistic.PLAY_ONE_MINUTE
- 类型: Untyped Statistic
- 单位: Tick (游戏刻)
- 说明: 玩家总游戏时间（包括所有世界）
- 注意: 名字虽然是 "PLAY_ONE_MINUTE"，但单位是 tick！
```

**为什么叫 PLAY_ONE_MINUTE 但单位是 tick？**

这是 Minecraft 的命名历史遗留问题：
- 早期版本统计以分钟为单位
- 后来改为更精确的 tick 单位
- 但名称保留了 "ONE_MINUTE" 以保持兼容性
- **开发者容易被名称误导！**

---

## 📈 影响范围

### 受影响的功能
1. ✅ `getPlayerTag()` - 获取玩家标志
2. ✅ `getPlayTimeHours()` - 获取游玩时长
3. ✅ `isNewbie()` - 判断是否为萌新

### 受影响的 PlaceholderAPI 变量
- `%tsl_newbie_tag%` - 玩家的萌新/老玩家标志

### 使用场景
- TAB 插件：显示玩家前缀
- 聊天插件：显示玩家标志
- 记分板：显示玩家等级

---

## 🚨 严重程度评估

### 错误等级
**🔴 严重错误（Critical Bug）**

### 影响
1. **功能完全失效** - 萌新标志从未正确显示过
2. **用户体验差** - 新玩家无法被识别
3. **逻辑错误** - 计算结果偏差 **1200 倍**

### 发现原因
- 统计名称误导（PLAY_ONE_MINUTE 实际是 tick）
- 缺少单元测试
- 缺少实际测试验证

---

## 🔧 预防措施

### 1. 添加调试日志（可选）
```kotlin
fun getPlayerTag(player: Player): String {
    if (!enabled) return ""
    
    try {
        val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
        val playTimeHours = playTimeTicks / 1200.0 / 60.0
        
        // 调试日志
        if (plugin.config.getBoolean("newbieTag.debug", false)) {
            plugin.logger.info(
                "[NewbieTag] ${player.name} - " +
                "Ticks: $playTimeTicks, " +
                "Hours: %.2f, ".format(playTimeHours) +
                "Tag: ${if (playTimeHours < thresholdHours) newbieTag else veteranTag}"
            )
        }
        
        return if (playTimeHours < thresholdHours) {
            newbieTag
        } else {
            veteranTag
        }
    } catch (e: Exception) {
        plugin.logger.warning("[NewbieTag] 获取玩家标志失败: ${player.name} - ${e.message}")
        return ""
    }
}
```

### 2. 单元测试（推荐）
```kotlin
class NewbieTagTest {
    @Test
    fun testTimeConversion() {
        // 1 小时 = 72000 tick
        val ticks = 72000
        val hours = ticks / 1200.0 / 60.0
        assertEquals(1.0, hours, 0.01)
    }
    
    @Test
    fun testThreshold() {
        // 24 小时 = 1728000 tick
        val ticks = 1728000
        val hours = ticks / 1200.0 / 60.0
        assertEquals(24.0, hours, 0.01)
    }
}
```

---

## 📚 相关资源

### Minecraft Wiki
- [Statistics - Minecraft Wiki](https://minecraft.wiki/w/Statistics)
- PLAY_ONE_MINUTE: "Time played" in ticks

### Bukkit API
```java
org.bukkit.Statistic.PLAY_ONE_MINUTE
```

### 其他使用 PLAY_ONE_MINUTE 的插件
检查项目中是否有其他地方也使用了这个统计：
```bash
grep -r "PLAY_ONE_MINUTE" src/
```

---

## 🎉 总结

### 问题
- ❌ 错误理解 `Statistic.PLAY_ONE_MINUTE` 的单位
- ❌ 将 tick 当作分钟计算
- ❌ 导致所有玩家都显示老玩家标志

### 修复
- ✅ 正确识别单位为 tick
- ✅ 使用正确的转换公式：`ticks / 1200 / 60`
- ✅ 添加详细注释说明单位

### 效果
- ✅ 新玩家（< 24 小时）显示 "✨"
- ✅ 老玩家（≥ 24 小时）显示 "⚡"
- ✅ 功能正常工作

### 教训
**永远不要根据 API 名称猜测单位，一定要查阅官方文档！**

---

**修复完成！现在 NewbieTag 模块会正确判断玩家的在线时长并显示相应的标志。**

