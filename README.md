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

### 管理工具

#### 🔧 维护模式
- 服务器维护期间阻止玩家登录
- 白名单管理（UUID 验证）
- 自定义维护 MOTD
- 命令：`/tsl maintenance`

#### ❄️ 玩家冻结
- 冻结玩家所有操作（管理员工具）
- 支持永久或定时冻结
- ActionBar 实时倒计时
- 命令：`/tsl freeze <玩家> [秒]`

#### 📋 命令别名
- 自定义命令快捷方式
- 支持中文别名
- 完整 Tab 补全
- 命令：`/tsl aliasreload`

---

### 玩家互动

#### 💋 玩家亲吻
- Shift + 右键玩家亲吻
- 爱心粒子 + 音效
- 个人开关防骚扰
- 命令：`/tsl kiss <玩家>`

#### 🎩 帽子系统
- 将任意物品戴在头上
- 支持堆叠物品（自动只戴 1 个）
- 背包满时自动掉落
- 命令：`/tsl hat`

#### 📏 体型调整
- 调整玩家体型大小
- 可配置范围限制
- 支持小数精度
- 命令：`/tsl scale <数值>`

#### 📊 延迟查询
- 查看单人或全服延迟
- 延迟排行榜（可点击翻页）
- 颜色分级显示
- 命令：`/tsl ping [玩家|all]`

---

### 生物互动

#### 🐾 生物举起（Toss）
- **Shift + 右键生物** - 举起生物
- **左键** - 投掷生物
- 叠罗汉效果（最多 3 个）
- 可调节投掷速度
- 个人开关防误触
- 命令：`/tsl toss toggle`

#### 🐎 生物骑乘（Ride）
- **右键生物** - 直接骑乘
- 支持骑乘任意生物
- 黑名单配置
- 个人开关控制
- 命令：`/tsl ride toggle`

#### 👶 永久幼年（BabyLock）
- 给幼年生物命名特殊前缀锁定
- 前缀：`[幼]`、`[小]`、`[Baby]`
- 防止成长和消失
- 自动检测和锁定

---

### 保护功能

#### 🌾 农田保护
- 防止玩家踩踏农田
- 自动保护所有耕地
- 无需命令，自动生效

#### 👤 访客保护
- LuckPerms 权限驱动
- 怪物不会攻击访客
- 访客发光效果
- 权限：`tsl.visitor`

---

### 系统增强

#### 🏆 成就过滤
- 过滤成就公屏消息
- 保留挑战成就公告
- 个人成就统计
- PlaceholderAPI 变量

#### 👥 MOTD 假玩家
- 调整服务器列表人数显示
- 正数增加，负数减少
- 配置：`fakeplayer.count`

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

感谢所有 TSL 服务器玩家的支持和反馈！

