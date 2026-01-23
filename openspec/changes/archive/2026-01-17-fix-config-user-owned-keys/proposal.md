# Change: 修复配置更新时用户自定义映射被覆盖的问题

## Why

`ConfigUpdateManager` 在配置版本更新时，会将用户自定义的 `random-variable.variables` 和 `papi-alias.mappings` 配置覆盖为默认值（空或示例数据），导致用户配置丢失。

## What Changes

- **BUG FIX**: 将 `random-variable.variables` 和 `papi-alias.mappings` 添加到 `USER_OWNED_KEYS` 集合
- 确保这两个用户专属配置在版本更新时被完整保留

## Impact

- Affected code: `ConfigUpdateManager.kt:27-29`
- Affected specs: `randomvariable`, `papialias`
- 无破坏性变更，仅修复现有行为

## Root Cause

`USER_OWNED_KEYS` 集合仅包含 `permission-checker.rules`，缺少其他用户自定义配置路径：

```kotlin
private val USER_OWNED_KEYS = setOf(
    "permission-checker.rules"  // ✓ 已有
    // 缺少: "random-variable.variables"
    // 缺少: "papi-alias.mappings"
)
```

当 `checkAndUpdate()` 检测到版本不匹配时，`mergeConfigWithComments()` 会用默认配置覆盖不在 `USER_OWNED_KEYS` 中的用户配置。
