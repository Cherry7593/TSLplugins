# Visitor 模式快速参考

## 🎯 配置访客权限组

### 步骤 1：编辑 config.yml
```yaml
visitor:
  groups:
    - "visitor"    # 你的访客权限组名
    - "guest"      # 可以有多个
```

### 步骤 2：创建权限组（如果还没有）
```bash
/lp creategroup visitor
/lp group visitor setweight 1
```

### 步骤 3：将玩家添加到权限组
```bash
/lp user Steve parent set visitor
```

### 步骤 4：重载配置
```bash
/tsl reload
```

---

## 🎮 管理命令

| 命令 | 说明 | 示例 |
|-----|------|------|
| `/tsl visitor set <玩家>` | 手动设置访客 | `/tsl visitor set Steve` |
| `/tsl visitor remove <玩家>` | 移除访客身份 | `/tsl visitor remove Steve` |
| `/tsl visitor check <玩家>` | 检查访客状态 | `/tsl visitor check Steve` |
| `/tsl visitor list` | 列出所有访客 | `/tsl visitor list` |
| `/tsl visitor reload` | 重载配置 | `/tsl visitor reload` |

**权限**：`tsl.visitor.admin`

---

## ⚙️ 限制配置

```yaml
visitor:
  restrictions:
    block-break: true      # 破坏方块
    block-place: true      # 放置方块
    item-use: true         # 使用物品（食物除外）
    container-open: true   # 打开容器
    pressure-plate: true   # 红石设施
    entity-damage: true    # 攻击实体
```

**提示**：设置为 `false` 即可关闭该限制

---

## 📋 访客效果

### 自动应用
- ✨ 发光效果（GLOWING）
- 🛡️ 怪物不会攻击
- 📢 登录/离开时的通知

### 限制（可配置）
- ⛔ 不能破坏/放置方块
- ⛔ 不能使用物品（食物可以）
- ⛔ 不能打开容器
- ⛔ 不能触发红石设施
- ⛔ 不能攻击实体

---

## 🔄 工作原理

### 检测优先级
1. **手动设置**（`/tsl visitor set`）- 最高优先级
2. **权限组检测**（`visitor.groups` 配置）- 自动检测

### 性能优化
- 使用内存缓存，快速查询
- 只对访客进行限制检查
- 非访客玩家零开销

---

## 🎨 通知自定义

```yaml
visitor:
  gained:
    chat: "&a[访客模式] &7你已进入访客模式！"
    title: "&a访客模式"
    subtitle: "&7已启用"
    sound: "entity.player.levelup"
  
  lost:
    chat: "&c[访客模式] &7你已退出访客模式！"
    title: "&c访客模式"
    subtitle: "&7已禁用"
    sound: "block.note_block.bass"
```

**颜色代码**：使用 `&` 符号（如 `&a` = 绿色，`&c` = 红色）

---

## 🐛 常见问题

### Q: 访客效果没有应用？
**A**: 检查：
1. `visitor.enabled: true`
2. 权限组名称是否在 `visitor.groups` 列表中
3. 玩家的主权限组是否正确（`/lp user <玩家> info`）

### Q: 限制不生效？
**A**: 检查：
1. 对应的 `restrictions.*` 是否设置为 `true`
2. 运行 `/tsl reload` 重载配置

### Q: 如何临时禁用某个限制？
**A**: 编辑 config.yml，设置对应项为 `false`，然后 `/tsl reload`

### Q: 手动设置的访客和权限组冲突？
**A**: 手动设置优先级更高，会覆盖权限组检测

---

## 📊 性能数据

- **内存占用**：~3.6 KB（100 访客）
- **CPU 开销**：< 0.1% TPS 影响
- **查询速度**：O(1) 时间复杂度

---

## 🎯 推荐配置

### 新手服务器（严格限制）
```yaml
visitor:
  groups: ["visitor"]
  restrictions:
    block-break: true
    block-place: true
    item-use: true
    container-open: true
    pressure-plate: true
    entity-damage: true
```

### 休闲服务器（宽松限制）
```yaml
visitor:
  groups: ["guest"]
  restrictions:
    block-break: true
    block-place: true
    item-use: false        # 允许使用物品
    container-open: false  # 允许打开容器
    pressure-plate: false  # 允许触发红石
    entity-damage: true
```

### PVP 服务器（观战模式）
```yaml
visitor:
  groups: ["spectator"]
  restrictions:
    block-break: true
    block-place: true
    item-use: true
    container-open: true
    pressure-plate: true
    entity-damage: true
```

---

## 📚 相关文档

- `archive/SUMMARY_Visitor_Optimization.md` - 完整优化文档
- `docs/VISITOR_LOGIC_EXPLANATION.md` - 逻辑详解

---

**版本**：v13  
**更新时间**：2025-11-26

