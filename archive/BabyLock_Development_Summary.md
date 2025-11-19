# 🍼 BabyLock 永久幼年生物功能开发总结

**开发日期**: 2025-11-20  
**功能状态**: ✅ 开发完成  
**配置版本**: 8

---

## 📋 功能概述

### 核心目标
实现一个简单直观的机制：**幼年生物 + 指定前缀命名 = 永久幼年（锁定不长大）**

### 使用方式
1. 用命名牌给幼年生物命名，名字以指定前缀开头（如 `[幼]小牛`）
2. 插件自动锁定该生物为永久幼年
3. 移除前缀或重命名 → 自动解锁，允许继续成长

### 特性
- ✅ 自动检测和锁定（无需额外命令）
- ✅ 多前缀支持（`[幼]`, `[小]`, `[Baby]` 等）
- ✅ 实体类型白名单（可限制特定生物）
- ✅ 防止消失（锁定生物永久存在）
- ✅ 不区分大小写（可配置）
- ✅ 插件重启后保持锁定（依赖原版 API）
- ✅ Folia 完全兼容

---

## 🔧 技术实现

### 核心类结构

```
BabyLock/
├── BabyLockManager.kt     # 配置管理、前缀检查、锁定逻辑
└── BabyLockListener.kt    # 事件监听、自动锁定/解锁
```

### 关键技术点

#### 1. 使用原版 API
```kotlin
// 使用 Bukkit Ageable 接口的 ageLock 属性
@Suppress("DEPRECATION")  // ageLock 在新版本标记为弃用但仍可用
entity.ageLock = true  // 锁定年龄
entity.isPersistent = true  // 防止消失
```

**优势**:
- 服务器原生支持，无需额外存储
- 插件重启后状态自动保持
- 兼容性好，不依赖 NBT 或其他复杂机制

#### 2. 事件监听策略
```kotlin
// 监听玩家用命名牌命名
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
    if (event.hand != EquipmentSlot.HAND) return
    
    // 延迟检查，等待名字更新
    entity.scheduler.run(plugin, { _ ->
        checkAndUpdateLock(entity, player)
    }, null)
}

// 监听生物繁殖（新生幼年生物）
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
fun onEntityBreedEvent(event: EntityBreedEvent) {
    // 延迟检查，等待可能的命名
    entity.scheduler.runDelayed(plugin, { _ ->
        checkAndUpdateLock(entity, breeder)
    }, null, 10L)  // 延迟 0.5 秒
}
```

**为什么要延迟**:
- 命名牌命名是异步的，名字不会立即更新
- 延迟确保获取到最新的名字
- 使用实体调度器确保 Folia 兼容性

#### 3. 前缀匹配逻辑
```kotlin
fun hasLockPrefix(name: String): Boolean {
    if (name.isEmpty()) return false
    
    return prefixes.any { prefix ->
        if (caseSensitive) {
            name.startsWith(prefix)
        } else {
            name.startsWith(prefix, ignoreCase = true)
        }
    }
}
```

**灵活性**:
- 支持多个前缀（配置列表）
- 可选大小写敏感
- 使用 `startsWith` 而非完全匹配

#### 4. 名字提取
```kotlin
// 从 Component 提取纯文本名字
val customName = entity.customName()
if (customName == null) {
    // 没有名字，确保解锁
    if (entity.ageLock) entity.ageLock = false
    return
}

val plainName = PlainTextComponentSerializer.plainText()
    .serialize(customName)
```

**处理**:
- Adventure API 的 Component 转纯文本
- 移除颜色代码和格式化
- 只检查实际文字内容

#### 5. 实体类型白名单
```kotlin
fun isTypeEnabled(entityType: EntityType): Boolean {
    // 空列表 = 全部 Ageable 生物启用
    if (enabledTypes.isEmpty()) return true
    return enabledTypes.contains(entityType)
}
```

**配置**:
```yaml
enabled_types: []  # 空 = 全部启用

# 或限制特定类型
enabled_types:
  - COW
  - SHEEP
  - CHICKEN
  - ALLAY
```

---

## 📊 工作流程

### 命名触发锁定
```
1. 玩家用命名牌右键幼年牛
2. PlayerInteractEntityEvent 触发
3. 延迟 1 tick，等待名字更新
4. 检查：是 Ageable？是幼年？名字有前缀？类型启用？
5. 满足条件 → entity.ageLock = true
6. 发送消息："这只 小牛 现在会永远保持幼年啦！"
```

### 重命名触发解锁
```
1. 玩家重命名生物，移除前缀
2. PlayerInteractEntityEvent 触发
3. 延迟检查
4. 名字不匹配前缀 → entity.ageLock = false
5. 发送消息："已解除 小牛 的幼年锁定，它可以继续成长啦。"
```

### 繁殖新生
```
1. 两只动物繁殖
2. EntityBreedEvent 触发
3. 延迟 0.5 秒（可能被立即命名）
4. 如果有前缀名字 → 自动锁定
5. 否则保持正常
```

---

## ⚙️ 配置说明

### config.yml 配置

```yaml
babylock:
  # 是否启用功能
  enabled: true
  
  # 名字前缀列表（满足任一前缀即触发锁定）
  prefixes:
    - "[幼]"
    - "[小]"
    - "[Baby]"
  
  # 是否区分大小写
  case_sensitive: false
  
  # 防止锁定的生物消失
  prevent_despawn: true
  
  # 启用的实体类型（白名单）
  # 留空表示全部 Ageable 生物均启用
  # 可选值: ALLAY, CHICKEN, COW, SHEEP, PIG, RABBIT, WOLF, CAT, 
  #         HORSE, DONKEY, MULE, LLAMA, TRADER_LLAMA, FOX, BEE, 
  #         GOAT, AXOLOTL, CAMEL, SNIFFER 等
  enabled_types: []
  
  # 消息配置
  messages:
    prefix: "&6[TSL喵]&r "
    # 锁定成功
    lock: "%prefix%&d这只 &e{entity} &d现在会永远保持幼年啦！"
    # 解除成功
    unlock: "%prefix%&e已解除 &7{entity} &e的幼年锁定，它可以继续成长啦。"
```

### 配置示例

#### 示例 1: 默认配置（全部生物）
```yaml
babylock:
  enabled: true
  prefixes: ["[幼]", "[小]", "[Baby]"]
  case_sensitive: false
  prevent_despawn: true
  enabled_types: []  # 全部 Ageable 生物
```

#### 示例 2: 只限制特定生物
```yaml
babylock:
  enabled: true
  prefixes: ["[幼]"]
  case_sensitive: false
  prevent_despawn: true
  enabled_types:
    - ALLAY      # 只有善魂
    - COW        # 牛
    - SHEEP      # 羊
    - CHICKEN    # 鸡
```

#### 示例 3: 区分大小写
```yaml
babylock:
  enabled: true
  prefixes: ["[Baby]", "[BABY]"]  # 需要精确匹配
  case_sensitive: true
  prevent_despawn: true
  enabled_types: []
```

---

## 🎮 使用示例

### 场景 1: 永久幼年善魂
```
1. 找到一只善魂（Allay）
2. 用命名牌命名为 "[幼]小善魂"
3. ✅ 锁定成功！善魂永远保持小只
```

### 场景 2: 迷你农场
```
1. 繁殖一些幼年动物
2. 命名为 "[小]小牛"、"[小]小羊"、"[小]小鸡"
3. ✅ 它们永远不会长大
4. 配合 prevent_despawn，永久存在
```

### 场景 3: 解除锁定
```
1. 有一只名为 "[幼]小马" 的锁定马
2. 重命名为 "大马"（移除前缀）
3. ✅ 自动解锁，可以继续成长
```

### 场景 4: 多前缀支持
```
配置: prefixes: ["[幼]", "[小]", "[Baby]"]

有效名字:
- "[幼]小牛" ✅
- "[小]迷你羊" ✅
- "[Baby]Chicken" ✅
- "普通猪" ❌ 不锁定
```

---

## 🐛 注意事项

### 1. ageLock API 已弃用
```kotlin
@Suppress("DEPRECATION")
entity.ageLock = true
```
- Bukkit 标记为弃用，但仍完全可用
- 没有替代 API
- 使用 @Suppress 抑制警告

### 2. 只对 Ageable 生物有效
```kotlin
if (entity !is Ageable) return
```
- 僵尸、骷髅等非 Ageable 生物不受影响
- 自动过滤，不会报错

### 3. 必须是幼年才能锁定
```kotlin
if (entity.isAdult) return false
```
- 成年生物即使命名也不会锁定
- 避免意外锁定成年生物

### 4. 延迟检查的重要性
```kotlin
// ❌ 错误：立即检查，名字还未更新
fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
    checkAndUpdateLock(event.rightClicked, event.player)
}

// ✅ 正确：延迟检查，确保名字已更新
fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
    entity.scheduler.run(plugin, { _ ->
        checkAndUpdateLock(entity, player)
    }, null)
}
```

---

## 🧪 测试要点

### 基本功能测试
- [ ] 给幼年牛命名 "[幼]小牛" → 锁定成功
- [ ] 重命名为 "普通牛" → 解锁成功
- [ ] 给成年牛命名 "[幼]大牛" → 不锁定（必须是幼年）
- [ ] 给僵尸命名 "[幼]僵尸" → 无效果（不是 Ageable）

### 前缀测试
- [ ] "[幼]测试" → 锁定
- [ ] "[小]测试" → 锁定
- [ ] "[Baby]Test" → 锁定
- [ ] "[其他]测试" → 不锁定（不在前缀列表）

### 大小写测试
```yaml
case_sensitive: false
```
- [ ] "[幼]测试" → 锁定
- [ ] "[幼]测试" → 锁定（小写）
- [ ] "[Youyou]测试" → 不锁定（不同前缀）

```yaml
case_sensitive: true
```
- [ ] "[幼]测试" → 锁定
- [ ] "[幼]测试" → 不锁定（区分大小写）

### 白名单测试
```yaml
enabled_types: [COW, SHEEP]
```
- [ ] 牛 → 可以锁定
- [ ] 羊 → 可以锁定
- [ ] 鸡 → 不能锁定（不在白名单）

### 消失测试
```yaml
prevent_despawn: true
```
- [ ] 锁定的生物远离玩家 → 不消失
- [ ] 普通生物远离玩家 → 正常消失

### 重启测试
- [ ] 锁定生物 → 重启服务器 → 仍然锁定
- [ ] 解锁生物 → 重启服务器 → 仍然解锁

---

## 📈 性能考虑

### 优化点
1. **配置缓存** - 启动/reload时读取，事件处理直接访问缓存
2. **快速失败** - 先检查简单条件（类型、年龄）
3. **延迟执行** - 避免阻塞主事件处理
4. **MONITOR优先级** - 在其他插件处理后检查

### 性能影响
- ✅ **极小** - 只在玩家命名时触发
- ✅ **无轮询** - 纯事件驱动
- ✅ **无存储** - 依赖原版 API
- ✅ **Folia友好** - 使用实体调度器

---

## 🔄 集成情况

### 主插件集成
```kotlin
// TSLplugins.kt
private lateinit var babyLockManager: BabyLockManager

override fun onEnable() {
    // ...
    babyLockManager = BabyLockManager(this)
    val babyLockListener = BabyLockListener(this, babyLockManager)
    pm.registerEvents(babyLockListener, this)
    // ...
}

fun reloadBabyLockManager() {
    babyLockManager.loadConfig()
}
```

### 重载命令集成
```kotlin
// ReloadCommand.kt
override fun handle(...) {
    // ...
    plugin.reloadBabyLockManager()
    // ...
}
```

### 配置版本
- **版本 7** → **版本 8**
- 添加 `babylock` 配置节

---

## 📝 开发经验

### 设计亮点
1. **极简操作** - 只需命名，无需额外命令
2. **直观反馈** - 锁定/解锁都有提示消息
3. **灵活配置** - 多前缀、白名单、大小写控制
4. **零学习成本** - 玩家一看就懂

### 技术亮点
1. **依赖原版API** - 无需额外存储，天然持久化
2. **事件驱动** - 无轮询，性能优秀
3. **Folia兼容** - 完全符合异步架构
4. **类型安全** - Kotlin 类型系统避免错误

### 可能的扩展
- [ ] 添加命令手动锁定/解锁
- [ ] 添加 GUI 管理界面
- [ ] 统计锁定生物数量
- [ ] 支持自定义锁定效果（粒子、音效）

---

## 📚 相关文档

- **需求文档**: `新功能.md`
- **开发笔记**: `DEV_NOTES.md` - 第 12 节
- **配置文件**: `config.yml` - `babylock` 节
- **源代码**: `src/main/kotlin/org/tsl/tSLplugins/BabyLock/`

---

## ✅ 完成清单

- [x] BabyLockManager.kt - 配置管理和逻辑
- [x] BabyLockListener.kt - 事件监听
- [x] 集成到 TSLplugins.kt
- [x] 添加配置到 config.yml
- [x] 更新配置版本（7→8）
- [x] 集成重载命令
- [x] 更新 DEV_NOTES.md
- [x] 创建总结文档

---

**开发完成**: 2025-11-20  
**代码状态**: ✅ 编译通过（仅警告）  
**功能状态**: ✅ 可以部署测试  
**文档完整度**: 100%

