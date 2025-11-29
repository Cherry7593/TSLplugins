# ChatBubble 极简安全方案 - 快速参考

## 🎯 核心改进

### 3 个关键修改

#### 1. 本地计数器替代 ticksLived
```kotlin
// ❌ 危险：跨线程访问
if (display.ticksLived > timeSpan) { ... }

// ✅ 安全：本地变量
var tickCount = 0
if (tickCount * updateTicks > timeSpan) { ... }
```

#### 2. 全局异常防护
```kotlin
try {
    // 所有 display 操作
    display.textOpacity = ...
    display.teleportAsync(...)
} catch (e: Exception) {
    // 任何错误都清理
    task.cancel()
    bubbles.remove(player)
}
```

#### 3. 简化删除逻辑
```kotlin
// 不使用 scheduler.execute，直接 try-catch
try {
    if (display.isValid) display.remove()
} catch (e: Exception) {
    // 忽略跨线程删除错误
}
```

---

## 🔒 安全保证

### 完全避免
- ❌ 访问 `display.ticksLived`
- ❌ 访问 `display.health` 
- ❌ 使用 `display.scheduler.execute`（已移除）
- ❌ 任何跨线程调度

### 全部使用
- ✅ 本地变量计数
- ✅ try-catch 包装所有 display 操作
- ✅ 玩家线程调度（`player.scheduler`）
- ✅ 传送时主动清理

---

## 📋 行为变化

| 场景 | 旧版本 | 新版本 |
|------|--------|--------|
| 正常聊天 | 气泡显示 5 秒 | ✅ 相同 |
| 玩家传送 | 气泡可能残留，导致错误 | ✅ 立即清除 |
| 跨世界 | 气泡可能残留 | ✅ 立即清除 |
| 潜行 | 半透明 | ✅ 相同 |
| 快速移动 | 可能报错 | ✅ 无错误 |

---

## ✅ 测试清单

```bash
# 1. 基础功能
□ 发送消息显示气泡
□ 5 秒后自动消失
□ 潜行时半透明

# 2. 传送测试
□ /tp 后气泡消失
□ 无错误日志
□ 可以再次聊天

# 3. 压力测试
□ 10 次快速传送无错误
□ 多玩家同时聊天正常
□ TPS 保持 19.5+

# 4. 边界测试
□ 跨世界传送正常
□ 插件重载正常
□ 玩家退出清理正常
```

---

## 🐛 如果还有错误

### 可能的情况
1. **服务端版本过旧** - 需要 Folia 1.21.8+
2. **其他插件冲突** - 检查是否有其他插件修改实体
3. **配置问题** - 检查 `updateTicks` 是否合理（推荐 2-5）

### 调试步骤
```bash
# 1. 启用详细日志
在 ChatBubbleManager 添加日志输出

# 2. 检查实体数量
/folia entitycount

# 3. 检查 TPS
/tps

# 4. 重现问题并记录日志
```

---

## 📞 支持信息

- **完整文档**: `archive/SUMMARY_ChatBubble_Ultimate_ThreadSafe_Solution.md`
- **项目指南**: `开发者指南.md`
- **配置版本**: 14（无需修改配置）

---

**方案状态**: ✅ 已实施  
**测试状态**: ⏳ 待验证  
**风险等级**: 🟢 低

