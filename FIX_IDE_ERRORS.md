# 解决 IDE 错误的步骤

## 当前状态

✅ **Gradle 构建成功** - 依赖已正确下载
❌ **IDE 显示 33 个错误** - IDE 没有同步依赖

## 立即执行的步骤

### 步骤 1：刷新 Gradle（必须）

在 IntelliJ IDEA 中：

1. 打开右侧的 **Gradle** 面板
2. 点击左上角的 **🔄 刷新** 图标
3. 等待同步完成（可能需要 1-2 分钟）

**快捷方式**：
- 按下 `Ctrl + Shift + O`（Windows/Linux）
- 或 `Cmd + Shift + O`（macOS）

### 步骤 2：验证同步结果

同步完成后检查：
- ✅ `com.comphenix.protocol` 的导入不再显示红色
- ✅ 错误数量变为 0
- ✅ 代码高亮正常

### 步骤 3：如果仍有错误

如果刷新后仍有错误，尝试：

**方法 A：重建项目**
```
菜单：Build → Rebuild Project
```

**方法 B：清理缓存**
```
菜单：File → Invalidate Caches / Restart... → Invalidate and Restart
```

## 为什么会出现这个问题？

你刚刚添加了新的依赖（ProtocolLib）到 `build.gradle.kts`：
```kotlin
maven("https://repo.dmulloy2.net/repository/public/")
compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
```

IDE 需要重新同步 Gradle 才能识别这个新依赖。

## 技术验证

### ✅ Gradle 命令行确认
```bash
$ ./gradlew dependencies --configuration compileClasspath
+--- com.comphenix.protocol:ProtocolLib:5.3.0  # ✓ 已下载
```

### ✅ 构建成功
```bash
$ ./gradlew shadowJar
BUILD SUCCESSFUL
```

### ✅ 依赖文件正确
- `build.gradle.kts` ✓
- `plugin.yml` ✓
- `BossvoiceListener.kt` ✓

**结论**：代码没有问题，只需要 IDE 同步！

## 同步后的预期结果

```kotlin
import com.comphenix.protocol.PacketType        // ✅ 绿色，无错误
import com.comphenix.protocol.ProtocolLibrary   // ✅ 绿色，无错误
import com.comphenix.protocol.events.*          // ✅ 绿色，无错误
```

所有 33 个错误都会消失！

## 需要帮助？

如果刷新后仍有问题，请检查：
1. Gradle JVM 是否设置为 Java 21
2. 网络连接是否正常（需要下载依赖）
3. Gradle 缓存是否需要清理

---

**立即行动**：现在就在 IntelliJ IDEA 中刷新 Gradle！🔄

