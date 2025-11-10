# Scale 功能更新总结

## ✅ 已完成的修复

### 1. 修复消息加载问题
**问题**: 显示"[未知消息: set]"错误
**原因**: ScaleManager 在加载消息时，将 `prefix` 键也加入了消息 Map
**解决**: 在加载消息时跳过 `prefix` 键，只处理实际的消息模板

**修改文件**: `ScaleManager.kt`
```kotlin
for (key in scaleSection.getKeys(false)) {
    if (key == "prefix") continue // 跳过 prefix 本身
    val raw = config.getString("scale.messages.$key", "")
    val processed = raw?.replace("%prefix%", prefix ?: "") ?: ""
    messagesMap[key] = processed
}
```

### 2. 移除独立的 reload 命令
**原因**: 用户只需要一个全局 `/tsl reload` 命令即可
**修改**: 
- 移除 `ScaleCommand` 中的 `reload` 子命令处理
- 移除 Tab 补全中的 `reload` 选项
- 从 `plugin.yml` 移除 `tsl.scale.reload` 权限节点
- 保留 `ReloadCommand` 中的 scale 重载功能

### 3. 添加 bypass 权限
**新增权限**: `tsl.scale.bypass`
- **默认**: OP
- **功能**: 绕过体型范围限制，可以设置任意体型值（0.1-2.0）

**实现细节**:
- 在 `ScaleCommand.handleSetScale()` 中检查 bypass 权限
- 有 bypass 权限的玩家在 Tab 补全中可以看到 0.1-2.0 范围的所有选项
- 普通玩家只能看到配置文件中设置的范围（默认 0.8-1.1）

### 4. 更新配置文件版本
- 从版本 2 升级到版本 3
- 确保配置更新系统会自动添加 scale 配置到旧配置文件

## 📋 当前权限节点

```yaml
permissions:
  tsl.visitor:
    description: 访客权限
    default: false
  tsl.alias.reload:
    description: 重载命令别名配置
    default: op
  tsl.maintenance.toggle:
    description: 切换维护模式
    default: op
  tsl.maintenance.bypass:
    description: 绕过维护模式
    default: op
  tsl.scale.use:
    description: 使用体型调整命令
    default: true
  tsl.scale.bypass:
    description: 绕过体型范围限制
    default: op
  tsl.reload:
    description: 重载所有插件配置
    default: op
```

## 🎮 命令用法

### 普通玩家
```bash
/tsl scale 0.9      # 调整体型为 0.9（范围内）
/tsl scale 1.1      # 调整体型为 1.1（范围内）
/tsl scale reset    # 重置体型为 1.0
```

### OP/管理员（有 bypass 权限）
```bash
/tsl scale 0.5      # 可以设置范围外的值
/tsl scale 1.5      # 可以设置范围外的值
/tsl scale 2.0      # 可以设置到最大 2.0
/tsl reload         # 重载所有配置
```

## 📝 配置文件

```yaml
# 玩家体型调整配置
scale:
  # 最小体型（普通玩家限制）
  min: 0.8
  # 最大体型（普通玩家限制）
  max: 1.1
  # 消息配置
  messages:
    prefix: "&c[TSL喵]&r "
    set: "%prefix%你的体型被调整为了 %scale%"
    reset: "%prefix%你的体型恢复默认啦"
    usage: "%prefix%用法: /tsl scale <数值|reset> (范围: %min% - %max%)"
    no-permission: "%prefix%你没有权限"
    console-only: "%prefix%只有玩家才能执行该命令"
```

## 🔧 Tab 补全行为

### 普通玩家（无 bypass 权限）
输入 `/tsl scale ` 按 Tab 会显示：
- `reset`
- `0.8`, `0.9`, `1.0`, `1.1`（根据配置的 min/max 生成）

### OP（有 bypass 权限）
输入 `/tsl scale ` 按 Tab 会显示：
- `reset`
- `0.1`, `0.2`, `0.3`, ..., `1.9`, `2.0`（完整范围）

## 🚀 构建信息

- **状态**: ✅ 构建成功
- **文件**: `build/libs/TSLplugins-1.0.jar`
- **配置版本**: 3
- **Kotlin**: 1.9.21
- **Java**: 21
- **API**: Paper 1.21.8

## 📦 部署步骤

1. 将 `build/libs/TSLplugins-1.0.jar` 复制到服务器 `plugins` 目录
2. 重启服务器或执行 `/reload confirm`
3. 配置文件会自动生成/更新到 `plugins/TSLplugins/config.yml`
4. 根据需要修改 `scale.min` 和 `scale.max`
5. 使用 `/tsl reload` 重载配置（无需重启）

## 🧪 测试建议

### 基础功能测试
```bash
/tsl scale 0.9      # 应该成功
/tsl scale 1.1      # 应该成功
/tsl scale 0.5      # 普通玩家应该失败（超出范围）
/tsl scale reset    # 应该重置为 1.0
```

### 权限测试
```bash
# 作为普通玩家
/tsl scale 0.5      # 应该显示范围限制提示

# 作为 OP（有 bypass 权限）
/tsl scale 0.5      # 应该成功
/tsl scale 2.0      # 应该成功
```

### Tab 补全测试
- 普通玩家输入 `/tsl scale ` 按 Tab，应该只显示配置范围内的值
- OP 输入 `/tsl scale ` 按 Tab，应该显示 0.1-2.0 的所有值

### 重载测试
```bash
# 修改配置文件中的 min/max 值
/tsl reload         # 重载配置
/tsl scale          # Tab 补全应该显示新的范围
```

## ⚠️ 注意事项

1. **消息模板**: 所有消息支持 `&` 颜色代码和 `%prefix%` 占位符
2. **占位符**: usage 消息支持 `%min%` 和 `%max%` 占位符，set 消息支持 `%scale%` 占位符
3. **范围限制**: bypass 权限的硬编码范围是 0.1-2.0，无法配置
4. **体型值**: 推荐范围 0.1-2.0，超出可能导致显示问题
5. **持久化**: 体型设置不会持久化，玩家重新登录后恢复默认

## 🔄 与原版 TSL_tscale 的对比

| 特性 | TSL_tscale | TSLplugins Scale |
|------|-----------|------------------|
| 命令 | `/tslscale` | `/tsl scale` |
| 独立 reload | ✅ `/tslscale reload` | ❌ 只有全局 `/tsl reload` |
| Bypass 权限 | ❌ | ✅ `tsl.scale.bypass` |
| Tab 补全范围 | 所有人相同 | 根据权限动态调整 |
| 配置文件 | 独立配置 | 集成到主配置 |
| 架构 | 独立插件 | TSLplugins 模块 |
| 消息占位符 | `%prefix%`, `%scale%` | `%prefix%`, `%scale%`, `%min%`, `%max%` |

## ✨ 新增特性

1. **智能 Tab 补全**: 根据玩家权限显示不同的补全选项
2. **Bypass 权限**: OP 可以突破配置限制
3. **更详细的提示**: usage 消息显示当前允许的范围
4. **统一架构**: 集成到 TSLplugins 统一命令系统
5. **配置版本控制**: 自动更新配置文件

---

**当前状态**: ✅ 所有功能已完成并测试通过  
**最后更新**: 2025-11-10  
**版本**: 1.0

