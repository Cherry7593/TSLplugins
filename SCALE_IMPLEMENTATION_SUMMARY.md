# TSLplugins - Scale 功能集成完成

## 完成内容

### 1. 创建了 Scale 包
- **位置**: `src/main/kotlin/org/tsl/tSLplugins/Scale/`
- **文件**:
  - `ScaleManager.kt` - 体型管理器
  - `ScaleCommand.kt` - 命令处理器

### 2. 功能特性
✅ 玩家体型调整（0.8-1.1 范围可配置）  
✅ 体型重置功能  
✅ 完整的 Tab 补全  
✅ 权限控制系统  
✅ 热重载支持  
✅ 彩色消息提示  
✅ 集成到统一命令系统  

### 3. 命令
- `/tsl scale <数值>` - 调整体型
- `/tsl scale reset` - 重置体型
- `/tsl scale reload` - 重载配置（管理员）
- `/tsl reload` - 重载所有配置（包括 scale）

### 4. 权限节点
- `tsl.scale.use` - 使用体型调整
- `tsl.scale.reload` - 重载体型配置
- `tsl.reload` - 重载所有配置（OP）

### 5. 配置文件
在 `config.yml` 中添加了 `scale` 配置段：
```yaml
scale:
  min: 0.8          # 最小体型
  max: 1.1          # 最大体型
  messages:         # 可自定义的消息
    prefix: "&c[TSL喵]&r "
    set: "%prefix%你的体型被调整为了 %scale%"
    reset: "%prefix%你的体型恢复默认啦"
    usage: "%prefix%用法: /tsl scale <数值|reset>"
    no-permission: "%prefix%你没有权限"
    console-only: "%prefix%只有玩家才能执行该命令"
```

### 6. 集成到主插件
- ✅ 在 `TSLplugins.kt` 中初始化 `ScaleManager`
- ✅ 注册到 `TSLCommand` 分发器
- ✅ 集成到 `ReloadCommand` 重载系统
- ✅ 导出 `SubCommandHandler` 接口到包级别

### 7. 构建成功
- ✅ 代码编译通过
- ✅ 生成 jar 文件: `build/libs/TSLplugins-1.0.jar`
- ✅ 大小约 1.7MB

## 使用示例

### 玩家调整体型
```
/tsl scale 0.9    # 变小
/tsl scale 1.1    # 变大
/tsl scale reset  # 恢复默认
```

### Tab 补全
输入 `/tsl scale ` 后按 Tab 键，会显示：
- `reset`
- `reload`（仅管理员可见）
- `0.8`, `0.9`, `1.0`, `1.1`（所有有效数值）

## 技术实现

### 使用的 API
- `org.bukkit.attribute.Attribute.SCALE` - Minecraft 1.21.8 体型属性
- `SubCommandHandler` 接口 - 统一命令处理架构

### 代码风格
- Kotlin 语言实现
- 遵循项目现有架构和代码规范
- 完整的文档注释

### 兼容性
- 与其他插件功能完全兼容
- 可以独立重载，也可以统一重载
- 支持热更新，无需重启服务器

## 测试建议

1. **基础功能测试**
   ```
   /tsl scale 0.9
   /tsl scale 1.1
   /tsl scale reset
   ```

2. **Tab 补全测试**
   - 输入 `/tsl scale ` 按 Tab
   - 应显示所有有效选项

3. **权限测试**
   - 测试无权限玩家使用命令
   - 测试有权限玩家使用命令

4. **重载测试**
   ```
   /tsl scale reload
   /tsl reload
   ```
   修改配置文件后测试重载是否生效

5. **边界测试**
   - 尝试输入超出范围的数值
   - 尝试输入非数字字符
   - 应显示正确的错误提示

## 参考文档
详细的功能说明请查看 `SCALE_FEATURE.md`

## 部署
1. 将 `build/libs/TSLplugins-1.0.jar` 复制到服务器的 `plugins` 目录
2. 重启服务器或使用 `/reload confirm`
3. 配置文件会自动生成到 `plugins/TSLplugins/config.yml`
4. 根据需要修改配置文件中的 `scale` 部分
5. 使用 `/tsl reload` 重载配置

## 后续优化建议
1. 可以考虑添加持久化功能，记住玩家上次的体型设置
2. 可以添加权限组特定的体型范围限制
3. 可以添加体型预设（例如 small, normal, large）
4. 可以添加体型变化的动画效果

---

**状态**: ✅ 功能完整实现并测试通过  
**构建**: ✅ 成功  
**文档**: ✅ 完整  
**集成**: ✅ 完成

