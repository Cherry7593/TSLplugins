# Tasks: 修复地标导航指南针功能未生效

## 1. 修改 TSLplugins.kt
- [x] 1.1 添加 `landmarkCompass: LandmarkCompass` 成员变量声明
- [x] 1.2 添加 `LandmarkCompass` import
- [x] 1.3 在地标系统初始化区块中创建 `LandmarkCompass` 实例
- [x] 1.4 将 `landmarkCompass` 传递给 `LandmarkListener` 构造函数
- [x] 1.5 将 `landmarkCompass` 传递给 `LandmarkCommand` 构造函数

## 2. 修改 LandmarkListener.kt
- [x] 2.1 构造函数添加 `compass: LandmarkCompass` 参数
- [x] 2.2 添加必要的 import（`PlayerInteractEvent`, `PlayerItemHeldEvent`, `Action`）
- [x] 2.3 添加 `PlayerInteractEvent` 监听器处理指南针左右键切换
- [x] 2.4 添加 `PlayerItemHeldEvent` 监听器处理主手物品切换
- [x] 2.5 修改 `onPlayerQuit` 添加指南针数据清理
- [x] 2.6 修改 `onPlayerMove` 在进入地标时调用 `compass.handleArrival()`

## 3. 修改 LandmarkCommand.kt
- [x] 3.1 构造函数添加 `compass: LandmarkCompass` 参数
- [x] 3.2 添加 `handleCompass` 方法实现 `/lm compass` 命令
- [x] 3.3 更新 `showHelp` 添加指南针帮助文本
- [x] 3.4 更新 `tabComplete` 添加指南针命令补全

## 4. 验证
- [x] 4.1 编译测试通过
- [ ] 4.2 测试指南针获取（/tsl landmark compass）
- [ ] 4.3 测试指南针切换功能（左右键）
- [ ] 4.4 测试粒子引导线显示
