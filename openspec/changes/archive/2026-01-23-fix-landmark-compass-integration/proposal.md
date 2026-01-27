# Change: 修复地标导航指南针功能未生效

## Why
地标指南针功能的代码已经编写完成（`LandmarkCompass.kt` 存在且完整），但该功能在实际运行中不生效。原因是指南针相关代码未被整合到主插件初始化流程和事件监听器中。

具体问题：
1. `TSLplugins.kt` 未创建 `LandmarkCompass` 实例
2. `LandmarkListener.kt` 构造函数缺少 `compass` 参数，未添加指南针交互监听
3. `LandmarkCommand.kt` 构造函数缺少 `compass` 参数，无法执行指南针命令

## What Changes
- 修改 `TSLplugins.kt`：添加 `landmarkCompass` 成员变量声明和初始化
- 修改 `LandmarkListener.kt`：
  - 构造函数添加 `compass: LandmarkCompass` 参数
  - 添加 `PlayerInteractEvent` 监听指南针左右键操作
  - 添加 `PlayerItemHeldEvent` 监听主手切换以启停粒子
  - 修改 `onPlayerQuit` 清理指南针数据
  - 修改 `onPlayerMove` 在进入地标时触发到达检测
- 修改 `LandmarkCommand.kt`：
  - 构造函数添加 `compass: LandmarkCompass` 参数
  - 确保 `/lm compass` 子命令可以正常工作

## Impact
- Affected specs: `landmark`
- Affected code:
  - `TSLplugins.kt` - 添加指南针初始化
  - `Landmark/LandmarkListener.kt` - 添加事件监听
  - `Landmark/LandmarkCommand.kt` - 添加指南针命令参数
