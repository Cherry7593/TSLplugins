# 小镇PHome模块优化记录

> 审查日期: 2026-01-22

## 待修复问题清单

### P1 - 重要优化

- [x] **1. 异步文件 I/O**
  - 文件: `TownPHomeStorage.kt`
  - 问题: 每次 setHome/removeHome 都同步写入文件
  - 方案: 使用脏标记 + 定期保存（与 LandmarkStorage 保持一致）

---

## 修复记录

### 修复 #1: 异步文件 I/O
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - 添加 `dirty` AtomicBoolean 脏标记
  - `setHome()`/`removeHome()` 改为调用 `markDirty()` 标记脏数据
  - 添加 `startAutoSave()` 60秒定期保存任务
  - 添加 `forceSaveAll()` 用于插件关闭时强制保存
  - `saveAll()` 仅在脏标记为 true 时执行保存
