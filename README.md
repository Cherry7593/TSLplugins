# TSLplugins

Paper/Folia 1.21.8 多功能整合插件，Kotlin 开发，原生支持 Folia 多线程。

## 环境要求

- Java 21+
- Paper 1.21.8+ 或 Folia 1.21.8+
- 可选: PlaceholderAPI, LuckPerms

## 安装

1. 下载 `TSLplugins-1.0.jar` 放入 `plugins/`
2. 启动服务器生成配置
3. 编辑 `config.yml` 启用所需功能
4. `/tsl reload` 应用配置

## 功能模块

### 玩家交互

| 模块 | 命令 | 说明 |
|------|------|------|
| Kiss | `/tsl kiss` / Shift+右键 | 亲吻玩家，粒子+音效 |
| Hat | `/tsl hat` | 戴帽子 |
| Scale | `/tsl scale <倍数>` | 调整体型 |
| Ping | `/tsl ping [all]` | 延迟查询/排行 |
| Toss | `/tsl toss` | 举起生物并投掷 |
| Ride | `/tsl ride` | 骑乘生物 |
| ChatBubble | - | 聊天气泡显示 |
| Speed | `/tsl speed <值>` | 调整移动速度 |

### 管理工具

| 模块 | 命令 | 说明 |
|------|------|------|
| Freeze | `/tsl freeze <玩家> [秒]` | 冻结玩家 |
| Maintenance | `/tsl maintenance` | 维护模式 |
| Spec | `/tsl spec` | 观察模式 |
| Patrol | `/tsl patrol` | 巡逻模式 |
| Ignore | `/tsl ignore <玩家>` | 聊天屏蔽 |
| Near | `/tsl near [范围]` | 附近玩家 |
| FixGhost | `/tsl fixghost` | 修复幽灵方块 |
| PlayerList | `/tsl list` | 玩家列表 |

### 世界功能

| 模块 | 说明 |
|------|------|
| BabyLock | 永久幼年生物（命名牌触发） |
| Phantom | 幻翼生成控制 |
| EndDragon | 末影龙破坏限制 |
| FarmProtect | 农田踩踏保护 |
| SnowAntiMelt | 雪层防融化 |

### 系统增强

| 模块 | 说明 |
|------|------|
| Visitor | 访客保护（怪物不攻击、发光） |
| Advancement | 成就消息过滤 |
| FakePlayerMotd | MOTD 假人数 |
| Alias | 命令别名 |
| Permission | 权限检测规则 |
| NewbieTag | 新人标签 |
| PlayerCountCmd | 人数触发命令 |

### 高级功能

| 模块 | 命令 | 说明 |
|------|------|------|
| WebBridge | `/tsl webbridge` | MC↔Web 双向通信，自动重连 |
| Mcedia | `/tsl mcedia` | 视频播放器（盔甲架），配置模板 |
| TimedAttribute | `/tsl attr` | 计时属性效果 |
| Neko | `/tsl neko` | 猫娘模式（聊天后缀） |
| RandomVariable | - | PAPI 混合分布随机数 |
| BlockStats | - | 方块放置统计（PAPI） |

## 配置文件

- `config.yml` - 主配置，所有模块开关和参数
- `aliases.yml` - 命令别名
- `maintenance.yml` - 维护白名单

## PAPI 变量

```
%tsl_adv_count%         成就数量
%tsl_kiss_count%        亲吻次数
%tsl_kissed_count%      被亲次数
%tsl_blocks_placed_total%  放置方块总数
%tsl_ping%              延迟
%tsl_random_<变量名>%   随机数变量
```

## 数据存储

- **PDC**: 玩家开关状态（Kiss/Ride/Toss）
- **YAML**: 玩家档案、屏蔽列表
- **SQLite**: 计时属性、Mcedia 播放器、配置模板

## 构建

```bash
./gradlew shadowJar
```

产物: `build/libs/TSLplugins-1.0.jar`

## 技术栈

- Kotlin 1.9.21
- Gradle 8.5
- Paper/Folia API 1.21.8
- Java 21

---

TSL Minecraft Server

