# Tasks: Fix Module Bugs Batch 1

## 实施任务

### Phase 1: 幻翼控制模块

- [x] T1. PhantomModule 热重载修复 ✅
  - 在 `doEnable()` 末尾延迟调用 `processAllPlayers()`
  - 确保热重载后立即重置禁用幻翼玩家的统计

### Phase 2: phome 模块

- [x] T2. 重构命令结构 ✅
  - `/phome` 无参数时直接调用 `gui.open(sender)`
  - `/phome <名称>` 直接传送（非保留字时）
  - 移除 `list` 和 `tp` 作为命令

- [x] T3. 禁止保留字作为家名称 ✅
  - 定义保留字列表：`set`, `del`, `delete`, `remove`, `gui`, `help`, `list`, `tp`
  - 在 `handleSet()` 中检查并拒绝

- [x] T4. 正确解析十六进制颜色代码 ✅
  - 添加 `parseHexColors()` 方法，使用 `LegacyComponentSerializer.builder().hexColors()`
  - 在 GUI 中正确显示 `#xxxxxx` 和 `&#xxxxxx` 格式颜色

- [x] T5. 设置/删除 phome 时通知成员 ✅
  - 在 `TownPHomeManager` 中添加 `notifyTownMembers()` 方法
  - 通知同一小镇的所有在线玩家（排除操作者）

### Phase 3: 计时属性模块

- [x] T6. 添加命令别名 attr ✅
  - 重写 `getAdditionalCommandHandlers()` 返回 `"attr" to TimedAttributeCommand(manager)`

### Phase 4: 验证

- [x] T7. 构建验证 ✅
  - 运行 `./gradlew classes` 通过
