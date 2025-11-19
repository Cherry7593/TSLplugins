# 📚 文档索引

**最后更新**: 2025-11-20

---

## 核心文档

### 主要文档
1. **README.md** - 项目简介和快速开始
2. **DEV_NOTES.md** - 开发笔记和技术要点 ⭐
3. **USER_GUIDE.md** - 用户使用指南
4. **DOCS_STRUCTURE.md** - 文档结构说明

---

## 开发文档 (archive/)

### 功能开发总结

#### Ride & Toss 功能
- **Ride_Toss_Development_Summary.md** ⭐  
  Ride 和 Toss 功能的综合开发总结
  - 功能概述
  - 技术实现
  - 修复的问题
  - 代码优化成果
  - 配置说明
  - OP 无限制功能
  - 测试要点
  - 开发经验总结

### 详细技术文档

#### 代码优化
- **SUMMARY_Code_Optimization.md**  
  详细的代码优化说明
  - RideListener 优化（6项）
  - TossListener 优化（14项）
  - 性能提升数据
  - Kotlin 最佳实践

#### OP 特权功能
- **SUMMARY_Toss_Velocity_OP_Unlimited.md**  
  OP 速度无限制功能说明
  - Minecraft 速度限制分析
  - 实现方案
  - 权限系统
  - 使用指南
  - 配置示例

#### 问题修复记录
- **SUMMARY_Throw_Velocity_Fix.md**  
  Vector.y 只读属性问题修复
  - 问题：`throwVelocity.y = value` 赋值错误
  - 解决：使用 `setY()` 方法
  - Bukkit Vector API 说明

- **SUMMARY_Left_Click_Air_Fix.md**  
  左键空气无法投掷问题修复
  - 问题：`ignoreCancelled = true` 导致事件被忽略
  - 解决：移除 `ignoreCancelled` 参数
  - 事件系统说明

---

## 文档结构

```
TSLplugins/
├── README.md                    # 项目简介
├── DEV_NOTES.md                # 开发笔记 ⭐
├── USER_GUIDE.md               # 用户指南
├── DOCS_STRUCTURE.md           # 文档结构
├── OVERVIEW.md                 # 项目概览
│
└── archive/                    # 开发文档归档
    ├── DOC_INDEX.md                              # 本文档 ⭐
    ├── Ride_Toss_Development_Summary.md          # 综合开发总结 ⭐
    ├── SUMMARY_Code_Optimization.md              # 代码优化详情
    ├── SUMMARY_Toss_Velocity_OP_Unlimited.md     # OP 速度无限制
    ├── SUMMARY_Throw_Velocity_Fix.md             # 投掷速度修复
    └── SUMMARY_Left_Click_Air_Fix.md             # 左键空气修复
```

---

## 文档更新记录

### 2025-11-20
- ✅ 清理冗余文档（删除 13 个重复/过时文档）
- ✅ 创建综合开发总结（Ride_Toss_Development_Summary.md）
- ✅ 更新 DEV_NOTES.md（补充 Ride 详细说明）
- ✅ 创建文档索引（本文档）

### 删除的文档
以下文档因重复或过时已删除：
- CustomAnvil 相关（4个）- 功能已废弃
- 调试相关（2个）- 问题已解决
- 部分修复记录（3个）- 已整合到综合文档
- 重复的最终报告（4个）- 保留最完整版本

---

## 快速查找

### 我想了解...

**功能使用**:
→ USER_GUIDE.md

**开发技术细节**:
→ DEV_NOTES.md

**Ride/Toss 功能全貌**:
→ archive/Ride_Toss_Development_Summary.md

**代码优化方法**:
→ archive/SUMMARY_Code_Optimization.md

**OP 速度无限制**:
→ archive/SUMMARY_Toss_Velocity_OP_Unlimited.md

**具体问题修复**:
→ archive/SUMMARY_*_Fix.md

---

## 文档维护原则

### 保留标准
- ✅ 有独特技术价值
- ✅ 包含重要参考信息
- ✅ 解决了特定问题
- ✅ 可作为开发经验总结

### 删除标准
- ❌ 内容重复
- ❌ 功能已废弃
- ❌ 临时调试文档
- ❌ 已被更全面的文档替代

### 整合标准
- 多个相关文档 → 创建综合文档
- 零散的修复记录 → 整合到功能总结
- 重复的最终报告 → 保留最完整版本

---

**维护者**: GitHub Copilot  
**最后清理**: 2025-11-20  
**文档数量**: 主文档 4 个 + 归档文档 6 个

