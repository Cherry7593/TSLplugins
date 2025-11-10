# 权限检查机制优化说明

## 1. 维护模式权限检查优化

### 问题背景
之前没有实现维护模式的动态权限检查。

### 优化方案
实现**事件驱动**的权限检查机制：

#### 登录前拦截
使用 `AsyncPlayerPreLoginEvent` 在玩家登录前就拦截，避免区块加载和实体注册：
```kotlin
@EventHandler(priority = EventPriority.LOWEST)
fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (!manager.isMaintenanceEnabled()) return
    if (manager.isWhitelisted(event.uniqueId)) return
    
    event.disallow(
        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
        kickMessage
    )
}
```

#### 主动触发检查
在以下关键时机主动触发权限检查：

**a. 开启维护模式时**
```kotlin
fun handleOn(sender: CommandSender) {
    manager.setMaintenanceEnabled(true)
    // 立即检查所有在线玩家
    permissionListener.checkOnlinePlayers()
}
```

**b. 从白名单移除玩家时**
```kotlin
fun handleRemove(sender: CommandSender, args: Array<out String>) {
    manager.removeFromWhitelist(uuid)
    if (manager.isMaintenanceEnabled()) {
        // 立即检查该玩家是否在线
        permissionListener.checkOnlinePlayers()
    }
}
```

**c. 玩家切换世界时**
某些权限插件的权限配置是基于世界的，因此需要监听世界切换：
```kotlin
@EventHandler
fun onWorldChange(event: PlayerChangedWorldEvent) {
    if (!manager.isMaintenanceEnabled()) return
    if (!shouldAllowPlayer(player)) {
        player.kick(kickMessage)
    }
}
```

### 权限检查逻辑
```kotlin
private fun shouldAllowPlayer(uuid: UUID, hasBypassPermission: Boolean): Boolean {
    return manager.isWhitelisted(uuid) || hasBypassPermission
}
```

玩家可以通过两种方式保持在服务器：
- 在维护模式白名单中（通过命令添加）
- 拥有 `tsl.maintenance.bypass` 权限

---

## 2. 游客模式权限检查优化

### 问题背景
之前的实现有两个检查机制：
1. **LuckPerms 权限变更事件**（主要机制）
2. **每30秒轮询检查**（保险机制）

每30秒的轮询检查是不必要的性能开销。

### 优化方案
**移除轮询机制**，完全依赖 LuckPerms 的权限变更事件：

#### LuckPerms 权限变更事件监听
```kotlin
private fun setupLuckPerms() {
    luckPerms?.eventBus?.subscribe(
        plugin, 
        UserDataRecalculateEvent::class.java, 
        ::onPermissionChange
    )
}

private fun onPermissionChange(event: UserDataRecalculateEvent) {
    // 当玩家权限变更时自动触发
    val hasPermission = player.hasPermission("tsl.visitor")
    val wasVisitor = visitorPlayers.contains(uuid)
    
    when {
        hasPermission && !wasVisitor -> applyVisitorEffect(player)
        !hasPermission && wasVisitor -> removeVisitorEffect(player)
    }
}
```

#### 玩家登录时检查
```kotlin
@EventHandler
fun onPlayerJoin(event: PlayerJoinEvent) {
    // 延迟1秒确保权限加载完成
    player.scheduler.runDelayed(plugin, { _ ->
        checkAndApplyVisitorStatus(player)
    }, null, 20L)
}
```

### 工作流程
1. **玩家登录** → 检查权限，应用效果（如果有权限）
2. **权限变更**（通过命令修改）→ LuckPerms 事件触发 → 立即更新效果
3. **玩家退出** → 从缓存中移除

---

## 性能对比

### 维护模式
- ✅ **现在**：事件驱动，只在必要时检查（零轮询开销）

### 游客模式
- ❌ **之前**：LuckPerms 事件 + 每30秒轮询所有在线玩家
- ✅ **现在**：仅 LuckPerms 事件驱动（移除轮询，零额外开销）

---

## 响应速度

### 维护模式
- ✅ 开启维护模式 → 立即踢出无权限玩家
- ✅ 从白名单移除 → 立即生效
- ✅ 玩家登录 → 登录前就拦截

### 游客模式
- ✅ 权限变更（命令修改） → 实时响应（通过 LuckPerms 事件）
- ✅ 玩家登录 → 1秒后检查并应用

---

## 优势总结
1. ✅ **零性能开销** - 不再有定期轮询
2. ✅ **即时响应** - 权限变更立即生效
3. ✅ **架构优雅** - 事件驱动，符合最佳实践
4. ✅ **更加可靠** - LuckPerms 官方事件保证准确性

---

## 相关文件

### 维护模式
- `MaintenancePermissionListener.kt` - 权限变更监听器
- `MaintenanceLoginListener.kt` - 登录前拦截
- `MaintenanceCommand.kt` - 命令处理中的主动检查
- `MaintenanceManager.kt` - 管理维护模式状态和白名单

### 游客模式
- `VisitorEffect.kt` - 游客模式效果管理（已优化，移除轮询）

