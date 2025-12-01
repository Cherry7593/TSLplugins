# 玩家配置 YAML 存储 - 快速参考

## 🎯 核心变化

**旧方式**: PDC 存储（玩家 dat 文件内）  
**新方式**: YAML 存储（`playerdata/<uuid>.yml`）

---

## 📁 文件结构

```
plugins/TSLplugins/
  └── playerdata/
      ├── <uuid-1>.yml  # 玩家1的配置
      ├── <uuid-2>.yml  # 玩家2的配置
      └── <uuid-3>.yml  # 玩家3的配置
```

---

## 🔄 自动迁移流程

```
1. 玩家加入服务器
2. 检查是否已迁移（migratedFromPdc）
3. 如果未迁移 → 读取 PDC 数据 → 写入 YAML → 删除 PDC
4. 如果已迁移 → 直接使用 YAML 数据
```

---

## 💾 保存时机

| 事件 | 操作 | 说明 |
|------|------|------|
| 玩家加入 | 加载配置到内存 | 自动迁移 PDC |
| 玩家修改配置 | 修改内存中的 Profile | 不立即保存 |
| 玩家退出 | 保存到 YAML + 清除缓存 | 节省内存 |
| 插件重载 | 保存所有在线玩家 | 批量保存 |
| 插件关闭 | 保存所有在线玩家 | 批量保存 |

---

## 📊 配置文件格式

```yaml
playerName: "Notch"
kissEnabled: true
rideEnabled: false
tossEnabled: true
tossVelocity: 2.5
migratedFromPdc: true
lastSaved: 1732924800000
```

---

## 🔧 核心类

| 类 | 功能 |
|----|------|
| `TSLPlayerProfile` | 数据类，存储玩家配置 |
| `TSLPlayerProfileStore` | 存储管理器，负责读写 YAML |
| `PlayerDataManager` | 对外接口，保持兼容性 |

---

## 💡 使用方式（对其他模块）

### 无需修改代码！

```kotlin
// 读取配置（和之前一样）
val enabled = playerDataManager.getKissToggle(player)

// 修改配置（和之前一样）
playerDataManager.setKissToggle(player, true)

// 内部已自动切换到 YAML 存储
```

---

## ✅ 优势

- ✅ **可读性**: 文本格式，可直接编辑
- ✅ **可维护性**: 可离线修改配置
- ✅ **备份友好**: 独立文件，易于备份
- ✅ **调试友好**: 可直接查看配置内容
- ✅ **向后兼容**: 自动迁移 PDC 数据
- ✅ **性能优化**: 内存缓存 + 批量保存

---

## 🧪 测试要点

1. **新玩家**: 创建默认配置
2. **老玩家**: 自动迁移 PDC 数据
3. **配置修改**: 修改后保存正确
4. **重载/关闭**: 数据不丢失

---

## 📝 日志示例

```
[INFO] [ProfileStore] 创建玩家数据目录: .../playerdata
[INFO] [PlayerData] 已从 PDC 迁移玩家数据: Notch
[INFO] [ProfileStore] 加载玩家配置: Notch (...)
[INFO] [ProfileStore] 保存玩家配置: Notch (...)
[INFO] [ProfileStore] 开始保存 3 个玩家配置...
[INFO] [ProfileStore] 保存完成: 3/3 成功
```

---

**日期**: 2025-11-30  
**版本**: TSLplugins v1.0

