# ChatBubble 气泡堆叠功能更新

**日期**: 2025-11-26  
**更新类型**: 功能增强

---

## 🎯 更新内容

### 新增功能：气泡堆叠

当玩家连续发言时，旧气泡会向上移动，新气泡显示在原位置，形成堆叠效果。

---

## 📋 实现细节

### 1. 数据结构改变

**之前**：
```kotlin
private val bubbles: MutableMap<Player, TextDisplay> = ConcurrentHashMap()
// 每个玩家只有一个气泡，新消息会替换旧气泡
```

**现在**：
```kotlin
private val bubbleStacks: MutableMap<Player, MutableList<TextDisplay>> = ConcurrentHashMap()
// 每个玩家可以有多个气泡，形成堆叠
```

---

### 2. 堆叠逻辑

```kotlin
fun createOrUpdateBubble(player: Player, message: Component) {
    // 1. 获取或创建玩家的气泡栈
    val stack = bubbleStacks.getOrPut(player) { mutableListOf() }
    
    // 2. 移除失效的气泡
    stack.removeAll { !it.isValid }
    
    // 3. 如果达到最大堆叠数，移除最旧的（最上面的）
    if (stack.size >= maxStackSize) {
        stack.lastOrNull()?.remove()
        stack.removeLastOrNull()
    }
    
    // 4. 创建新气泡（在最底部，索引0）
    val display = player.world.spawn(location, TextDisplay::class.java) { ... }
    
    // 5. 添加到栈的最前面（最底部）
    stack.add(0, display)
    
    // 6. 启动更新任务，根据索引计算高度
    player.scheduler.runAtFixedRate(plugin, { task ->
        val index = stack.indexOf(display)
        if (index >= 0) {
            // 更新位置（根据索引计算高度）
            display.teleportAsync(calculateBubbleLocation(player, index))
        }
    }, null, 1L, updateTicks)
}
```

---

### 3. 位置计算

```kotlin
private fun calculateBubbleLocation(player: Player, stackIndex: Int): Location {
    val extraHeight = stackIndex * stackSpacing
    return player.location.add(
        0.0, 
        player.boundingBox.height + yOffset + extraHeight,  // 基础高度 + 堆叠偏移
        0.0
    ).apply {
        yaw = 0f
        pitch = 0f
    }
}
```

**示例**：
- 气泡0（最新）：`yOffset + 0 * spacing` = 0.75 方块
- 气泡1（旧）：`yOffset + 1 * spacing` = 1.05 方块
- 气泡2（最旧）：`yOffset + 2 * spacing` = 1.35 方块

---

### 4. 新增配置

```yaml
chatbubble:
  # ... 其他配置 ...
  
  # 气泡堆叠配置
  stack:
    # 最大堆叠数量（连续发言时旧气泡会向上推）
    maxSize: 3
    # 堆叠间距（单位：方块，每个气泡之间的垂直距离）
    spacing: 0.3
```

**配置说明**：
- `maxSize: 3` - 最多堆叠 3 个气泡，第 4 条消息会移除最旧的
- `spacing: 0.3` - 每个气泡之间相距 0.3 方块（约 6 像素）

---

## 🎬 效果演示

### 场景 1：单条消息
```
玩家: "你好"
效果:
  ┌─────┐
  │你好 │ ← 气泡0（索引0，高度 0.75）
  └─────┘
    👤
```

### 场景 2：连续两条消息
```
玩家: "你好"
玩家: "怎么样"

效果:
  ┌─────┐
  │你好 │ ← 气泡1（索引1，高度 1.05，向上推了）
  └─────┘
  ┌────┐
  │怎么样│ ← 气泡0（索引0，高度 0.75，新消息）
  └────┘
    👤
```

### 场景 3：连续三条消息
```
玩家: "你好"
玩家: "怎么样"
玩家: "在吗"

效果:
  ┌─────┐
  │你好 │ ← 气泡2（索引2，高度 1.35，最旧）
  └─────┘
  ┌────┐
  │怎么样│ ← 气泡1（索引1，高度 1.05）
  └────┘
  ┌────┐
  │在吗 │ ← 气泡0（索引0，高度 0.75，最新）
  └────┘
    👤
```

### 场景 4：超过最大堆叠数
```
玩家: "你好"
玩家: "怎么样"
玩家: "在吗"
玩家: "听得到吗"

效果:
  （"你好" 气泡被移除）
  ┌────┐
  │怎么样│ ← 气泡2（索引2，高度 1.35）
  └────┘
  ┌────┐
  │在吗 │ ← 气泡1（索引1，高度 1.05）
  └────┘
  ┌─────┐
  │听得到吗│ ← 气泡0（索引0，高度 0.75，最新）
  └─────┘
    👤
```

---

## 🔧 技术要点

### 1. 动态位置更新

每个气泡的更新任务会：
- 检查自己在栈中的当前索引
- 根据索引计算应该在的高度
- 平滑移动到目标位置（`teleportAsync`）

```kotlin
val index = stack.indexOf(display)
if (index >= 0) {
    display.teleportAsync(calculateBubbleLocation(player, index))
}
```

**为什么需要动态计算**？
- 旧气泡会被新气泡"推"上去
- 气泡过期消失后，上方的气泡需要"掉"下来
- 索引是动态变化的

---

### 2. 自动清理机制

**气泡过期**：
```kotlin
if (display.ticksLived > timeSpan) {
    task.cancel()
    display.remove()
    stack.remove(display)  // 从栈中移除
    return@runAtFixedRate
}
```

**玩家退出**：
```kotlin
fun cleanupPlayer(player: Player) {
    bubbleStacks.remove(player)?.forEach { it.remove() }
    selfDisplayEnabled.remove(player)
}
```

---

### 3. 线程安全

使用 `ConcurrentHashMap` 和 `MutableList`：
```kotlin
private val bubbleStacks: MutableMap<Player, MutableList<TextDisplay>> = ConcurrentHashMap()
```

**注意**：
- `MutableList` 本身不是线程安全的
- 但每个玩家的气泡栈只在该玩家的实体调度器中修改
- Folia 的实体调度器保证了对同一玩家的操作在同一线程

---

### 4. 性能考虑

**优化点**：
- 每个气泡独立的更新任务（不会互相阻塞）
- 失效气泡自动清理（`stack.removeAll { !it.isValid }`）
- 最大堆叠数限制（避免无限堆积）

**性能影响**：
- 单气泡模式：每个玩家 1 个更新任务
- 堆叠模式（maxSize=3）：每个玩家最多 3 个更新任务
- 更新频率：默认 2 tick（0.1秒）一次

---

## 📊 配置建议

### 推荐配置（平衡）
```yaml
stack:
  maxSize: 3      # 3条消息足够
  spacing: 0.3    # 0.3方块间距清晰可见
```

### 紧凑配置（适合快速聊天）
```yaml
stack:
  maxSize: 5      # 更多堆叠
  spacing: 0.25   # 更紧凑
```

### 宽松配置（适合慢节奏）
```yaml
stack:
  maxSize: 2      # 只保留2条
  spacing: 0.4    # 间距更大
```

---

## 🐛 已知问题和解决

### 问题：气泡位置抖动

**原因**：多个更新任务同时修改位置

**解决**：每个气泡在自己的更新任务中动态计算索引

---

### 问题：函数名冲突

**原因**：`isInvisible()` 与 Bukkit 的成员函数冲突

**解决**：重命名为 `isInvisibleForBubble()`

```kotlin
// 避免冲突
private fun Player.isInvisibleForBubble(): Boolean {
    return this.gameMode == GameMode.SPECTATOR ||
           this.hasPotionEffect(PotionEffectType.INVISIBILITY)
}
```

---

## ✅ 测试场景

### 测试 1：连续发言
```
1. 玩家快速发送 3 条消息
2. 验证：
   ✅ 3个气泡同时存在
   ✅ 从下到上排列
   ✅ 新消息在最下方
```

### 测试 2：超过最大堆叠
```
1. 玩家快速发送 5 条消息（maxSize=3）
2. 验证：
   ✅ 只有 3 个气泡
   ✅ 最旧的 2 条已消失
```

### 测试 3：气泡过期
```
1. 玩家发送 3 条消息
2. 等待第一条过期
3. 验证：
   ✅ 第一条消失
   ✅ 剩余 2 条向下移动
```

### 测试 4：自我显示切换
```
1. 玩家发送 3 条消息
2. 执行 /tsl chatbubble
3. 验证：
   ✅ 所有气泡隐藏
4. 再次执行命令
5. 验证：
   ✅ 所有气泡显示
```

### 测试 5：玩家退出
```
1. 玩家发送 3 条消息
2. 玩家退出服务器
3. 验证：
   ✅ 所有气泡被移除
   ✅ 无内存泄漏
```

---

## 📝 代码修改总结

### 修改的文件

1. **ChatBubbleManager.kt**
   - 数据结构：`bubbles` → `bubbleStacks`
   - 新增：`maxStackSize`、`stackSpacing` 配置
   - 重构：`createOrUpdateBubble()` 支持堆叠
   - 新增：`calculateBubbleLocation(player, stackIndex)` 支持索引
   - 更新：`toggleSelfDisplay()`、`cleanupPlayer()`、`cleanupAll()` 支持多气泡
   - 修复：`isInvisible()` → `isInvisibleForBubble()` 避免冲突

2. **config.yml**
   - 新增：`chatbubble.stack.maxSize` 配置
   - 新增：`chatbubble.stack.spacing` 配置

---

## 🎉 总结

### 实现的效果

✅ **连续发言时旧气泡向上推**  
✅ **最多堆叠 N 个气泡（可配置）**  
✅ **气泡间距可配置**  
✅ **气泡过期后自动清理**  
✅ **动态位置更新（平滑移动）**  
✅ **性能优化（独立更新任务）**  
✅ **线程安全（Folia 兼容）**

### 用户体验提升

- 📢 **更好的聊天可读性** - 连续消息清晰可见
- 🎨 **更美观的视觉效果** - 气泡堆叠层次分明
- ⚙️ **灵活配置** - 服主可自定义堆叠行为

---

**更新完成时间**: 2025-11-26  
**配置版本**: 12（无需更改）  
**状态**: ✅ 完成并测试通过

