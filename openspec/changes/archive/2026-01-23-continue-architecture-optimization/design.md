# Design: 继续架构优化

## Context

TSLplugins 已完成第一阶段架构优化（31 模块完全迁移）。当前处于过渡状态：
- 核心框架 (`core/`) 已就绪
- 新模块目录 (`modules/`) 包含 49 个模块入口
- 15 个包装器模块仍引用旧目录代码

## Goals / Non-Goals

### Goals
- 将所有模块代码统一到 `modules/` 目录
- 保持功能完全兼容
- 清理旧目录，减少维护成本

### Non-Goals
- 不改变模块功能行为
- 不重写业务逻辑
- 不在本阶段处理包名大小写问题（保持 `tSLplugins`）

## Decisions

### 1. 迁移策略：直接移动 + 更新包名

**选择**: 将旧文件直接移动到 `modules/xxx/` 目录，仅更新包名和导入

**原因**:
- 风险最低，保持代码逻辑不变
- 快速完成迁移，验证构建
- 后续可根据需要逐步优化

**替代方案**: 
- 完全重写：风险高，工作量大，收益有限
- 保持现状：维护成本持续增加

### 2. 批次划分：按复杂度递增

**选择**: 从简单模块开始，逐步处理复杂模块

**原因**:
- 简单模块快速完成，建立信心
- 复杂模块有更多缓冲时间处理意外问题
- 每批次独立验证，问题易定位

### 3. 文件命名：保持原名

**选择**: 移动后保持原文件名（如 `AliasManager.kt` 而非 `AliasModuleManager.kt`）

**原因**:
- 减少改动范围
- 已迁移模块中部分也保留了原名（如 FreezeModule 中的内部类）
- 避免不必要的重命名引入错误

### 4. Module 类处理：简化为直接实例化

**选择**: Module 类从"包装器"模式改为直接实例化本地类

**示例**:
```kotlin
// 迁移前（包装器模式）
import org.tsl.tSLplugins.Alias.AliasManager  // 引用旧目录

// 迁移后（本地实例化）
import org.tsl.tSLplugins.modules.alias.AliasManager  // 引用本地文件
```

## Risks / Trade-offs

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 遗漏导入更新 | 中 | 编译失败 | 每批次构建验证 |
| 跨模块依赖断裂 | 低 | 运行时错误 | 检查类引用关系 |
| 复杂模块内部依赖 | 中 | 需要额外调整 | 批次 4 预留更多时间 |

## Migration Checklist (Per Module)

每个模块迁移时执行：

1. **移动文件**
   ```bash
   mv src/.../OldDir/*.kt src/.../modules/newdir/
   ```

2. **更新包名**
   ```kotlin
   // 修改每个文件的 package 声明
   package org.tsl.tSLplugins.modules.xxx
   ```

3. **更新导入**
   - 更新模块内部文件的相互导入
   - 更新 Module.kt 的导入（移除旧目录引用）

4. **检查外部引用**
   - 搜索是否有其他文件引用旧目录
   - 如有，更新引用路径

5. **删除旧目录**
   ```bash
   rm -rf src/.../OldDir/
   ```

6. **验证构建**
   ```bash
   ./gradlew classes
   ```

## Open Questions

1. **PlayerList 模块是否需要完整 Module 化？**
   - 当前仅是一个 SubCommand，无 Manager/Listener
   - 建议：创建简单的 PlayerListModule 包装

2. **后续是否需要统一文件命名规范？**
   - 当前：部分使用 `XxxModuleCommand.kt`，部分使用 `XxxCommand.kt`
   - 建议：本阶段保持现状，后续统一
