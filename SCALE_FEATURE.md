# Scale 功能说明

## 功能描述
玩家体型调整功能，允许玩家通过命令调整自己的体型大小。

## 命令
- `/tsl scale <数值>` - 调整玩家体型（数值范围在配置文件中设置，默认 0.8-1.1）
- `/tsl scale reset` - 重置玩家体型为默认值（1.0）
- `/tsl scale reload` - 重载体型配置（需要管理员权限）
- `/tsl reload` - 重载所有插件配置（包括体型配置）

## 权限
- `tsl.scale.use` - 使用体型调整命令的基础权限
- `tsl.scale.reload` - 重载体型配置的权限
- `tsl.reload` - 重载所有配置的权限（OP）

## 配置文件
在 `config.yml` 中添加了以下配置：

```yaml
# 玩家体型调整配置
scale:
  # 最小体型
  min: 0.8
  # 最大体型
  max: 1.1
  # 消息配置
  messages:
    # 命令前缀
    prefix: "&c[TSL喵]&r "
    # 成功调整体型时的消息
    set: "%prefix%你的体型被调整为了 %scale%"
    # 体型重置时的消息
    reset: "%prefix%你的体型恢复默认啦"
    # 用法提示
    usage: "%prefix%用法: /tsl scale <数值|reset>"
    # 权限不足
    no-permission: "%prefix%你没有权限"
    # 仅玩家可用
    console-only: "%prefix%只有玩家才能执行该命令"
```

## Tab 补全
命令支持 Tab 补全，会自动显示：
- `reset` - 重置体型
- `reload` - 重载配置（仅管理员可见）
- `0.8`, `0.9`, `1.0`, `1.1` 等有效的体型数值（根据配置文件中的 min 和 max 自动生成，步长 0.1）

## 实现细节

### 文件结构
```
src/main/kotlin/org/tsl/tSLplugins/Scale/
├── ScaleManager.kt      # 体型管理器，负责配置加载和体型设置
└── ScaleCommand.kt      # 命令处理器，负责处理命令和 Tab 补全
```

### 功能特性
1. **配置驱动**：体型范围和所有消息都可在配置文件中自定义
2. **权限控制**：支持细粒度的权限控制
3. **热重载**：支持通过命令重载配置，无需重启服务器
4. **用户友好**：提供完整的 Tab 补全和彩色消息提示
5. **统一架构**：使用插件统一的命令分发架构（SubCommandHandler）

### 技术实现
- 使用 Minecraft 1.21.8 的 `Attribute.SCALE` 属性来调整玩家体型
- 配置文件使用 YAML 格式，支持颜色代码（`&` 符号）
- 命令处理器实现了 `SubCommandHandler` 接口，与插件其他功能保持一致
- 支持 `/tsl reload` 统一重载，也支持独立的 `/tsl scale reload`

## 使用示例

### 玩家使用
```
/tsl scale 0.9       # 将体型调整为 0.9
/tsl scale 1.1       # 将体型调整为 1.1（最大值）
/tsl scale reset     # 重置为默认体型
```

### 管理员使用
```
/tsl scale reload    # 仅重载体型配置
/tsl reload          # 重载所有插件配置
```

## 注意事项
1. 体型调整仅对玩家自己可见和生效
2. 体型数值必须在配置文件设置的范围内（默认 0.8-1.1）
3. 体型调整会影响玩家的碰撞箱和渲染大小
4. 修改配置文件后需要执行重载命令才能生效

## 与原版 TSL_tscale 的区别
1. **架构集成**：作为 TSLplugins 的一个模块，而不是独立插件
2. **命令统一**：使用 `/tsl scale` 而不是 `/tslscale`
3. **统一重载**：可以通过 `/tsl reload` 一次性重载所有功能
4. **Kotlin 实现**：使用 Kotlin 语言，代码更简洁和现代化
5. **配置统一**：配置在主配置文件 `config.yml` 中，而不是单独的配置文件

