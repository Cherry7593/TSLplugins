# 🔍 Ride & Toss 黑名单问题 - 快速诊断卡片

## ⚡ 快速测试（5分钟）

### 1️⃣ 检查启动日志
```bash
grep "\[Ride\]" logs/latest.log
grep "\[Toss\]" logs/latest.log
```

**预期输出**:
```
[Ride] 配置文件中的黑名单字符串: [WITHER, ENDER_DRAGON, WARDEN, GHAST, ELDER_GUARDIAN]
[Ride] 成功添加黑名单: WITHER -> WITHER
[Ride] 已加载配置 - 默认状态: 启用, 黑名单: 5 种生物
```

### 2️⃣ 游戏内测试
1. 尝试骑乘/举起凋零（WITHER）
2. 立即查看日志

**预期输出**:
```
[Ride] 黑名单检查: WITHER -> 已禁止 (黑名单: [WITHER, ...])
```

### 3️⃣ 检查玩家权限
```bash
/lp user <玩家名> permission check tsl.ride.bypass
/lp user <玩家名> permission check tsl.toss.bypass
```

**预期结果**: 两个都应该返回 `false`

---

## 🎯 问题定位矩阵

| 日志现象 | 问题原因 | 解决方法 |
|---------|---------|---------|
| 黑名单字符串: `[]` | 配置未读取 | 检查 config.yml 格式 |
| "无效的实体类型" | 名称错误 | 使用正确的 EntityType 名称 |
| "已禁止" 但仍能操作 | 有 bypass 权限 | 移除 bypass 权限 |
| 无任何检查日志 | 监听器未触发 | 检查功能是否启用 + 权限 |

---

## 🔧 常用命令

```bash
# 重载配置
/tsl reload

# 检查权限
/lp user <玩家> permission check tsl.ride.bypass

# 移除权限
/lp user <玩家> permission unset tsl.ride.bypass

# 查看日志（实时）
tail -f logs/latest.log | grep "\[Ride\]\|\[Toss\]"
```

---

## 📋 正确的配置格式

```yaml
ride:
  enabled: true
  blacklist:
    - WITHER           # ✅ 正确
    - ENDER_DRAGON     # ✅ 正确（带下划线）
    - WARDEN           # ✅ 正确

toss:
  enabled: true
  blacklist:
    - WITHER
    - ENDER_DRAGON
    - WARDEN
```

### ❌ 常见错误

```yaml
# 错误 1: 缩进不对
ride:
enabled: true          # ❌ 应该缩进 2 空格

# 错误 2: 拼写错误
blacklist:
  - ENDERDRAGON        # ❌ 应该是 ENDER_DRAGON

# 错误 3: 使用中文空格
blacklist:
  -　WITHER            # ❌ 使用了中文空格
```

---

## 📞 支持

- 详细文档: `archive/SUMMARY_Blacklist_Debug.md`
- 完整总结: `archive/SUMMARY_Ride_Toss_Complete.md`

---

**提示**: 所有调试日志会在每次操作时输出，可能产生大量日志。问题解决后考虑移除或条件化。

