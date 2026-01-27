# Change: 继续架构优化 - 完全迁移包装器模块

## Why

当前架构优化已完成基础阶段（31 个模块完全迁移，核心框架就绪）。剩余 15 个包装器模块仍引用旧目录代码，造成：
- 代码分散在两处（`modules/xxx/` 和 `Xxx/`），维护成本高
- 包名不统一（`org.tsl.tSLplugins.Xxx` vs `org.tsl.tSLplugins.modules.xxx`）
- 新开发者理解成本增加

## What Changes

### 阶段 A：完全迁移包装器模块（当前阶段）
将 15 个包装器模块的旧代码完全移入 `modules/` 目录，按复杂度分批：

**批次 1 - 简单模块（1-2 文件）：**
- Permission → modules/permissionchecker/
- PlayerCountCmd → modules/playercountcmd/
- XconomyTrigger → modules/xconomytrigger/
- PlayerList → modules/playerlist/（新建）

**批次 2 - 中等模块（3 文件）：**
- Alias → modules/alias/
- Title → modules/title/
- Advancement → modules/advancement/

**批次 3 - 复杂模块（4-5 文件）：**
- Neko → modules/neko/
- Vault → modules/vault/
- TimedAttribute → modules/timedattribute/
- Maintenance → modules/maintenance/

**批次 4 - 高复杂模块（5+ 文件）：**
- TownPHome → modules/townphome/
- Landmark → modules/landmark/
- WebBridge → modules/webbridge/
- Mcedia → modules/mcedia/

### 阶段 B：服务层抽象（后续）
- 创建 `service/` 目录
- 迁移 MessageManager、DatabaseManager、PlayerDataManager

### 阶段 C：代码规范统一（后续）
- 包名统一为小写
- 清理遗留代码

## Impact

- **Affected directories**: 15 个旧模块目录将被删除
- **Affected files**: 约 50 个 Kotlin 文件需要重构
- **Breaking changes**: 无（内部重构，不影响 API）
- **Build verification**: 每批次完成后需验证构建

## Success Criteria

- [ ] 所有 15 个旧模块目录已删除
- [ ] `modules/` 包含所有 49+ 模块的完整代码
- [ ] 构建成功，无编译错误
- [ ] 功能测试通过
