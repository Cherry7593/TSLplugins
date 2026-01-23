# Tasks

## 1. 绑定状态存储（使用现有 TSLPlayerProfile）

- [x] 1.1 在 `TSLPlayerProfile` 添加 `bindStatus` 和 `bindQQ` 字段
- [x] 1.2 在 `TSLPlayerProfileStore` 添加字段的读写支持

## 2. 绑定状态更新

- [x] 2.1 在 `QQBindManager` 添加 `playerDataManager` 引用
- [x] 2.2 在绑定成功时更新 profile 缓存
- [x] 2.3 在解绑成功时清除 profile 缓存
- [x] 2.4 在 `TSLplugins` 中注入 `playerDataManager` 到 `QQBindManager`

## 3. PAPI 变量

- [x] 3.1 在 `TSLPlaceholderExpansion` 添加 `bind` 变量处理
- [x] 3.2 在 `TSLPlaceholderExpansion` 添加 `bind_qq` 变量处理
- [x] 3.3 更新构造函数注入 `playerDataManager`
- [x] 3.4 在 `TSLplugins` 中传递 `playerDataManager` 到扩展

## 4. 验证

- [x] 4.1 测试绑定后变量返回正确值
- [x] 4.2 测试解绑后变量返回正确值
- [x] 4.3 测试离线玩家变量查询（从 YML 读取）
