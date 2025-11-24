# TSLPlaceholderExpansion 移至根目录优化总结

**日期**: 2025-11-25  
**类型**: 架构优化

---

## 优化内容

### 1. 文件移动

将 `TSLPlaceholderExpansion.kt` 从功能模块目录移至根目录：

**移动前**：
```
Advancement/
├── AdvancementCount.kt
├── AdvancementMessage.kt
├── AdvancementCommand.kt
└── TSLPlaceholderExpansion.kt  ❌ 位置不合理
```

**移动后**：
```
根目录/
├── TSLplugins.kt
├── TSLCommand.kt
├── ReloadCommand.kt
├── ConfigUpdateManager.kt
├── PlayerDataManager.kt
└── TSLPlaceholderExpansion.kt  ✅ 与其他核心文件一起
```

---

## 优化原因

### TSLPlaceholderExpansion 的特殊性

`TSLPlaceholderExpansion` 是一个**跨模块的核心系统**，具有以下特点：

1. **整合多个模块的变量**
   - Advancement（成就统计）
   - Ping（延迟查询）
   - Kiss（亲吻统计）
   - Ride（骑乘开关）
   - Toss（举起开关和速度）

2. **依赖多个 Manager**
   ```kotlin
   class TSLPlaceholderExpansion(
       private val countHandler: AdvancementCount,
       private val pingManager: PingManager?,
       private val kissManager: KissManager?,
       private val rideManager: RideManager?,
       private val tossManager: TossManager?
   )
   ```

3. **不属于单一模块**
   - 之前放在 Advancement 包中不合理
   - 它服务于所有模块，不应属于任何一个

### 与其他根目录文件的一致性

| 根目录文件 | 作用 | 跨模块特性 |
|-----------|------|-----------|
| TSLplugins.kt | 初始化所有模块 | ✅ 管理所有模块 |
| TSLCommand.kt | 分发所有命令 | ✅ 处理所有模块命令 |
| ReloadCommand.kt | 重载所有模块 | ✅ 重载所有模块配置 |
| ConfigUpdateManager.kt | 配置版本控制 | ✅ 管理全局配置 |
| PlayerDataManager.kt | 统一数据管理 | ✅ 管理所有模块数据 |
| **TSLPlaceholderExpansion.kt** | **整合所有变量** | ✅ **提供所有模块变量** |

**所有根目录文件都是跨模块的核心基础设施！**

---

## 架构优势

### 1. 清晰的层次结构

```
根目录（核心系统层）
├── TSLplugins.kt              - 插件主类
├── TSLCommand.kt              - 命令系统
├── ReloadCommand.kt           - 重载系统
├── ConfigUpdateManager.kt     - 配置系统
├── PlayerDataManager.kt       - 数据系统
└── TSLPlaceholderExpansion.kt - 变量系统
    │
    ├── 服务所有业务模块
    ↓
模块目录（业务逻辑层）
├── Kiss/                      - 亲吻功能
├── Freeze/                    - 冻结功能
├── Toss/                      - 举起功能
└── ...                        - 其他功能
```

### 2. 避免架构混乱

**问题场景**：如果 TSLPlaceholderExpansion 放在 Advancement 包中
- ❌ 暗示它是 Advancement 模块的一部分
- ❌ 但实际上它依赖 Kiss、Ping、Toss 等多个模块
- ❌ 产生跨包依赖，违反模块独立原则

**优化后**：放在根目录
- ✅ 明确表示这是核心系统
- ✅ 与其他跨模块系统在一起
- ✅ 符合单一职责和分层架构原则

### 3. 便于理解和维护

开发者（包括 AI）查看项目结构时：

**之前**：
- "为什么 PAPI 扩展在 Advancement 包里？"
- "它是 Advancement 专用的吗？"
- "其他模块的变量在哪里？"

**现在**：
- "根目录有 6 个核心文件"
- "TSLPlaceholderExpansion 是变量系统，整合所有模块"
- "结构清晰，一目了然"

---

## 支持的 PAPI 变量

### 全局变量（无需玩家）

| 变量 | 说明 | 来源模块 |
|------|------|---------|
| `%tsl_ping%` | 服务器平均延迟 | Ping |

### 玩家变量

| 变量 | 说明 | 来源模块 |
|------|------|---------|
| `%tsl_adv_count%` | 玩家成就数量 | Advancement |
| `%tsl_kiss_count%` | 亲吻次数 | Kiss |
| `%tsl_kissed_count%` | 被亲吻次数 | Kiss |
| `%tsl_kiss_toggle%` | Kiss 开关状态 | Kiss |
| `%tsl_ride_toggle%` | Ride 开关状态 | Ride |
| `%tsl_toss_toggle%` | Toss 开关状态 | Toss |
| `%tsl_toss_velocity%` | Toss 投掷速度 | Toss |

**所有变量使用统一前缀 `tsl_`，在一个类中统一管理！**

---

## 开发者指南更新

已在开发者指南中新增**"根目录核心文件详解"**章节，详细说明：

### 新增内容

1. **为什么这些文件放在根目录？**
   - 解释根目录文件的特殊性
   - 说明跨模块核心系统的概念

2. **6 个核心文件的详细说明**
   - TSLplugins.kt - 主类
   - TSLCommand.kt - 命令分发器
   - ReloadCommand.kt - 重载系统
   - ConfigUpdateManager.kt - 配置版本控制
   - PlayerDataManager.kt - PDC 数据管理
   - **TSLPlaceholderExpansion.kt - PAPI 变量整合**（新增）

3. **开发流程优化**
   - 详细的 10 步添加新功能流程
   - 明确指出哪些根目录文件需要修改
   - 提供开发检查清单
   - 说明常见错误和解决方案

4. **根目录文件修改总结表**
   | 文件 | 是否必改 | 修改内容 |
   |------|---------|---------|
   | TSLplugins.kt | ✅ 必须 | 初始化、注册、reload |
   | ReloadCommand.kt | ✅ 必须 | 添加重载调用 |
   | ConfigUpdateManager.kt | ✅ 必须 | 递增版本号 |
   | PlayerDataManager.kt | ⚪ 可选 | PDC 数据方法 |
   | **TSLPlaceholderExpansion.kt** | ⚪ 可选 | **PAPI 变量** |

### 重点强调

在开发流程中特别强调了 **ReloadCommand.kt** 的重要性：

> **⚠️ 重要提醒**：
> - 如果忘记在 ReloadCommand 中添加，模块配置将无法重载！
> - 这是新手最容易忘记的步骤！

这与根目录文件的跨模块特性一致：
- **所有模块的重载操作都集中在 ReloadCommand.kt**
- **所有模块的 PAPI 变量都集中在 TSLPlaceholderExpansion.kt**

---

## 代码变更

### 1. 创建新文件

**文件位置**：`src/main/kotlin/org/tsl/tSLplugins/TSLPlaceholderExpansion.kt`

```kotlin
package org.tsl.tSLplugins

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import org.tsl.tSLplugins.Advancement.AdvancementCount
import org.tsl.tSLplugins.Ping.PingManager
import org.tsl.tSLplugins.Kiss.KissManager
import org.tsl.tSLplugins.Ride.RideManager
import org.tsl.tSLplugins.Toss.TossManager

/**
 * TSLplugins PlaceholderAPI 扩展
 * 
 * 这是一个核心系统文件，位于根目录，整合了所有模块的 PAPI 变量。
 */
class TSLPlaceholderExpansion(
    private val plugin: JavaPlugin,
    private val countHandler: AdvancementCount,
    private val pingManager: PingManager?,
    private val kissManager: KissManager?,
    private val rideManager: RideManager?,
    private val tossManager: TossManager?
) : PlaceholderExpansion() {
    // ...实现代码
}
```

### 2. 更新导入

**文件**：`TSLplugins.kt`

```kotlin
// 移除旧导入
// import org.tsl.tSLplugins.Advancement.TSLPlaceholderExpansion

// TSLPlaceholderExpansion 现在在同一个包中，无需导入
```

### 3. 删除旧文件

```
❌ 删除：src/main/kotlin/org/tsl/tSLplugins/Advancement/TSLPlaceholderExpansion.kt
```

---

## 测试验证

- ✅ 编译成功（无错误）
- ✅ JAR 文件生成（build/libs/TSLplugins-1.0.jar）
- ✅ 导入更新正确
- ✅ 文档更新完成

---

## 总结

### 优化成果

1. **架构更清晰**
   - 根目录 6 个核心文件，各司其职
   - 业务模块独立，不包含跨模块代码

2. **职责更明确**
   - TSLPlaceholderExpansion 作为 PAPI 变量系统
   - 与其他核心系统（命令、重载、配置、数据）并列

3. **文档更完善**
   - 开发者指南详细说明根目录文件的作用
   - 强调了跨模块系统的重要性
   - 提供了完整的开发流程和检查清单

### 设计原则体现

- ✅ **分层架构**：核心系统层 + 业务模块层
- ✅ **单一职责**：每个文件有明确的职责
- ✅ **模块独立**：业务模块不包含跨模块代码
- ✅ **易于理解**：结构清晰，一目了然

### 开发体验提升

- 开发者可以快速识别核心文件和业务模块
- 添加新功能时明确知道需要修改哪些根目录文件
- 维护时不会混淆业务逻辑和基础设施

---

**优化完成时间**: 2025-11-25  
**影响范围**: 架构优化、文档更新  
**向后兼容**: ✅ 完全兼容

