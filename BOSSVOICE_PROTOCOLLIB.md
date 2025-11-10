# Boss 声音范围控制 - ProtocolLib 实现说明

## 技术方案

### ✅ 采用方案：Packet 层拦截（性能最优）

使用 ProtocolLib 在数据包层面拦截声音，直接判断玩家是否应该收到声音包：
- **超出范围** → 取消发送声音包（直接丢弃）
- **在范围内** → 正常发送声音包

### 为什么性能最优？

#### 传统方案（❌ 已弃用）
```kotlin
// 监听 Boss 死亡/生成事件
@EventHandler
fun onEnderDragonDeath(event: EntityDeathEvent) {
    // 遍历所有玩家
    world.players.forEach { player ->
        // 计算距离
        if (player.location.distance(bossLocation) <= range) {
            // 播放声音
            player.playSound(location, sound, volume, pitch)
        }
    }
}
```

**问题**：
- ❌ 需要遍历所有玩家（80个玩家 = 80次循环）
- ❌ 需要多次计算距离（80次 distance 计算）
- ❌ 需要多次播放声音（80次 playSound 调用）
- ❌ 可能需要 stopSound 停止默认声音
- ❌ 使用 scheduler 调度器额外开销

#### Packet 方案（✅ 当前实现）
```kotlin
// 拦截声音数据包
override fun onPacketSending(event: PacketEvent) {
    // 检查是否是 Boss 声音
    if (isBossSound(packet)) {
        // 检查接收者（单个玩家）是否在范围内
        if (!isInRange(player.location, soundLocation, range)) {
            // 取消发送（直接丢弃包）
            event.isCancelled = true
        }
    }
}
```

**优势**：
- ✅ 每个声音只产生**一次包判定**（服务器发包时）
- ✅ 每个玩家只判定**一次**（是否取消发送）
- ✅ 不需要遍历玩家列表
- ✅ 不需要循环播放声音
- ✅ 不需要 stopSound 或 scheduler
- ✅ 超出范围的玩家**根本不会收到包**（零开销）

### 性能对比

假设有 **80 个在线玩家**，末影龙死亡：

| 方案 | 玩家遍历 | 距离计算 | 声音播放 | 包发送 |
|------|---------|---------|---------|--------|
| **传统方案** | 80 次 | 80 次 | 80 次 | 80 次 |
| **Packet 方案** | 0 次 | 80 次（每个玩家自动判定） | 0 次 | 仅范围内玩家 |

**结论**：Packet 方案只在服务器发包时判定一次，让 ProtocolLib 自动处理每个玩家的包发送决策。

---

## 实现细节

### 1. 拦截的数据包类型

#### a. NAMED_SOUND_EFFECT（命名声音效果）
用于拦截：
- **末影龙死亡声音**：`entity.ender_dragon.death`
- **凋零生成声音**：`entity.wither.spawn`

```kotlin
PacketType.Play.Server.NAMED_SOUND_EFFECT
```

#### b. WORLD_EVENT（世界事件）
用于拦截：
- **末地传送门激活**：事件 ID = `1038`

```kotlin
PacketType.Play.Server.WORLD_EVENT
```

### 2. 声音位置解析

#### NAMED_SOUND_EFFECT 包
```kotlin
// 获取声音类型
val soundEffect = packet.soundEffects.read(0)
val soundKey = soundEffect.key.toString() // 例如: "minecraft:entity.ender_dragon.death"

// 获取声音位置（注意：坐标需要除以8）
val x = packet.integers.read(0) / 8.0
val y = packet.integers.read(1) / 8.0
val z = packet.integers.read(2) / 8.0
```

#### WORLD_EVENT 包
```kotlin
// 获取事件 ID
val eventId = packet.integers.read(0) // 1038 = 末地传送门激活

// 获取事件位置
val blockPosition = packet.blockPositionModifier.read(0)
val location = Location(world, blockPosition.x, blockPosition.y, blockPosition.z)
```

### 3. 范围控制逻辑

```kotlin
val range = config.getDouble("bossvoice.ender-dragon-death", -1.0)

when {
    range == -1.0 -> {
        // 全服都能听到（默认行为，放行）
        return
    }
    range == 0.0 -> {
        // 静音（取消发送）
        event.isCancelled = true
        return
    }
    else -> {
        // 检查玩家是否在范围内
        if (!isInRange(player.location, soundLocation, range)) {
            event.isCancelled = true // 超出范围，取消发送
        }
    }
}
```

### 4. 距离判断

```kotlin
private fun isInRange(playerLoc: Location, targetLoc: Location, range: Double): Boolean {
    // 必须在同一个世界
    if (playerLoc.world != targetLoc.world) return false
    
    // 计算欧几里得距离
    val distance = playerLoc.distance(targetLoc)
    return distance <= range
}
```

---

## 配置说明

### 配置文件示例
```yaml
bossvoice:
  # 末影龙死亡声音范围（格子/方块）
  ender-dragon-death: 500  # 范围 500 格
  
  # 凋零生成声音范围（格子/方块）
  wither-spawn: 500        # 范围 500 格
  
  # 末地传送门激活声音范围（格子/方块）
  end-portal-activate: 500 # 范围 500 格
```

### 配置值说明

| 值 | 效果 | 说明 |
|----|------|------|
| `-1` | 全服都能听到 | 默认行为，不拦截声音包 |
| `0` | 静音 | 取消所有声音包，没有玩家能听到 |
| `> 0` | 指定范围 | 只有范围内的玩家能听到声音 |

### 实际效果

#### 示例1：范围 500 格
```yaml
ender-dragon-death: 500
```
- 距离末影龙死亡位置 **≤ 500 格**的玩家 → ✅ 收到声音包
- 距离末影龙死亡位置 **> 500 格**的玩家 → ❌ 包被取消，听不到

#### 示例2：静音
```yaml
ender-dragon-death: 0
```
- **所有玩家** → ❌ 包被取消，听不到

#### 示例3：全服（默认）
```yaml
ender-dragon-death: -1
```
- **所有玩家** → ✅ 正常收到声音包（原版行为）

---

## 性能分析

### 单次 Boss 事件的性能开销

假设：**80 个在线玩家**，末影龙死亡在坐标 `(0, 64, 0)`

#### Packet 方案（当前）
```
1. 服务器生成末影龙死亡声音包（1次）
2. ProtocolLib 拦截包（1次判定：是否是 Boss 声音）
3. 对每个玩家执行：
   - 检查世界是否相同（简单比较）
   - 计算距离（1次 distance 计算）
   - 决定是否发送（简单 boolean 判断）
4. 发送包给范围内的玩家（假设 20 人在范围内 = 20 次网络发送）

总开销：
- 包判定：1 次
- 距离计算：80 次（每个玩家）
- 网络发送：20 次（仅范围内）
```

#### 传统方案（已弃用）
```
1. 监听末影龙死亡事件（1次）
2. 停止默认声音（80 次 stopSound）
3. 遍历所有玩家（80 次循环）
   - 计算距离（80 次 distance 计算）
   - 播放声音（20 次 playSound，假设 20 人在范围内）
4. 服务器发送新的声音包（20 次网络发送）

总开销：
- 事件处理：1 次
- stopSound：80 次
- 距离计算：80 次
- playSound：20 次
- 网络发送：20 次
```

### 结论
Packet 方案避免了：
- ❌ 80 次 `stopSound` 调用
- ❌ 手动遍历玩家列表
- ❌ 20 次 `playSound` 调用
- ✅ 只需要让 ProtocolLib 自动判定包发送

---

## 依赖要求

### ProtocolLib
- **版本**：5.3.0+
- **作用**：提供数据包拦截 API
- **下载**：https://www.spigotmc.org/resources/protocollib.1997/

### plugin.yml 配置
```yaml
depend: [ ProtocolLib ]
```

插件会在启动时检查 ProtocolLib 是否存在，如果不存在则无法启动。

---

## 工作流程

```
1. 插件启动
   ↓
2. 初始化 BossvoiceListener
   ↓
3. 注册 ProtocolLib 数据包监听器
   ↓
4. Boss 事件发生（末影龙死亡/凋零生成/传送门激活）
   ↓
5. 服务器生成声音数据包
   ↓
6. ProtocolLib 拦截包 → 触发 onPacketSending()
   ↓
7. 判断是否是 Boss 声音 + 获取范围配置
   ↓
8. 对每个接收者（玩家）：
   - 检查玩家是否在范围内
   - 在范围内 → 正常发送包
   - 超出范围 → 取消发送（event.isCancelled = true）
   ↓
9. 玩家客户端收到声音包 → 播放声音
```

---

## 注意事项

1. **必须安装 ProtocolLib**
   - 插件依赖 ProtocolLib 才能工作
   - 如果服务器没有 ProtocolLib，插件将无法启动

2. **范围单位是方块（格子）**
   - `500` = 500 个方块的距离
   - 使用欧几里得距离（直线距离）

3. **跨世界不会听到**
   - 即使范围设置为 `-1`（全服），不同世界的玩家也不会听到

4. **性能优化**
   - 使用 Packet 层拦截，性能开销极低
   - 适合大型服务器（100+ 在线玩家）

---

## 相关文件

- `BossvoiceListener.kt` - Packet 拦截器（核心实现）
- `TSLplugins.kt` - 插件主类（初始化监听器）
- `config.yml` - 配置文件（范围设置）
- `build.gradle.kts` - 构建脚本（ProtocolLib 依赖）
- `plugin.yml` - 插件描述（声明依赖）

