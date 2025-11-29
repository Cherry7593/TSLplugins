# Folia 线程安全开发规范

> 基于 ChatBubble 模块实战经验总结的 Folia 多线程编程最佳实践

---

## 📋 目录

1. [核心概念](#核心概念)
2. [黄金法则](#黄金法则)
3. [常见陷阱](#常见陷阱)
4. [最佳实践](#最佳实践)
5. [调试技巧](#调试技巧)
6. [实战案例](#实战案例)

---

## 核心概念

### Folia 的区域线程模型

```
传统 Paper 服务器：
┌─────────────────────────────┐
│  主线程 (Main Thread)        │
│  - 所有实体                  │
│  - 所有区块                  │
│  - 所有任务                  │
└─────────────────────────────┘

Folia 服务器：
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Region A    │  │  Region B    │  │  Region C    │
│  Thread #1   │  │  Thread #2   │  │  Thread #3   │
│  - 实体 1-10 │  │  - 实体11-20 │  │  - 实体21-30 │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 关键理解

1. **每个 Region 运行在独立线程**
2. **实体绑定到所在 Region**
3. **跨 Region 访问实体 = 跨线程访问 = 错误**
4. **传送 = 实体跨 Region 移动**

---

## 黄金法则

### 法则 1：使用正确的调度器

```kotlin
// ❌ 错误：在错误的调度器上操作实体
player.scheduler.runDelayed {
    entity.remove()  // entity 可能已在其他 Region
}

// ✅ 正确：使用实体自己的调度器
entity.scheduler.runDelayed {
    entity.remove()  // 任务跟随实体移动
}
```

**调度器选择原则**：
- 操作**实体** → 使用 `entity.scheduler`
- 操作**玩家** → 使用 `player.scheduler`
- 操作**区块** → 使用 `chunk.scheduler`
- 操作**全局** → 使用 `plugin.server.globalRegionScheduler`

### 法则 2：永远不要直接访问跨 Region 的实体

```kotlin
// ❌ 错误：直接访问可能在其他 Region 的实体
fun someMethod() {
    val entity = getEntitySomewhere()
    entity.remove()  // 可能跨线程
    entity.teleport(...)  // 可能跨线程
    entity.isValid  // 可能跨线程
}

// ✅ 正确：使用调度器访问
fun someMethod() {
    val entity = getEntitySomewhere()
    entity.scheduler.run(plugin, { _ ->
        if (entity.isValid) {
            entity.remove()
        }
    }, null)
}
```

### 法则 3：传送 = 跨 Region = 清理旧数据

```kotlin
// ✅ 传送监听器必须清理旧数据
@EventHandler
fun onPlayerTeleport(event: PlayerTeleportEvent) {
    val player = event.player
    
    // 清理所有与玩家相关的实体引用
    cleanupPlayerEntities(player)
}
```

### 法则 4：所有跨线程操作必须用 try-catch

```kotlin
// ✅ 防御性编程
try {
    entity.scheduler.run(plugin, { _ ->
        try {
            if (entity.isValid) {
                entity.remove()
            }
        } catch (e: Exception) {
            // 内层：实体操作失败
        }
    }, null)
} catch (e: Exception) {
    // 外层：调度失败（实体已删除等）
}
```

---

## 常见陷阱

### 陷阱 1：使用玩家调度器删除跟随实体

**问题场景**：
```kotlin
// 创建跟随玩家的实体（如气泡、宠物）
val display = world.spawn(location, TextDisplay::class.java)
player.addPassenger(display)

// ❌ 错误：5秒后删除
player.scheduler.runDelayed(plugin, { _ ->
    display.remove()  // 玩家可能已传送，display 在其他 Region
}, null, 100L)
```

**为什么错误**：
1. 任务注册在玩家当前所在的 Region A
2. 玩家传送到 Region B（display 跟随）
3. 5秒后，Region A 执行任务
4. 尝试删除在 Region B 的 display → 跨线程错误

**正确做法**：
```kotlin
// ✅ 使用实体自己的调度器
display.scheduler.runDelayed(plugin, { _ ->
    try {
        if (display.isValid) {
            display.remove()
        }
    } catch (e: Exception) {}
}, null, 100L)
```

### 陷阱 2：手动操作 Passenger 关系

**问题场景**：
```kotlin
// ❌ 错误：手动移除 passenger
player.scheduler.runDelayed {
    player.removePassenger(display)  // 需要访问 display 的状态
    display.remove()
}
```

**为什么错误**：
- `removePassenger()` 需要访问 display 的内部状态
- 如果 display 在其他 Region → 跨线程错误

**正确做法**：
```kotlin
// ✅ 直接删除实体，让引擎自动清理 passenger 关系
display.scheduler.run(plugin, { _ ->
    try {
        if (display.isValid) {
            display.remove()  // 引擎自动处理 passenger 关系
        }
    } catch (e: Exception) {}
}, null)
```

### 陷阱 3：在外部检查 isValid

**问题场景**：
```kotlin
// ❌ 错误：在调度外检查
fun cleanup(entity: Entity) {
    if (entity.isValid) {  // 可能跨线程访问
        entity.scheduler.run {
            entity.remove()
        }
    }
}
```

**正确做法**：
```kotlin
// ✅ 在调度内检查
fun cleanup(entity: Entity) {
    try {
        entity.scheduler.run(plugin, { _ ->
            try {
                if (entity.isValid) {  // 在正确的线程检查
                    entity.remove()
                }
            } catch (e: Exception) {}
        }, null)
    } catch (e: Exception) {
        // 调度失败（实体可能已删除）
    }
}
```

### 陷阱 4：假设实体和玩家在同一 Region

**问题场景**：
```kotlin
// ❌ 错误假设
fun updateEntity(player: Player, entity: Entity) {
    // 假设在同一线程，直接访问
    entity.location = player.location.add(0.0, 2.0, 0.0)
}
```

**正确做法**：
```kotlin
// ✅ 分别使用各自的调度器
fun updateEntity(player: Player, entity: Entity) {
    player.scheduler.run(plugin, { _ ->
        val targetLoc = player.location.add(0.0, 2.0, 0.0)
        
        entity.scheduler.run(plugin, { _ ->
            entity.teleport(targetLoc)
        }, null)
    }, null)
}
```

---

## 最佳实践

### 实践 1：实体生命周期管理

```kotlin
class EntityManager(private val plugin: JavaPlugin) {
    private val entities = ConcurrentHashMap<Player, Entity>()
    
    // ✅ 创建：在玩家线程
    fun createEntity(player: Player) {
        player.scheduler.run(plugin, { _ ->
            val entity = player.world.spawn(...)
            entities[player] = entity
            
            // 定时删除：使用实体调度器
            entity.scheduler.runDelayed(plugin, { _ ->
                removeEntity(player, entity)
            }, null, 100L)
        }, null)
    }
    
    // ✅ 删除：使用实体调度器
    private fun removeEntity(player: Player, entity: Entity) {
        try {
            entity.scheduler.run(plugin, { _ ->
                try {
                    if (entity.isValid) {
                        entity.remove()
                    }
                } catch (e: Exception) {}
            }, null)
        } catch (e: Exception) {}
        
        entities.remove(player)
    }
    
    // ✅ 清理：传送时调用
    fun cleanup(player: Player) {
        entities.remove(player)?.let { entity ->
            removeEntity(player, entity)
        }
    }
}
```

### 实践 2：配置缓存模式

```kotlin
class FeatureManager(private val plugin: JavaPlugin) {
    // ✅ 启动时缓存配置，运行时零开销
    private var enabled: Boolean = true
    private var interval: Long = 20L
    
    fun loadConfig() {
        enabled = plugin.config.getBoolean("feature.enabled", true)
        interval = plugin.config.getLong("feature.interval", 20L)
    }
    
    // ✅ 事件处理：直接读缓存，无 I/O
    @EventHandler
    fun onEvent(event: SomeEvent) {
        if (!enabled) return  // 零开销检查
        
        // 处理逻辑...
    }
}
```

### 实践 3：防御性清理

```kotlin
// ✅ 多层防护，确保清理成功
fun cleanupAll() {
    entities.values.forEach { entity ->
        try {
            entity.scheduler.run(plugin, { _ ->
                try {
                    if (entity.isValid) {
                        entity.remove()
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to remove entity: ${e.message}")
                }
            }, null)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to schedule cleanup: ${e.message}")
        }
    }
    entities.clear()
}
```

### 实践 4：传送监听器模式

```kotlin
@EventHandler
fun onPlayerTeleport(event: PlayerTeleportEvent) {
    val player = event.player
    
    // ✅ 传送时清理所有相关实体
    // 避免跨 Region 访问问题
    cleanupPlayerData(player)
}

@EventHandler
fun onPlayerQuit(event: PlayerQuitEvent) {
    val player = event.player
    
    // ✅ 退出时清理
    cleanupPlayerData(player)
}

@EventHandler
fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
    val player = event.player
    
    // ✅ 跨世界时清理（跨世界 = 必定跨 Region）
    cleanupPlayerData(player)
}
```

---

## 调试技巧

### 技巧 1：识别跨线程错误

**典型错误日志**：
```
[ERROR]: Thread failed main thread check: Accessing entity state off owning region's thread
at org.bukkit.craftbukkit.entity.CraftEntity.someMethod()
at YourPlugin.kt:123
```

**错误解读**：
- `Thread failed main thread check` = 跨线程访问
- `off owning region's thread` = 不在实体所属的 Region 线程
- 查看堆栈定位到 `YourPlugin.kt:123`

### 技巧 2：添加调试日志

```kotlin
fun someMethod(player: Player, entity: Entity) {
    plugin.logger.info("[DEBUG] Player region: ${player.location.chunk}")
    plugin.logger.info("[DEBUG] Entity region: ${entity.location.chunk}")
    
    entity.scheduler.run(plugin, { _ ->
        plugin.logger.info("[DEBUG] Executing in entity's thread")
        entity.remove()
    }, null)
}
```

### 技巧 3：使用 try-catch 定位问题

```kotlin
try {
    // 可能出问题的代码
    entity.remove()
} catch (e: Exception) {
    plugin.logger.severe("Error at specific location: ${e.message}")
    e.printStackTrace()
}
```

### 技巧 4：检查调度器类型

```kotlin
// 在关键位置打印调度器信息
plugin.logger.info("Scheduler type: ${player.scheduler.javaClass.simpleName}")
plugin.logger.info("Entity scheduler: ${entity.scheduler.javaClass.simpleName}")
```

---

## 实战案例

### 案例 1：ChatBubble 气泡系统（已解决）

**需求**：玩家聊天时头顶显示气泡，5秒后消失

**问题演进**：
1. **第一次错误**：`player.removePassenger(display)` 跨线程
2. **第二次错误**：`display.remove()` 在玩家调度器中跨线程
3. **最终解决**：使用 `display.scheduler.runDelayed()`

**最终代码**：
```kotlin
fun createBubble(player: Player, message: Component) {
    val display = player.world.spawn(location, TextDisplay::class.java) {
        it.text(message)
    }
    player.addPassenger(display)
    bubbles[player] = display
    
    // ✅ 使用实体调度器，任务跟随实体
    display.scheduler.runDelayed(plugin, { _ ->
        try {
            if (display.isValid) {
                display.remove()
            }
        } catch (e: Exception) {}
        
        // 清理引用使用玩家调度器
        player.scheduler.run(plugin, { _ ->
            bubbles.remove(player)
        }, null)
    }, null, 100L)
}

fun cleanup(player: Player) {
    bubbles.remove(player)?.let { display ->
        try {
            display.scheduler.run(plugin, { _ ->
                try {
                    if (display.isValid) {
                        display.remove()
                    }
                } catch (e: Exception) {}
            }, null)
        } catch (e: Exception) {}
    }
}
```

**关键教训**：
- ✅ 删除跟随实体必须用实体自己的调度器
- ✅ 不要手动操作 passenger 关系
- ✅ 传送时主动清理

### 案例 2：粒子效果系统（假设）

**需求**：玩家周围持续显示粒子效果

**正确实现**：
```kotlin
class ParticleEffect(
    private val plugin: JavaPlugin,
    private val player: Player
) {
    private var taskId: ScheduledTask? = null
    
    fun start() {
        // ✅ 使用玩家调度器（粒子显示不涉及跨 Region 实体）
        taskId = player.scheduler.runAtFixedRate(plugin, { _ ->
            try {
                // 粒子效果在玩家当前位置
                player.world.spawnParticle(
                    Particle.HEART,
                    player.location.add(0.0, 2.0, 0.0),
                    10
                )
            } catch (e: Exception) {
                stop()
            }
        }, null, 1L, 20L)
    }
    
    fun stop() {
        taskId?.cancel()
        taskId = null
    }
}
```

**为什么这里可以用玩家调度器**：
- 粒子效果是瞬时的，不创建持久实体
- 只访问玩家的位置，不访问其他实体
- 任务取消时无需清理实体

---

## 快速检查清单

### 创建实体时

- [ ] 使用正确的调度器创建实体
- [ ] 如果实体跟随玩家，考虑 passenger 机制
- [ ] 设置好实体的生命周期管理

### 定时任务时

- [ ] 使用**实体**调度器操作实体
- [ ] 使用**玩家**调度器操作玩家数据
- [ ] 所有操作都有 try-catch 保护

### 清理实体时

- [ ] 使用实体的调度器删除
- [ ] 不要手动操作 passenger/vehicle 关系
- [ ] 清理所有相关引用

### 传送事件时

- [ ] 监听 PlayerTeleportEvent
- [ ] 清理所有跟随实体
- [ ] 清理所有任务引用

### 插件卸载时

- [ ] 清理所有创建的实体
- [ ] 取消所有定时任务
- [ ] 清空所有 Map/Set 引用

---

## 总结

### 核心原则

1. **使用正确的调度器** - 操作什么用什么的调度器
2. **避免跨线程访问** - 永远不要直接访问可能在其他 Region 的实体
3. **传送即清理** - 传送是跨 Region 的标志
4. **防御性编程** - 多层 try-catch 保护

### 开发流程

```
设计功能
  ↓
识别涉及的实体类型
  ↓
选择正确的调度器
  ↓
添加传送清理逻辑
  ↓
添加异常处理
  ↓
测试传送场景
```

### 测试重点

- ✅ 正常功能流程
- ✅ 玩家传送场景（重点）
- ✅ 玩家退出场景
- ✅ 跨世界传送
- ✅ 快速连续操作

---

## 参考资料

### 相关文档

- `CHATBUBBLE_SCHEDULER_FIX.md` - ChatBubble 修复技术要点
- `SUMMARY_ChatBubble_RemovePassenger_Fix.md` - 完整修复历程
- `开发者指南.md` - 项目整体架构

### Folia 官方资源

- [Folia GitHub](https://github.com/PaperMC/Folia)
- [Folia API 文档](https://jd.papermc.io/folia/1.20/)

---

**文档版本**: v1.0  
**最后更新**: 2025-11-29  
**基于项目**: TSLplugins v1.0  
**目标版本**: Folia 1.21.8

---

## 附录：常用代码片段

### 安全删除实体

```kotlin
fun safeRemoveEntity(entity: Entity) {
    try {
        entity.scheduler.run(plugin, { _ ->
            try {
                if (entity.isValid) {
                    entity.remove()
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to remove entity: ${e.message}")
            }
        }, null)
    } catch (e: Exception) {
        plugin.logger.warning("Failed to schedule removal: ${e.message}")
    }
}
```

### 玩家数据清理监听器

```kotlin
class PlayerCleanupListener(
    private val plugin: JavaPlugin,
    private val manager: YourManager
) : Listener {
    
    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        manager.cleanup(event.player)
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        manager.cleanup(event.player)
    }
    
    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        manager.cleanup(event.player)
    }
}
```

### 配置缓存模板

```kotlin
class FeatureManager(private val plugin: JavaPlugin) {
    // 配置缓存
    private var enabled: Boolean = true
    private var param1: String = ""
    private var param2: Int = 0
    
    init {
        loadConfig()
    }
    
    fun loadConfig() {
        val config = plugin.config
        enabled = config.getBoolean("feature.enabled", true)
        param1 = config.getString("feature.param1", "") ?: ""
        param2 = config.getInt("feature.param2", 0)
        
        plugin.logger.info("[Feature] Config loaded - enabled: $enabled")
    }
    
    fun isEnabled(): Boolean = enabled
}
```

---

**祝你在 Folia 开发中一帆风顺！** 🎉

