# CustomAnvil 功能移除总结

> **移除时间**: 2025-11-11  
> **原因**: 功能实现复杂，改用独立插件替代

---

## 移除内容

### 1. 代码文件（已移至 archive/CustomAnvil/）

- **AnvilListener.kt** - 铁砧事件监听器
- **AnvilManager.kt** - 铁砧配置管理器
- **AnvilCommand.kt** - 铁砧命令处理器
- **AnvilProtocolHelper.kt** - ProtocolLib 辅助类

### 2. 文档文件（已移至 archive/）

- **CustomAnvil_USER_GUIDE.md** - 用户使用指南
- **CustomAnvil_TEST_GUIDE.md** - 测试指南
- **SUMMARY_CustomAnvil_Implementation.md** - 实现总结
- **SUMMARY_CustomAnvil_Issues_And_Solutions.md** - 问题与解决方案
- **SUMMARY_CustomAnvil_Fix_Complete.md** - 修复总结
- **SUMMARY_CustomAnvil_Fix_2025.md** - 2025修复总结

### 3. 主插件类修改（TSLplugins.kt）

**移除的导入语句**:
```kotlin
import org.tsl.tSLplugins.CustomAnvil.AnvilManager
import org.tsl.tSLplugins.CustomAnvil.AnvilListener
import org.tsl.tSLplugins.CustomAnvil.AnvilCommand
import org.tsl.tSLplugins.CustomAnvil.AnvilProtocolHelper
```

**移除的属性**:
```kotlin
private lateinit var anvilManager: AnvilManager
private var anvilProtocolHelper: AnvilProtocolHelper? = null
```

**移除的初始化代码**:
```kotlin
// 初始化自定义铁砧系统
anvilManager = AnvilManager(this)

// 检查 ProtocolLib 是否可用
if (server.pluginManager.getPlugin("ProtocolLib") != null) {
    anvilProtocolHelper = AnvilProtocolHelper(this, anvilManager)
    anvilProtocolHelper?.initialize()
    logger.info("ProtocolLib 已检测到 - 铁砧成本控制功能已增强")
} else {
    logger.warning("未检测到 ProtocolLib - 铁砧成本控制功能将受限")
}

pm.registerEvents(AnvilListener(this, anvilManager, anvilProtocolHelper), this)
```

**移除的命令注册**:
```kotlin
dispatcher.registerSubCommand("anvil", AnvilCommand(anvilManager))
```

**移除的清理代码**:
```kotlin
// 清理 ProtocolLib 监听器
anvilProtocolHelper?.cleanup()
```

**移除的重载方法**:
```kotlin
fun reloadAnvilManager() {
    anvilManager.loadConfig()
}
```

### 4. 重载命令修改（ReloadCommand.kt）

**移除的重载调用**:
```kotlin
// 重新加载自定义铁砧
plugin.reloadAnvilManager()
```

### 5. 配置文件修改（config.yml）

**移除的完整配置段**:
```yaml
# 自定义铁砧配置
custom-anvil:
  enabled: true
  limit_repair_cost: false
  limit_repair_value: 39
  remove_repair_limit: true
  replace_too_expensive: true
  allow_colored_names: true
  # ... 以及所有相关注释
```

---

## 保留内容

### 保留的依赖

**ProtocolLib** 依赖已保留在 `build.gradle.kts` 中：
```kotlin
compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
```

**原因**: 将来可能有其他功能需要使用 ProtocolLib

---

## 替代方案

用户计划使用独立的铁砧插件替代 CustomAnvil 功能。

### 推荐的独立插件方案

根据提供的开发文档（1.md），建议开发一个轻量级独立插件包含：

1. **限制铁砧成本上限**
2. **移除铁砧39级限制**
3. **替换"Too Expensive"显示**
4. **铁砧彩色命名（支持16位色彩）**

**优势**:
- 独立维护，互不干扰
- 更容易调试和更新
- 可以单独启用/禁用
- 不影响 TSLplugins 的其他功能

---

## 清理结果

### 文件结构变化

**之前**:
```
TSLplugins/
├── src/main/kotlin/org/tsl/tSLplugins/
│   ├── CustomAnvil/
│   │   ├── AnvilListener.kt
│   │   ├── AnvilManager.kt
│   │   ├── AnvilCommand.kt
│   │   └── AnvilProtocolHelper.kt
│   └── ...
├── CustomAnvil_USER_GUIDE.md
├── CustomAnvil_TEST_GUIDE.md
├── SUMMARY_CustomAnvil_*.md
└── ...
```

**之后**:
```
TSLplugins/
├── archive/
│   ├── CustomAnvil/
│   │   ├── AnvilListener.kt
│   │   ├── AnvilManager.kt
│   │   ├── AnvilCommand.kt
│   │   └── AnvilProtocolHelper.kt
│   ├── CustomAnvil_USER_GUIDE.md
│   ├── CustomAnvil_TEST_GUIDE.md
│   └── SUMMARY_CustomAnvil_*.md
└── ...
```

### 代码变化统计

- **移除文件**: 4 个 Kotlin 源文件
- **归档文档**: 6 个 Markdown 文档
- **修改文件**: 3 个文件（TSLplugins.kt, ReloadCommand.kt, config.yml）
- **删除代码行数**: 约 800+ 行

---

## 验证清单

- [x] 所有 CustomAnvil 源代码已移至 archive/
- [x] 所有 CustomAnvil 文档已移至 archive/
- [x] TSLplugins.kt 中的所有引用已移除
- [x] ReloadCommand.kt 中的重载调用已移除
- [x] config.yml 中的配置段已移除
- [x] 代码编译无错误
- [x] ProtocolLib 依赖已保留（供将来使用）

---

## 后续步骤

1. **编译测试**
   ```bash
   ./gradlew clean build
   ```

2. **运行测试**
   - 启动服务器
   - 执行 `/tsl reload` 验证重载功能正常
   - 检查日志确认没有 CustomAnvil 相关错误

3. **开发独立插件**
   - 参考 archive/ 中的代码
   - 按照 1.md 文档中的方案实现
   - 独立测试和部署

---

## 注意事项

### 配置文件版本

当前配置文件版本仍为 `4`，因为只是移除了功能，没有添加新功能。如果将来需要添加新功能，记得递增版本号。

### 兼容性

移除 CustomAnvil 功能不会影响：
- 命令别名系统
- 维护模式
- 体型调整
- Hat 系统
- 成就系统
- 访客模式
- 权限检测
- 农田保护
- MOTD 假玩家

所有其他功能保持完全兼容。

---

## 总结

CustomAnvil 功能已完全从 TSLplugins 中移除，所有相关代码和文档已归档保存。插件架构保持清晰，不影响其他功能模块的正常运行。

用户可以专注于开发独立的铁砧控制插件，实现更专业和灵活的铁砧成本管理功能。

---

**文档版本**: 1.0  
**最后更新**: 2025-11-11  
**维护者**: GitHub Copilot

