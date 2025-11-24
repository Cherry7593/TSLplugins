# Freeze 命令优化总结

**日期**: 2025-11-24  
**任务来源**: 需求.md

---

## 优化内容

### ✅ Freeze 命令自动切换冻结/解冻

**需求**：
- 简化 freeze 命令的使用
- 一个命令完成冻结和解冻操作
- 自动判断玩家当前状态

**优化前**：
```bash
# 需要两个命令
/tsl freeze Steve       # 冻结玩家
/tsl unfreeze Steve     # 解冻玩家
```

**优化后**：
```bash
# 只需一个命令，自动切换
/tsl freeze Steve       # 如果未冻结→冻结；如果已冻结→解冻
/tsl freeze Steve 300   # 冻结 5 分钟（只在未冻结时有效）
```

---

## 实现逻辑

### 自动切换机制

```kotlin
private fun handleToggleFreeze(sender: CommandSender, targetName: String, duration: Int) {
    val target = Bukkit.getPlayer(targetName)
    // ...验证和权限检查...
    
    // 检查当前是否已被冻结
    if (manager.isFrozen(target.uniqueId)) {
        // 已冻结 → 执行解冻
        manager.unfreezePlayer(target.uniqueId)
        sender.sendMessage("已解冻 ${target.name}")
        target.sendMessage("你已被解冻！")
    } else {
        // 未冻结 → 执行冻结
        manager.freezePlayer(target.uniqueId, duration)
        val durationText = if (duration > 0) formatTime(duration) else "永久"
        sender.sendMessage("已冻结 ${target.name} 持续时间: $durationText")
        target.sendMessage("你已被冻结！持续时间: $durationText")
    }
}
```

---

## 行为说明

### 冻结玩家
```bash
# Steve 当前未被冻结
/tsl freeze Steve
# 结果: Steve 被永久冻结

/tsl freeze Steve 300
# 结果: Steve 被冻结 5 分钟
```

### 解冻玩家
```bash
# Steve 当前已被冻结
/tsl freeze Steve
# 结果: Steve 被解冻（忽略时间参数）

/tsl freeze Steve 300
# 结果: Steve 被解冻（时间参数无效，因为是解冻操作）
```

### 列出冻结玩家
```bash
/tsl freeze list
# 显示所有被冻结的玩家
```

---

## 修改文件

### 1. FreezeCommand.kt
- **重写整个文件**，移除 `handleFreeze` 和 `handleUnfreeze` 方法
- **新增** `handleToggleFreeze` 方法，实现自动切换
- **简化** Tab 补全，移除 "unfreeze" 建议

**核心改动**：
```kotlin
// 移除的逻辑
when {
    args[0].equals("unfreeze", ignoreCase = true) -> handleUnfreeze(...)
    else -> handleFreeze(...)
}

// 新的逻辑
when {
    args[0].equals("list", ignoreCase = true) -> handleList(sender)
    else -> handleToggleFreeze(sender, args[0], duration)
}
```

### 2. TSLplugins.kt
- **移除** `unfreeze` 子命令注册
- 只保留 `freeze` 子命令

**修改前**：
```kotlin
dispatcher.registerSubCommand("freeze", FreezeCommand(freezeManager))
dispatcher.registerSubCommand("unfreeze", FreezeCommand(freezeManager))  // ❌ 删除
```

**修改后**：
```kotlin
dispatcher.registerSubCommand("freeze", FreezeCommand(freezeManager))
// unfreeze 命令已移除，freeze 自动切换
```

### 3. config.yml
- **更新** usage 消息，说明自动切换功能

**修改前**：
```yaml
usage: |-
  %prefix%&e使用方法:
  &7/tsl freeze <玩家> [时间] &f- 冻结玩家
  &7/tsl freeze unfreeze <玩家> &f- 解冻玩家
  &7/tsl freeze list &f- 列出被冻结的玩家
```

**修改后**：
```yaml
usage: |-
  %prefix%&e使用方法:
  &7/tsl freeze <玩家> [时间] &f- 切换冻结状态（自动冻结/解冻）
  &7/tsl freeze list &f- 列出被冻结的玩家
  &8提示: 如果玩家已冻结则解冻，未冻结则冻结
```

---

## Tab 补全优化

### 优化前
```
/tsl freeze <TAB>
显示: list, unfreeze, 玩家名...
```

### 优化后
```
/tsl freeze <TAB>
显示: list, 玩家名...  # 移除了 unfreeze 建议
```

---

## 使用示例

### 示例 1：冻结玩家
```bash
# 初始状态：Steve 未被冻结
> /tsl freeze Steve
[冻结] 已冻结 Steve 持续时间: 永久

# Steve 收到消息
[冻结] 你已被冻结！持续时间: 永久
```

### 示例 2：解冻玩家
```bash
# 初始状态：Steve 已被冻结
> /tsl freeze Steve
[冻结] 已解冻 Steve

# Steve 收到消息
[冻结] 你已被解冻！
```

### 示例 3：定时冻结
```bash
# 冻结玩家 5 分钟
> /tsl freeze Steve 300
[冻结] 已冻结 Steve 持续时间: 5分0秒

# 再次执行（解冻）
> /tsl freeze Steve
[冻结] 已解冻 Steve  # 时间参数被忽略
```

### 示例 4：查看列表
```bash
> /tsl freeze list
[冻结] 当前被冻结的玩家:
- Steve [在线] 剩余: 4分30秒
- Alex [离线] 剩余: 永久
```

---

## 优势

### 1. ✅ 操作更简单
- 只需记住一个命令
- 不需要判断应该用 freeze 还是 unfreeze

### 2. ✅ 更直观
- 同一个命令完成相反操作
- 符合"切换"的直觉

### 3. ✅ 减少错误
- 不会出现"对已冻结的玩家执行 freeze"的困惑
- 不会出现"对未冻结的玩家执行 unfreeze"的错误

### 4. ✅ 代码更简洁
- 移除了重复的 unfreeze 子命令
- 统一的处理逻辑

---

## 兼容性

### 旧命令支持
- ❌ `/tsl unfreeze <玩家>` - 不再支持（命令已移除）
- ✅ `/tsl freeze <玩家>` - 自动切换，兼容新旧用法

### 升级说明
如果服务器有使用 `/tsl unfreeze` 的脚本或命令方块：
1. 替换为 `/tsl freeze <玩家>`
2. 新命令会自动判断并解冻

---

## 测试场景

### 测试 1：基本切换
```bash
1. /tsl freeze Steve
   验证: Steve 被冻结 ✅
   
2. /tsl freeze Steve
   验证: Steve 被解冻 ✅
   
3. /tsl freeze Steve
   验证: Steve 再次被冻结 ✅
```

### 测试 2：定时冻结
```bash
1. /tsl freeze Steve 60
   验证: Steve 被冻结 60 秒 ✅
   
2. 等待 30 秒
   
3. /tsl freeze Steve
   验证: Steve 被解冻（剩余时间被取消）✅
```

### 测试 3：列表查看
```bash
1. /tsl freeze Steve
2. /tsl freeze Alex 300
3. /tsl freeze list
   验证: 显示 Steve (永久) 和 Alex (剩余时间) ✅
```

### 测试 4：Bypass 权限
```bash
1. 给 Steve 添加 tsl.freeze.bypass 权限
2. /tsl freeze Steve
   验证: 提示 "Steve 拥有冻结豁免权限" ✅
```

---

## 相关文件

### 修改的文件
- `Freeze/FreezeCommand.kt` - 完全重写，实现自动切换
- `TSLplugins.kt` - 移除 unfreeze 子命令注册
- `config.yml` - 更新使用说明

---

## 总结

Freeze 命令已优化为自动切换模式：

1. ✅ **一个命令完成冻结和解冻**
2. ✅ **自动判断当前状态**
3. ✅ **操作更简单直观**
4. ✅ **代码更简洁**

现在使用 `/tsl freeze <玩家>` 即可智能切换冻结/解冻状态！

---

**完成日期**: 2025-11-24  
**插件版本**: 1.0  
**配置版本**: 10

