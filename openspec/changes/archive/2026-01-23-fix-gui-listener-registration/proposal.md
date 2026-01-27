# Proposal: Fix GUI Listener Registration

## Summary

修复 TownPHomeGUI 和 McediaGUI 未注册为监听器导致事件处理不生效的问题。

## Problem

**根本原因**：GUI 类实现了 `Listener` 接口并定义了 `@EventHandler` 方法，但在模块的 `doEnable()` 中没有调用 `registerListener(gui)`。

**对比**：

| 模块 | 代码 | GUI 事件处理 |
|------|------|-------------|
| LandmarkModule | `registerListener(gui)` | ✅ 正常 |
| TownPHomeModule | 无注册 | ❌ 不工作 |
| McediaModule | 无注册 | ❌ 不工作 |

**影响**：
- `TownPHomeGUI.onInventoryClick()` 不会被调用
- `McediaGUI.onInventoryClick()` 不会被调用
- 物品可以从 GUI 中被拿走

## Solution

在模块的 `doEnable()` 中添加 `registerListener(gui)` 调用：

```kotlin
// TownPHomeModule.kt
override fun doEnable() {
    ...
    gui = TownPHomeGUI(javaPlugin, manager)
    registerListener(gui)  // 添加这行
}

// McediaModule.kt  
override fun doEnable() {
    ...
    gui = McediaGUI(javaPlugin, manager)
    registerListener(gui)  // 添加这行
    ...
}
```

## Scope

- **Files affected**: 
  - `TownPHomeModule.kt`
  - `McediaModule.kt`
- **Risk**: Very Low - 仅添加监听器注册
- **Breaking changes**: None
