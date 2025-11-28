# 访客权限系统更新说明

## 更新时间
2025年11月26日

## 🔄 权限系统变更

### 旧系统（已移除）❌
```yaml
# plugin.yml
permissions:
  tsl.visitor:
    description: 访客权限
    default: false
```

**工作方式**：
- 使用单一权限节点 `tsl.visitor`
- 需要手动给每个玩家分配权限
- 使用 `player.hasPermission("tsl.visitor")` 检测

---

### 新系统（当前）✅
```yaml
# plugin.yml
permissions:
  tsl.visitor.admin:
    description: 访客模式管理权限
    default: op

# config.yml
visitor:
  groups:
    - "visitor"
    - "guest"
```

**工作方式**：
- 基于 LuckPerms 权限组检测
- 配置访客权限组列表
- 自动检测玩家主权限组
- 支持多个访客权限组

---

## 📋 移除的内容

### 1. plugin.yml
- ❌ 移除：`tsl.visitor` 权限节点
- ✅ 新增：`tsl.visitor.admin` 管理权限

### 2. 代码
- ❌ 移除：所有 `hasPermission("tsl.visitor")` 检查
- ✅ 改为：`checkVisitorByGroup()` 权限组检测

### 3. 文档
- ✅ 更新：`WIKI.md` - 访客模式章节
- ✅ 更新：`plugin.yml` - 权限定义
- ℹ️ 保留：归档文档（作为历史记录）

---

## 🎯 迁移指南

### 如果你之前使用了 `tsl.visitor` 权限

#### 步骤 1：创建访客权限组
```bash
/lp creategroup visitor
/lp group visitor setweight 1
```

#### 步骤 2：迁移玩家
```bash
# 查看拥有 tsl.visitor 权限的玩家
/lp search permission tsl.visitor

# 将玩家移动到权限组
/lp user <玩家名> parent set visitor

# 移除旧权限（可选，不影响功能）
/lp user <玩家名> permission unset tsl.visitor
```

#### 步骤 3：配置 config.yml
```yaml
visitor:
  groups:
    - "visitor"
```

#### 步骤 4：重载插件
```bash
/tsl reload
```

---

## ✨ 新系统优势

### 1. 更符合 LuckPerms 设计理念
- 直接使用权限组管理
- 无需单独配置权限节点
- 更易于管理和扩展

### 2. 更灵活
- 支持多个访客权限组
- 可以通过权限组优先级控制
- 支持权限组继承

### 3. 更强大
- 新增 6 大限制功能
- 管理命令系统
- 消息冷却机制

---

## 🔍 兼容性说明

### 向后兼容
- ✅ 旧配置会自动更新到 v13
- ✅ 旧的通知配置完全保留
- ⚠️ 需要手动迁移玩家到权限组

### 不兼容项
- ❌ `tsl.visitor` 权限不再生效
- ❌ 需要使用权限组代替

---

## 📚 相关权限

### 保留的权限
- `tsl.visitor.admin` - 访客管理权限（新增）
  - 允许使用 `/tsl visitor` 命令
  - 默认：OP

### 移除的权限
- `tsl.visitor` - 旧的访客权限（已移除）

---

## 💡 使用示例

### 示例 1：设置默认玩家为访客
```bash
# 创建访客组
/lp creategroup visitor

# 设置默认组继承访客组
/lp group default parent add visitor
```

### 示例 2：单独设置玩家为访客
```bash
# 方法1：通过权限组（推荐）
/lp user Steve parent set visitor

# 方法2：通过管理命令（临时）
/tsl visitor set Steve
```

### 示例 3：检查玩家状态
```bash
# 查看玩家权限组
/lp user Steve info

# 查看访客状态
/tsl visitor check Steve
```

---

## 🐛 故障排除

### 问题：玩家没有成为访客
**检查**：
1. 玩家的主权限组是否在 `visitor.groups` 列表中
2. 配置文件是否正确加载（`/tsl reload`）
3. LuckPerms 是否正确安装

### 问题：旧权限还在起作用
**解决**：
- 旧权限 `tsl.visitor` 已完全移除，不会起作用
- 如果看到效果，说明玩家在访客权限组中

---

## 📝 更新日志

### v13 (2025-11-26)
- ❌ 移除 `tsl.visitor` 权限节点
- ✅ 新增 `tsl.visitor.admin` 管理权限
- ✅ 改为基于权限组的检测系统
- ✅ 新增 6 大限制功能
- ✅ 新增管理命令系统
- ✅ 新增消息冷却机制

---

## 🎉 总结

**旧系统**：手动分配 `tsl.visitor` 权限  
**新系统**：基于 LuckPerms 权限组自动检测

**迁移工作量**：低（只需创建权限组并移动玩家）  
**功能增强**：高（新增限制、命令、优化等）

**推荐**：立即迁移到新系统以享受更好的功能和管理体验！

---

**文档版本**：v13  
**最后更新**：2025-11-26

