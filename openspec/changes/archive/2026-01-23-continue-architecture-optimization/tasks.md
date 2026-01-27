# Tasks: 继续架构优化

## 阶段 A：完全迁移包装器模块

### A1. 批次 1 - 简单模块（1-2 文件）

- [x] A1.1 迁移 Permission 模块
  - 将 `Permission/PermissionChecker.kt` 内容合并到 `modules/permissionchecker/PermissionCheckerModule.kt`
  - 更新包名和导入
  - 删除旧目录
  - 验证构建

- [x] A1.2 迁移 PlayerCountCmd 模块
  - 将 `PlayerCountCmd/PlayerCountCmdController.kt` 移入 `modules/playercountcmd/`
  - 更新包名和导入
  - 删除旧目录
  - 验证构建

- [x] A1.3 迁移 XconomyTrigger 模块
  - 将 `XconomyTrigger/` 下 2 个文件移入 `modules/xconomytrigger/`
  - 更新包名和导入
  - 删除旧目录
  - 验证构建

- [x] A1.4 创建 PlayerList 模块
  - 创建 `modules/playerlist/PlayerListModule.kt`
  - 将 `PlayerList/PlayerListCommand.kt` 移入
  - 更新 TSLplugins.kt 注册
  - 删除旧目录
  - 验证构建

### A2. 批次 2 - 中等模块（3 文件）

- [x] A2.1 迁移 Alias 模块
  - 将 `Alias/` 下 3 个文件移入 `modules/alias/`
  - 更新包名：`org.tsl.tSLplugins.modules.alias`
  - 更新 AliasModule.kt 直接使用本地类
  - 删除旧目录
  - 验证构建

- [x] A2.2 迁移 Title 模块
  - 将 `Title/` 下 3 个文件移入 `modules/title/`
  - 更新包名和导入
  - 更新 TitleModule.kt
  - 删除旧目录
  - 验证构建

- [x] A2.3 迁移 Advancement 模块
  - 将 `Advancement/` 下 3 个文件移入 `modules/advancement/`
  - 更新包名和导入
  - 更新 AdvancementModule.kt
  - 删除旧目录
  - 验证构建

### A3. 批次 3 - 复杂模块（4-5 文件）

- [x] A3.1 迁移 Neko 模块
  - 将 `Neko/` 下 4 个文件移入 `modules/neko/`
  - 更新包名和导入
  - 更新 NekoModule.kt
  - 删除旧目录
  - 验证构建

- [x] A3.2 迁移 Vault 模块
  - 将 `Vault/` 下 3 个文件移入 `modules/vault/`
  - 更新包名和导入
  - 更新 VaultModule.kt
  - 删除旧目录
  - 验证构建

- [x] A3.3 迁移 TimedAttribute 模块
  - 将 `TimedAttribute/` 下 5 个文件移入 `modules/timedattribute/`
  - 更新包名和导入
  - 更新 TimedAttributeModule.kt
  - 删除旧目录
  - 验证构建

- [x] A3.4 迁移 Maintenance 模块
  - 将 `Maintenance/` 下 5 个文件移入 `modules/maintenance/`
  - 更新包名和导入
  - 更新 MaintenanceModule.kt
  - 删除旧目录
  - 验证构建

### A4. 批次 4 - 高复杂模块（5+ 文件）

- [x] A4.1 迁移 TownPHome 模块
  - 将 `TownPHome/` 下 5 个文件移入 `modules/townphome/`
  - 更新包名和导入
  - 更新 TownPHomeModule.kt
  - 删除旧目录
  - 验证构建

- [x] A4.2 迁移 Landmark 模块
  - 将 `Landmark/` 下 8 个文件移入 `modules/landmark/`
  - 更新包名和导入
  - 更新 LandmarkModule.kt
  - 删除旧目录
  - 验证构建

- [x] A4.3 迁移 WebBridge 模块
  - 将 `WebBridge/` 下所有文件移入 `modules/webbridge/`
  - 更新包名和导入（注意：文件较多，约 14 个）
  - 更新 WebBridgeModule.kt
  - 删除旧目录
  - 验证构建

- [x] A4.4 迁移 Mcedia 模块
  - 将 `Mcedia/` 下所有文件移入 `modules/mcedia/`
  - 更新包名和导入（约 7 个文件）
  - 更新 McediaModule.kt
  - 删除旧目录
  - 验证构建

### A5. 最终验证

- [x] A5.1 完整构建验证
  - 运行 `./gradlew clean shadowJar`
  - 确认无编译错误

- [x] A5.2 更新文档
  - 更新 ARCHITECTURE_OPTIMIZATION.md 状态
  - 记录最终代码统计：139 文件，26,256 行代码
  - 全部 15 个包装器模块已完成迁移

---

## 阶段 B：服务层抽象 ✅

- [x] B1. 创建 service/ 目录结构
- [x] B2. 迁移 MessageManager
- [x] B3. 迁移 DatabaseManager
- [x] B4. 迁移 PlayerDataManager
  - 同时迁移 TSLPlayerProfile 和 TSLPlayerProfileStore
- [x] B5. 更新所有引用（10 个文件）
  - TSLplugins.kt, TSLPlaceholderExpansion.kt
  - ModuleContext.kt, ModuleRegistry.kt
  - PeaceModule.kt, PlayTimeModule.kt, PhantomModule.kt
  - McediaStorage.kt, QQBindManager.kt, TimedEffectStorage.kt

---

## 阶段 C：代码规范统一 ✅

- [x] C1. 评估包名重命名影响
  - 影响：139 个文件的 package 声明 + 192 个 import 语句 + plugin.yml
  - 结论：风险较高，建议保持当前包名 `org.tsl.tSLplugins`
- [x] C2. 统一包名为小写（已评估，暂不执行）
- [x] C3. 清理遗留文件
  - 无临时文件 (*.bak, *.tmp)
  - 无空目录
- [x] C4. 修复关键警告
  - 移除 9 个不必要的类型转换 (No cast needed)
  - 警告数：54 → 39
- [x] C5. 最终构建验证通过

---

## 工作量估计

| 批次 | 模块数 | 文件数 | 复杂度 |
|------|--------|--------|--------|
| A1 | 4 | ~5 | 简单 |
| A2 | 3 | ~9 | 中等 |
| A3 | 4 | ~15 | 复杂 |
| A4 | 4 | ~34 | 高复杂 |
| **总计** | **15** | **~63** | - |
