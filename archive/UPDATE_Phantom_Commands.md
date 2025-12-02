# Phantom 模块命令优化总结

**优化日期**: 2025-12-02  
**变更**: 从简单的切换命令改为明确的开/关/状态查询命令

---

## 🔄 命令变更

### 旧命令 ❌
```bash
/tsl phantom    # 切换开关（不明确当前状态）
```

**问题**:
- 不清楚当前是开还是关
- 切换后需要额外确认状态
- 用户体验不够友好

---

### 新命令 ✅
```bash
/tsl phantom on       # 明确开启
/tsl phantom off      # 明确关闭
/tsl phantom status   # 查看当前状态
```

**优势**:
- ✅ 命令意图明确
- ✅ 可以直接查看状态
- ✅ 防止重复设置（已开启时提示）
- ✅ 更好的用户体验

---

## 📝 修改的文件（2个）

### 1. PhantomCommand.kt

**重写命令处理逻辑**:

```kotlin
// 旧代码 ❌ - 简单切换
override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
    val currentState = manager.isPhantomAllowed(sender)
    val newState = !currentState
    manager.setPhantomAllowed(sender, newState)
    // ...
}

// 新代码 ✅ - 子命令分发
override fun handle(sender: CommandSender, args: Array<out String>): Boolean {
    if (args.isEmpty()) {
        showHelp(sender)
        return true
    }
    
    when (args[0].lowercase()) {
        "on" -> handleOn(sender)
        "off" -> handleOff(sender)
        "status" -> handleStatus(sender)
        else -> showHelp(sender)
    }
}
```

**新增方法**:

1. **handleOn()** - 处理开启命令
   - 检查当前状态
   - 如果已开启，提示"已经是开启状态"
   - 如果关闭，开启并显示成功消息

2. **handleOff()** - 处理关闭命令
   - 检查当前状态
   - 如果已关闭，提示"已经是关闭状态"
   - 如果开启，关闭并显示成功消息

3. **handleStatus()** - 显示状态
   - 显示当前开关状态（允许/禁止）
   - 显示对应的效果说明

4. **showHelp()** - 显示帮助
   - 列出所有可用的子命令
   - 显示命令说明

**Tab 补全**:
```kotlin
override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
    return when (args.size) {
        1 -> listOf("on", "off", "status")
            .filter { it.startsWith(args[0], ignoreCase = true) }
        else -> emptyList()
    }
}
```

---

### 2. plugin.yml

**更新命令说明**:
```yaml
# 旧的
/tsl phantom

# 新的
/tsl phantom on
/tsl phantom off
/tsl phantom status
```

---

## 🎮 使用示例

### 开启幻翼骚扰
```bash
玩家: /tsl phantom on
系统: ✓ 已允许幻翼骚扰
      长时间不睡觉会出现幻翼

# 如果已经开启
玩家: /tsl phantom on
系统: 幻翼骚扰已经是开启状态！
```

### 关闭幻翼骚扰
```bash
玩家: /tsl phantom off
系统: ✓ 已禁止幻翼骚扰
      幻翼将不会出现

# 如果已经关闭
玩家: /tsl phantom off
系统: 幻翼骚扰已经是关闭状态！
```

### 查看当前状态
```bash
玩家: /tsl phantom status
系统: ========== 幻翼控制状态 ==========
      当前状态: 禁止
      幻翼不会出现
```

### 查看帮助
```bash
玩家: /tsl phantom
系统: ========== 幻翼控制命令 ==========
      /tsl phantom on - 允许幻翼骚扰
      /tsl phantom off - 禁止幻翼骚扰
      /tsl phantom status - 查看当前状态
```

---

## ✅ 优化效果

### 用户体验改进
- ✅ **命令更明确** - 知道自己在做什么
- ✅ **状态可查询** - 不用猜测当前状态
- ✅ **防止误操作** - 重复设置时会提示
- ✅ **帮助更完善** - 无参数时显示帮助

### 技术改进
- ✅ **子命令架构** - 易于扩展
- ✅ **Tab 补全** - 提供 on/off/status 补全
- ✅ **代码结构** - 更清晰的方法划分

---

## 📊 代码对比

### 代码行数
| 版本 | 行数 | 变化 |
|------|------|------|
| 旧版本 | ~70 | - |
| 新版本 | ~170 | +100 |

**新增功能**:
- handleOn() - ~15 行
- handleOff() - ~15 行
- handleStatus() - ~20 行
- showHelp() - ~20 行
- Tab 补全优化 - ~10 行

---

## 🧪 测试场景

### 场景 1：首次使用
```
1. 玩家不知道当前状态
2. 输入 /tsl phantom status
3. 看到当前状态：禁止
4. 输入 /tsl phantom on 开启
```

### 场景 2：防止重复操作
```
1. 玩家输入 /tsl phantom on
2. 系统提示：已允许幻翼骚扰
3. 玩家再次输入 /tsl phantom on
4. 系统提示：幻翼骚扰已经是开启状态！
```

### 场景 3：查看帮助
```
1. 玩家输入 /tsl phantom
2. 系统显示帮助信息
3. 玩家了解所有可用命令
```

### 场景 4：Tab 补全
```
1. 玩家输入 /tsl phantom [Tab]
2. 显示: on, off, status
3. 玩家输入 /tsl phantom o[Tab]
4. 自动补全: on
```

---

## 💡 设计亮点

### 1. 状态检查
```kotlin
private fun handleOn(sender: Player) {
    val currentState = manager.isPhantomAllowed(sender)
    
    if (currentState) {
        sender.sendMessage("幻翼骚扰已经是开启状态！")
        return
    }
    
    // 执行开启操作
}
```

### 2. 友好的状态显示
```kotlin
private fun handleStatus(sender: Player) {
    val currentState = manager.isPhantomAllowed(sender)
    
    if (currentState) {
        sender.sendMessage("当前状态: 允许")
        sender.sendMessage("  长时间不睡觉会出现幻翼")
    } else {
        sender.sendMessage("当前状态: 禁止")
        sender.sendMessage("  幻翼不会出现")
    }
}
```

### 3. 完善的帮助
```kotlin
private fun showHelp(sender: CommandSender) {
    sender.sendMessage("========== 幻翼控制命令 ==========")
    sender.sendMessage("/tsl phantom on - 允许幻翼骚扰")
    sender.sendMessage("/tsl phantom off - 禁止幻翼骚扰")
    sender.sendMessage("/tsl phantom status - 查看当前状态")
}
```

---

## 🔗 相关文件

```
Modified:
└── Phantom/
    └── PhantomCommand.kt             # 重写命令处理
└── plugin.yml                        # 更新命令说明

Updated:
└── archive/
    └── SUMMARY_Phantom_Module.md    # 更新文档

archive/
└── UPDATE_Phantom_Commands.md       # 本文档
```

---

**优化完成时间**: 2025-12-02  
**优化状态**: ✅ 完成  
**编译状态**: ✅ 通过（无错误）

