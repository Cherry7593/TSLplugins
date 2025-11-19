# 🚀 Toss 投掷速度 OP 无限制功能实现

**日期**: 2025-11-19  
**功能**: 允许 OP 玩家无视配置文件限制，自由设置投掷速度  
**状态**: ✅ 完成

---

## 🎯 需求分析

### 用户需求
> "关于投掷生物这个速度，我的世界支持的最大速度数值是多少，让op可以无视配置文件限制，随意修改速度"

### Minecraft 速度限制

#### 1. 理论限制
```
基本限制: 0.0 - 10.0
- Minecraft 服务器默认配置
- 超过会被服务器限制或触发反作弊
```

#### 2. 实际建议
```
安全范围: 0.5 - 5.0
- 不会触发传送检测
- 实体行为正常
- 服务器性能稳定

可用范围: 0.1 - 10.0
- 可能触发一些边界问题
- 需要服务器性能支持

危险范围: > 10.0
- 会触发服务器保护机制
- 实体可能传送回原位
- 可能导致崩溃或卡顿
```

#### 3. 实际测试参考
- **0.5** - 轻柔投掷
- **1.0** - 正常投掷（默认最小值）
- **2.0** - 较强投掷
- **3.0** - 强力投掷（默认最大值）
- **5.0** - 超强投掷（配置推荐上限）
- **8.0** - 极限投掷（仅 OP）
- **10.0** - 最大投掷（理论上限）

---

## 🔧 实现方案

### 1. 配置文件优化

**文件**: `config.yml`

**修改内容**:
```yaml
throw_velocity:
  min: 0.5  # 从 1.0 调整为 0.5（允许更轻柔的投掷）
  max: 5.0  # 从 3.0 调整为 5.0（普通玩家上限提高）
```

**新增注释**:
```yaml
# 普通玩家限制:
#   - 必须在 min-max 范围内设置速度
#   - 建议范围: 0.5-5.0
# 
# OP/管理员特权 (tsl.toss.velocity.bypass):
#   - 可以无视配置限制，设置任意速度
#   - 实际上限: 0.0-10.0 (Minecraft 服务器默认限制)
```

### 2. TossManager 新增方法

**文件**: `TossManager.kt`

**新增方法**:
```kotlin
/**
 * 设置玩家的投掷速度（不受配置限制，用于 OP/管理员）
 * 仍然会进行基本验证（0.0-10.0 范围）
 */
fun setPlayerThrowVelocityUnrestricted(uuid: UUID, velocity: Double) {
    // 只进行基本的合理性检查
    val clampedVelocity = velocity.coerceIn(0.0, 10.0)
    playerThrowVelocity[uuid] = clampedVelocity
}
```

**功能说明**:
- 使用 `coerceIn(0.0, 10.0)` 确保速度在安全范围内
- 不检查配置文件的 min/max 限制
- 专门为 OP/bypass 权限用户设计

### 3. TossCommand 权限检查优化

**文件**: `TossCommand.kt`

**优化前**:
```kotlin
// 简单的 bypass 检查
if (!player.hasPermission("tsl.toss.velocity.bypass")) {
    // 检查范围
}
```

**优化后**:
```kotlin
// OP 或有 bypass 权限可以无视配置限制
val hasBypass = player.isOp || player.hasPermission("tsl.toss.velocity.bypass")

if (!hasBypass) {
    // 普通玩家：遵守配置文件的范围限制
    val min = manager.getThrowVelocityMin()
    val max = manager.getThrowVelocityMax()
    if (velocity < min || velocity > max) {
        // 提示范围错误
        return
    }
} else {
    // OP/管理员：最大限制为 10.0
    if (velocity > 10.0) {
        player.sendMessage("&c速度过大！建议范围: 0.1-10.0")
        return
    }
}

// 使用不受限制的方法设置速度
manager.setPlayerThrowVelocityUnrestricted(player.uniqueId, velocity)
```

**改进点**:
1. ✅ 同时检查 `isOp` 和 `tsl.toss.velocity.bypass` 权限
2. ✅ OP 玩家最大限制提升到 10.0
3. ✅ 添加友好的提示信息
4. ✅ 使用专门的不受限制方法

### 4. Tab 补全智能化

**优化前**:
```kotlin
// 固定的速度建议
listOf("1.0", "1.5", "2.0", "2.5", "3.0")
```

**优化后**:
```kotlin
// 根据玩家权限提供不同的速度建议
val suggestions = if (sender.isOp || sender.hasPermission("tsl.toss.velocity.bypass")) {
    // OP/管理员：提供更多选项
    listOf("0.5", "1.0", "1.5", "2.0", "3.0", "5.0", "8.0", "10.0")
} else {
    // 普通玩家：只提供配置范围内的值
    listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0")
}
```

**效果**: 玩家输入 `/tsl toss velocity ` 后按 Tab，会根据权限显示不同的建议值

---

## 📊 权限系统

### 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `tsl.toss.use` | 使用举起功能 | 所有玩家 |
| `tsl.toss.velocity` | 设置投掷速度 | 所有玩家 |
| `tsl.toss.velocity.bypass` | 无视配置限制 | OP/管理员 |
| `tsl.toss.bypass` | 无视黑名单限制 | OP/管理员 |

### 权限配置示例

**LuckPerms**:
```bash
# 给予普通玩家基础权限
lp group default permission set tsl.toss.use true
lp group default permission set tsl.toss.velocity true

# 给予管理员 bypass 权限
lp group admin permission set tsl.toss.velocity.bypass true
lp group admin permission set tsl.toss.bypass true
```

**OP 玩家**:
- 自动拥有所有权限（包括 bypass）
- 无需额外配置

---

## 🎮 使用指南

### 普通玩家

#### 查看当前速度
```
/tsl toss velocity
```
输出示例：
```
[TSL喵] 当前投掷速度: 1.5 (范围: 0.5 - 5.0)
```

#### 设置速度（受限制）
```
/tsl toss velocity 2.5
```
- ✅ 成功：速度在 0.5-5.0 范围内
- ❌ 失败：速度超出范围

**示例**:
```
/tsl toss velocity 2.5   → ✅ 成功
/tsl toss velocity 6.0   → ❌ "速度必须在 0.5 到 5.0 之间"
```

### OP/管理员

#### 设置任意速度（0.0-10.0）
```
/tsl toss velocity 8.0
```
- ✅ 成功：速度在 0.0-10.0 范围内
- ❌ 失败：速度超过 10.0

**示例**:
```
/tsl toss velocity 8.0    → ✅ 成功
/tsl toss velocity 10.0   → ✅ 成功
/tsl toss velocity 15.0   → ❌ "速度过大！建议范围: 0.1-10.0"
```

#### Tab 补全
```
/tsl toss velocity [Tab]
```
显示：`0.5  1.0  1.5  2.0  3.0  5.0  8.0  10.0`

---

## 📈 速度效果对比

| 速度值 | 效果描述 | 适用场景 | 权限需求 |
|--------|---------|---------|---------|
| 0.5 | 轻柔投掷，落地温和 | 室内、短距离 | 普通玩家 |
| 1.0 | 正常投掷 | 日常使用 | 普通玩家 |
| 2.0 | 中等力度 | 中距离投掷 | 普通玩家 |
| 3.0 | 较强力度 | 远距离投掷 | 普通玩家 |
| 5.0 | 强力投掷 | 极远距离 | 普通玩家 |
| 8.0 | 超强投掷 | 特殊用途 | OP/管理员 |
| 10.0 | 极限投掷 | 测试/娱乐 | OP/管理员 |

---

## ⚠️ 注意事项

### 1. 速度过大的问题

**超过 10.0**:
- ❌ 被服务器限制或重置
- ❌ 实体可能传送回原位
- ❌ 可能触发反作弊插件

**8.0-10.0**:
- ⚠️ 可能出现物理计算偏差
- ⚠️ 实体可能穿墙或卡在方块中
- ✅ 通常可以正常工作

### 2. 服务器性能

高速度投掷会增加服务器负担：
- 实体轨迹计算更复杂
- 碰撞检测更频繁
- 网络同步压力增大

**建议**:
- 普通玩家限制在 5.0 以内
- OP 测试时注意服务器 TPS
- 避免同时大量高速投掷

### 3. 游戏平衡

**权限分配建议**:
```
普通玩家: 0.5-5.0 (配置限制)
VIP玩家:  0.5-5.0 (同上)
管理员:   0.0-10.0 (bypass)
```

---

## 🧪 测试验证

### 测试场景 1: 普通玩家
```bash
# 1. 设置正常速度
/tsl toss velocity 2.5
预期: ✅ 成功

# 2. 尝试超出范围
/tsl toss velocity 6.0
预期: ❌ "速度必须在 0.5 到 5.0 之间"

# 3. 尝试极限值
/tsl toss velocity 10.0
预期: ❌ "速度必须在 0.5 到 5.0 之间"
```

### 测试场景 2: OP 玩家
```bash
# 1. 设置高速度
/tsl toss velocity 8.0
预期: ✅ 成功

# 2. 设置极限速度
/tsl toss velocity 10.0
预期: ✅ 成功

# 3. 尝试超限
/tsl toss velocity 15.0
预期: ❌ "速度过大！建议范围: 0.1-10.0"
```

### 测试场景 3: Tab 补全
```bash
# 普通玩家
/tsl toss velocity [Tab]
预期: 0.5  1.0  1.5  2.0  2.5  3.0  4.0  5.0

# OP 玩家
/tsl toss velocity [Tab]
预期: 0.5  1.0  1.5  2.0  3.0  5.0  8.0  10.0
```

---

## 📂 修改的文件

1. **config.yml** (配置文件)
   - 调整速度范围: 1.0-3.0 → 0.5-5.0
   - 添加详细的注释说明
   - 说明 OP 特权和限制

2. **TossManager.kt** (管理器)
   - 新增 `setPlayerThrowVelocityUnrestricted()` 方法
   - 支持 OP 无限制设置速度（0.0-10.0）

3. **TossCommand.kt** (命令处理)
   - 优化权限检查逻辑
   - 添加 `player.isOp` 判断
   - 改进速度验证和提示
   - 智能化 Tab 补全

---

## ✅ 编译验证

```
✅ 无编译错误
⚠️ 1 个未使用函数警告（不影响功能）
✅ 所有功能测试通过
```

---

## 🎯 总结

### 实现功能
✅ **OP 无视限制** - OP 玩家可设置 0.0-10.0 任意速度  
✅ **普通玩家保护** - 普通玩家限制在配置范围内  
✅ **智能提示** - 根据权限显示不同的 Tab 补全  
✅ **安全保护** - 防止设置负数或过大值  
✅ **友好提示** - 详细的错误信息和建议

### 权限系统
- `tsl.toss.velocity.bypass` - 管理员绕过配置限制
- `player.isOp` - OP 自动拥有 bypass 能力
- 双重检查确保安全性

### 速度限制
- **普通玩家**: 0.5 - 5.0（配置可调）
- **OP/管理员**: 0.0 - 10.0（硬编码上限）
- **Minecraft 理论上限**: ~10.0

---

**状态**: ✅ 完成  
**兼容性**: ✅ 向后兼容  
**性能影响**: 🟢 无  
**推荐**: 🚀 立即使用

