# IDE 同步 Gradle 依赖指南

## 问题现象
IDE 显示 33 个错误，所有错误都是 `Unresolved reference 'comphenix'`，表示 ProtocolLib 依赖没有被 IDE 识别。

## 解决方案

### 方法 1：手动触发 Gradle 同步（推荐）

1. **在 IntelliJ IDEA 中打开 Gradle 面板**：
   - 点击右侧边栏的 **Gradle** 图标
   - 或者通过菜单：`View` → `Tool Windows` → `Gradle`

2. **刷新 Gradle 项目**：
   - 点击 Gradle 面板左上角的 **刷新** 图标（圆形箭头）
   - 或者右键点击项目名称 → `Reload Gradle Project`

3. **等待同步完成**：
   - IDE 会重新下载并索引所有依赖
   - 同步完成后，ProtocolLib 的类应该可以正确识别

### 方法 2：通过菜单同步

1. 打开菜单：`File` → `Reload All from Disk`
2. 或者：`File` → `Invalidate Caches / Restart...` → `Invalidate and Restart`

### 方法 3：命令行验证（已完成）

Gradle 命令行已经确认依赖正确下载：
```bash
./gradlew dependencies --configuration compileClasspath
# 输出包含：com.comphenix.protocol:ProtocolLib:5.3.0 ✓
```

这说明问题只是 IDE 没有同步，不是依赖本身的问题。

## 验证依赖是否正确

### 1. 检查 build.gradle.kts
```kotlin
repositories {
    maven("https://repo.dmulloy2.net/repository/public/") // ✓
}

dependencies {
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0") // ✓
}
```

### 2. 检查 plugin.yml
```yaml
depend: [ ProtocolLib ] # ✓
```

### 3. Gradle 依赖树确认
```
+--- com.comphenix.protocol:ProtocolLib:5.3.0 ✓
```

## 如果同步后仍然有错误

### 检查 Gradle JVM 版本
1. 打开 `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
2. 确保 `Gradle JVM` 设置为 **Java 21**

### 重建项目
1. 打开菜单：`Build` → `Rebuild Project`
2. 等待重建完成

### 清理 Gradle 缓存
```bash
./gradlew clean build --refresh-dependencies
```

## 为什么会出现这个问题？

1. **添加新依赖后** IDE 需要重新索引
2. **Gradle 已经下载了依赖**，但 IDE 的索引还没更新
3. **手动触发同步** ���以强制 IDE 重新扫描依赖

## 快捷键

- **刷新 Gradle**：`Ctrl + Shift + O`（Windows/Linux）或 `Cmd + Shift + O`（macOS）
- **同步项目**：点击通知栏的 "Load Gradle Changes" 按钮

## 预期结果

同步完成后：
- ✅ 所有 `com.comphenix.protocol.*` 的导入变为绿色
- ✅ 错误数量从 33 个降为 0 个
- ✅ 代码高亮正常
- ✅ 可以正常跳转到 ProtocolLib 的类

## 构建确认

即使 IDE 显示错误，Gradle 命令行构建是成功的：
```bash
./gradlew clean shadowJar
# BUILD SUCCESSFUL
```

这进一步证明问题只是 IDE 同步，不是代码本身的问题。

