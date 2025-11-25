# TSLplugins

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21.8-green.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-brightgreen.svg)](https://papermc.io/software/folia)

> Minecraft Folia 1.21.8 多功能整合插件 - 使用 Kotlin 开发

---

## 📖 简介

TSLplugins 是一个功能丰富的 Minecraft 服务器插件，集成了 13 个实用功能模块。完全使用 Kotlin 编写，完美支持 Paper 和 Folia 1.21.8。

### ✨ 核心特性

- 🎯 **13 个功能模块** - 涵盖管理、互动、保护等多个方面
- 🔧 **模块化设计** - 每个功能独立，可单独启用/禁用
- ⚡ **Folia 原生支持** - 完美兼容多线程服务器
- 🔄 **配置热重载** - 无需重启即可应用更改
- 💾 **离线数据持久化** - 玩家配置永久保存
- 🎨 **完全可配置** - 所有功能行为和消息均可自定义

---

## 🎮 功能概览

### 管理工具 (3个)

| 功能 | 命令 | 说明 |
|------|------|------|
| 🔧 **维护模式** | `/tsl maintenance` | 阻止玩家登录，白名单管理 |
| ❄️ **玩家冻结** | `/tsl freeze` | 冻结玩家所有操作，支持定时 |
| 📋 **命令别名** | `/tsl aliasreload` | 自定义命令快捷方式 |

### 玩家互动 (4个)

| 功能 | 触发方式 | 说明 |
|------|---------|------|
| 💋 **玩家亲吻** | `/tsl kiss` 或 Shift+右键玩家 | 粒子+音效，个人开关 |
| 🎩 **帽子系统** | `/tsl hat` | 将手持物品戴在头上 |
| 📏 **体型调整** | `/tsl scale <数值>` | 调整玩家体型大小 |
| 📊 **延迟查询** | `/tsl ping` | 查看延迟，支持排行榜 |

### 生物互动 (3个)

| 功能 | 触发方式 | 说明 |
|------|---------|------|
| 🐾 **生物举起** | Shift+右键生物 | 举起并投掷生物，叠罗汉 |
| 🐎 **生物骑乘** | 右键生物 | 骑乘任意生物 |
| 👶 **永久幼年** | 命名生物 `[幼]xx` | 锁定幼年状态，防止成长 |

### 保护功能 (2个)

| 功能 | 说明 |
|------|------|
| 🌾 **农田保护** | 防止玩家踩踏农田 |
| 👤 **访客保护** | 怪物不攻击、发光效果 |

### 系统增强 (3个)

| 功能 | 说明 |
|------|------|
| 🏆 **成就过滤** | 过滤成就公屏消息 |
| 👥 **MOTD假玩家** | 调整服务器列表显示人数 |
| 📦 **方块统计** | PlaceholderAPI 变量，配合排行榜 |

**总计：15 个功能模块**

---

## 🚀 快速开始

### 安装要求

- **服务器**: Paper 或 Folia 1.21.8+
- **Java**: 21
- **必需插件**: 无
- **可选插件**: 
  - [LuckPerms](https://luckperms.net/) - 访客保护
  - [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) - 变量支持

### 安装步骤

1. 下载 `TSLplugins-1.0.jar`
2. 放入服务器 `plugins` 目录
3. 重启服务器（自动生成配置文件）
4. 根据需要编辑 `plugins/TSLplugins/config.yml`
5. 执行 `/tsl reload` 应用配置

### 基础命令

```bash
/tsl reload              # 重载所有配置
/tsl hat                 # 戴帽子
/tsl scale 1.2           # 调整体型
/tsl ping all            # 查看延迟排行
/tsl kiss <玩家>         # 亲吻玩家
/tsl freeze <玩家> 300   # 冻结玩家 5 分钟
/tsl maintenance toggle  # 切换维护模式
```

---

## 📚 文档

- **[WIKI.md](WIKI.md)** - 详细功能说明、配置示例、权限列表
- **[开发者指南.md](开发者指南.md)** - 架构设计、代码规范、开发指南

---

## ⚙️ 配置文件

### config.yml
主配置文件，包含所有功能模块的配置：
- 功能开关（每个模块独立控制）
- 参数设置（范围、时间、黑名单等）
- 消息文本（完全自定义）

### aliases.yml
命令别名配置：
```yaml
aliases:
  - "t:tpa"          # /t → /tpa
  - "传送:tpa"       # 支持中文
  - "h:home"         # /h → /home
```

### maintenance.yml
维护模式白名单（自动生成和管理）

---

## 🎯 核心功能亮点

### 🔄 配置热重载
修改配置后执行 `/tsl reload` 即可应用，无需重启服务器。

### 💾 离线数据持久化
使用 PersistentDataContainer (PDC) 技术：
- Kiss/Ride/Toss 的个人开关永久保存
- Toss 投掷速度设置永久保存
- 服务器重启不丢失数据

### 🎨 完全可配置
所有功能行为、消息文本、参数范围均可通过配置文件自定义。

### ⚡ 性能优化
- 配置缓存机制（事件处理零开销）
- Folia 原生调度器（完美多线程支持）
- 事件驱动架构（无轮询任务）

---

## 📊 PlaceholderAPI 变量

安装 PlaceholderAPI 后可使用以下变量：

```
%tsl_adv_count%        # 玩家成就数量
%tsl_kiss_count%       # 玩家亲吻次数
%tsl_kissed_count%     # 被亲吻次数
%tsl_kiss_toggle%      # Kiss 功能状态
%tsl_ride_toggle%      # Ride 功能状态
%tsl_toss_toggle%      # Toss 功能状态
%tsl_toss_velocity%    # Toss 投掷速度
```

---

## 🛠️ 技术栈

- **语言**: Kotlin 1.9.21
- **构建工具**: Gradle 8.5 + Kotlin DSL
- **目标平台**: Paper/Folia 1.21.8
- **Java 版本**: 21

---

## 📝 许可证

本项目仅供 TSL 服务器使用。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📮 联系方式

- **作者**: Zvbj
- **服务器**: TSL Minecraft Server

---

## 🎉 致谢

---

## 📝 更新日志

### v1.0 (2025-11-26) - 当前版本

#### 配置版本: 11

**新增功能**：
- ✅ **BlockStats** - 方块统计系统
  - PlaceholderAPI 变量 `%tsl_blocks_placed_total%`
  - 基于原版统计，零性能开销
  - 适配 Topper 排行榜插件

**优化改进**：
- 🔧 config.yml 编码修复（UTF-8）
- 🔧 Toss 黑名单修正（`village` → `VILLAGER`）
- 📝 开发者指南更新（根目录文件详解）
- 📝 架构文档完善（重载系统说明）

---

### 历史版本回顾

#### 配置版本: 10
**重大功能**：
- ✅ **Kiss（玩家亲吻）** - 社交互动系统
  - 双触发方式（命令 + Shift右键）
  - PDC 数据持久化
  - 粒子效果 + 音效
  - 统计系统（亲吻次数/被亲吻次数）
  - PlaceholderAPI 支持

- ✅ **Freeze（玩家冻结）** - 管理员工具
  - 永久/定时冻结
  - 自动过期机制
  - ActionBar 实时提示
  - 全面操作限制
  - 切换式命令

- ✅ **PDC 数据持久化系统**
  - PlayerDataManager 统一管理
  - 替代 HashMap 内存存储
  - 离线数据永久保存
  - 支持跨服同步

**系统优化**：
- 🔧 Freeze 切换命令优化（自动判断冻结/解冻）
- 🔧 TSLPlaceholderExpansion 整合所有变量
- 🔧 配置缓存机制完善
- 📝 开发者指南完善

#### 早期版本
**核心系统**：
- ✅ 维护模式（Maintenance）
- ✅ 命令别名（Alias）  
- ✅ 帽子系统（Hat）
- ✅ 体型调整（Scale）
- ✅ 延迟查询（Ping）
- ✅ 生物举起（Toss）
- ✅ 生物骑乘（Ride）
- ✅ 永久幼年（BabyLock）
- ✅ 农田保护（FarmProtect）
- ✅ 访客保护（Visitor）
- ✅ 成就过滤（Advancement）
- ✅ MOTD 假玩家（FakePlayer）

**架构设计**：
- ✅ Manager-Command-Listener 模式
- ✅ 配置缓存机制
- ✅ 配置版本控制系统
- ✅ 统一重载系统
- ✅ Folia 调度器支持

---

## 🛣️ 开发路线图

### 计划中的功能
- [ ] 数据统计持久化（Kiss 统计数据）
- [ ] 管理日志系统（Freeze 操作记录）
- [ ] 更多 PAPI 变量支持
- [ ] BlockStats 分类统计（石头、木头等）
- [ ] Web 管理面板

### 优化计划
- [ ] 性能监控系统
- [ ] 数据库支持（可选）
- [ ] 多语言支持
- [ ] 更多粒子效果选项

---

## 📄 许可证

本项目为私有项目，仅供 TSL 服务器使用。

---

## 🙏 致谢

感谢所有 TSL 服务器玩家的支持和反馈！

---

**开发信息**：
- 开发语言：Kotlin 1.9.21
- 目标平台：Paper/Folia 1.21.8
- 配置版本：11
- 最后更新：2025-11-26

