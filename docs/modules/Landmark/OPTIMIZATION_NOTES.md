# 地标模块优化记录

> 审查日期: 2026-01-22

## 待修复问题清单

### P0 - 必须修复

- [x] **1. findSafeLocation 线程安全问题**
  - 文件: `LandmarkManager.kt:200-232`
  - 问题: 访问方块数据可能在错误的区域线程执行，Folia 上有崩溃风险
  - 方案: 移除 safeLanding 中的同步方块检查，warpPoint 直接使用；中心点传送使用 getHighestBlockYAt

### P1 - 重要优化

- [x] **2. 异步文件 I/O**
  - 文件: `LandmarkStorage.kt`
  - 问题: 每次更新都同步写入文件，高频操作时卡顿
  - 方案: 使用脏标记 + 定期保存（60秒）

- [x] **3. getOfflinePlayer 阻塞调用**
  - 文件: `LandmarkCommand.kt:254,288`
  - 问题: 可能触发网络请求阻塞主线程
  - 方案: 优先使用 getPlayerExact + getOfflinePlayerIfCached

### P2 - 代码清理

- [x] **4. 移除 GUI 冗余状态 Map**
  - 文件: `LandmarkGUI.kt:31-38`
  - 问题: 状态已存储在 holder 中，Map 是冗余的
  - 方案: 删除 playerPages, playerMenuType, playerEditingLandmark

- [x] **5. 修复可视化任务管理**
  - 文件: `LandmarkOPKTool.kt:42,241,247-249`
  - 问题: taskId.hashCode() 没实际用途，任务无法正确取消
  - 方案: 存储 ScheduledTask 对象

### P3 - 可选清理

- [x] **6. 移除未使用代码**
  - `LandmarkData.kt:110-117` - LandmarkCreationSession 未使用
  - `LandmarkVisibility` 枚举被 Landmark 类使用，保留

---

## 修复记录

### 修复 #1: findSafeLocation 线程安全
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - 移除了 `findSafeLocation()`, `isSafeBlock()`, `isSolidBlock()` 方法
  - warpPoint 直接使用（已在设置时验证安全）
  - 中心点传送使用 `world.getHighestBlockYAt()` + 1（线程安全）

### 修复 #2: 异步文件 I/O
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - 添加 `landmarksDirty`, `unlocksDirty` AtomicBoolean 脏标记
  - `saveLandmarks()`/`saveUnlocks()` 仅设置脏标记
  - 添加 `startAutoSave()` 60秒定期保存
  - 添加 `forceSaveAll()` 用于插件关闭时强制保存

### 修复 #3: getOfflinePlayer 阻塞
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - trust/untrust 命令改用 `Bukkit.getPlayerExact() ?: Bukkit.getOfflinePlayerIfCached()`
  - 未缓存玩家给出友好提示

### 修复 #4: GUI 冗余状态
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - 移除 `playerPages`, `playerMenuType`, `playerEditingLandmark` Map
  - 移除 `onInventoryClose` 事件处理器
  - 移除未使用的 import

### 修复 #5: 可视化任务管理
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - `visualizationTasks` 类型改为 `MutableMap<UUID, ScheduledTask>`
  - `stopVisualization()` 正确调用 `task.cancel()`

### 修复 #6: 未使用代码
- 日期: 2026-01-22
- 状态: ✅ 已完成
- 变更:
  - 移除 `LandmarkCreationSession` 数据类
  - `LandmarkVisibility` 保留（被 Landmark 类使用）
