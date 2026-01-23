# 地标系统 (Landmark System)

地标系统提供服务器级交通网络，玩家通过探索解锁地标，并在地标区域内传送到已解锁的目标地标。

## 功能概述

- **地标管理**: 管理员创建/编辑/删除地标，设置 AABB 区域范围
- **区域检测**: 自动检测玩家进入地标区域并发送提示
- **解锁系统**: 首次进入地标自动解锁，支持默认解锁地标
- **GUI 菜单**: 可视化地标列表，显示解锁状态，点击传送
- **传送系统**: 限制只能在地标区域内发起传送，目标必须已解锁
- **维护者权限**: 管理员可授权玩家编辑指定地标的部分属性

## 命令

主命令为 `/tsl landmark`，可通过内置别名 `/lm` 简写使用。

### 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/tsl landmark` | `tsl.landmark.use` | 打开地标菜单 |
| `/tsl landmark gui` | `tsl.landmark.use` | 打开地标菜单 |
| `/tsl landmark list` | `tsl.landmark.use` | 列出所有地标 |
| `/tsl landmark tp <名称>` | `tsl.landmark.tp` | 传送到地标 |
| `/tsl landmark info <名称>` | `tsl.landmark.use` | 查看地标信息 |

### 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/tsl landmark create <名称>` | `tsl.landmark.admin` | 开始创建地标 |
| `/tsl landmark setpos1` | `tsl.landmark.admin` | 设置区域点1 |
| `/tsl landmark setpos2` | `tsl.landmark.admin` | 设置区域点2 |
| `/tsl landmark finalize` | `tsl.landmark.admin` | 完成地标创建 |
| `/tsl landmark cancel` | `tsl.landmark.admin` | 取消创建 |
| `/tsl landmark delete <名称> confirm` | `tsl.landmark.admin` | 删除地标 |
| `/tsl landmark edit <名称> [字段] [值]` | `tsl.landmark.admin` | 编辑地标属性 |
| `/tsl landmark setwarp <名称>` | `tsl.landmark.admin` | 设置传送点 |
| `/tsl landmark trust <地标> <玩家>` | `tsl.landmark.admin` | 添加维护者 |
| `/tsl landmark untrust <地标> <玩家>` | `tsl.landmark.admin` | 移除维护者 |

### 命令别名

在 `aliases.yml` 中已内置以下简写：
- `/lm` → `/tsl landmark`
- `/landmark` → `/tsl landmark`

## 权限

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `tsl.landmark.use` | true | 使用地标 GUI |
| `tsl.landmark.tp` | true | 传送到地标 |
| `tsl.landmark.admin` | op | 管理员权限 |

## 配置

```yaml
landmark:
  enabled: false                          # 是否启用
  max-region-volume: 1000000             # 区域最大体积
  enter-message-cooldown-seconds: 10     # 进入提示冷却（秒）
  show-coords-in-gui: false              # GUI 中显示坐标
  teleport:
    require-inside-landmark: true        # 必须在地标内才能传送
    safe-landing: true                   # 安全落点检测
    cast-time-seconds: 0                 # 吟唱时间（0=即时传送）
    cancel-on-move: true                 # 移动取消传送
    cooldown-seconds: 0                  # 传送冷却时间
```

## 数据存储

- `plugins/TSLplugins/landmarks.json` - 地标数据
- `plugins/TSLplugins/landmark-unlocks.json` - 玩家解锁记录

## 使用流程

### 创建地标

1. 执行 `/lm create 主城` 开始创建
2. 走到区域一角，执行 `/lm setpos1`
3. 走到区域对角，执行 `/lm setpos2`
4. 执行 `/lm finalize` 完成创建

### 玩家使用

1. 进入地标区域会自动解锁并显示提示
2. 在任意地标区域内执行 `/lm` 打开菜单
3. 点击已解锁的地标即可传送

## 验收测试步骤

1. **创建地标**: 管理员创建带 Region 的地标，重启后仍存在
2. **进入检测**: 玩家首次进入 Region 收到提示并解锁；区域内移动不重复刷提示
3. **GUI 展示**: GUI 可展示所有地标，未解锁地标不可传送
4. **传送限制**: 玩家只能在地标 Region 内发起传送，且只能传送到已解锁地标
5. **维护者权限**: 维护者只能编辑被授权地标的 lore/icon/落点，无法删除
6. **删除确认**: 管理员删除地标需确认，删除后 GUI 不再显示

## 模块结构

```
Landmark/
├── LandmarkData.kt      # 数据类定义
├── LandmarkStorage.kt   # JSON 持久化
├── LandmarkManager.kt   # 核心管理类
├── LandmarkCommand.kt   # 命令处理
├── LandmarkListener.kt  # 事件监听
└── LandmarkGUI.kt       # GUI 菜单
```
