# TSLplugins

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21.8-green.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-brightgreen.svg)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)

> Minecraft Folia 1.21.8 多功能整合插件 - 使用 Kotlin 开发

---

## 📖 简介

TSLplugins 是一个功能丰富的 Minecraft 服务器插件，集成了多个实用功能模块。完全使用 Kotlin 编写，原生支持 Paper 和 Folia 1.21.8 多线程架构。

### ✨ 核心特性

- 🎯 **模块化架构** - 多个独立功能模块，可按需启用
- 🔧 **即插即用设计** - 每个模块独立运行，互不干扰
- ⚡ **Folia 原生支持** - 完美兼容多线程区域调度器
- 🔄 **配置热重载** - 无需重启即可应用配置更改
- 💾 **数据持久化** - 玩家配置 YAML 存储，支持离线编辑
- 🎨 **完全可配置** - 所有功能行为和消息均可自定义
- 🌐 **Web 通信桥** - MC ↔ Web 双向消息同步

---

## 🚀 快速开始

### 环境要求

- Java 21+
- Paper 1.21.8 或 Folia 1.21.8
- 可选依赖：PlaceholderAPI、LuckPerms

### 安装步骤

1. 下载最新版本的 `TSLplugins-1.0.jar`
2. 将 JAR 文件放入服务器的 `plugins/` 目录
3. 启动服务器，插件将自动生成配置文件
4. 编辑 `plugins/TSLplugins/config.yml` 配置所需功能
5. 使用 `/tsl reload` 重载配置

### 基础命令

```
/tsl reload          - 重载配置
/tsl help            - 查看帮助
/tsl <模块> <参数>   - 使用模块功能
```

---

## 🎮 功能模块

### 核心管理

| 模块 | 命令 | 说明 |
|------|------|------|
| 🔧 **维护模式** | `/tsl maintenance` | 维护期间阻止玩家登录，支持白名单 |
| 🌐 **WebBridge** | `/tsl webbridge` | MC ↔ Web 双向通信，手动连接模式 |
| 👁️ **观察模式** | `/tsl spec` | 随机观察在线玩家，支持白名单 |
| 👻 **幻翼控制** | `/tsl phantom` | 控制幻翼生成，定时检查与重置 |

### 玩家互动

| 模块 | 触发方式 | 说明 |
|------|---------|------|
| 💋 **玩家亲吻** | `/tsl kiss` 或 Shift+右键玩家 | 粒子+音效，个人开关 |
| 🎩 **帽子系统** | `/tsl hat` | 将手持物品戴在头上 |
| 📏 **体型调整** | `/tsl scale` | 调整玩家体型大小 |
| 📊 **延迟查询** | `/tsl ping` | 查看延迟，支持排行榜 |

### 游客保护

| 模块 | 说明 | 特性 |
|------|------|------|
| 🚪 **访客模式** | 白名单驱动的权限联动 | 压力板/按钮拦截，减少提示刷屏 |

---

## 📚 文档导航

### 用户文档
- 📖 **[功能详细说明](docs/WIKI.md)** - 所有功能的命令、权限、配置详解
- 🔄 **[更新日志](docs/CHANGELOG.md)** - 版本更新记录
- ❓ **[常见问题](docs/WIKI.md#常见问题)** - 疑难解答

### 开发文档
- 🏗️ **[开发者指南](docs/DEVELOPER_GUIDE.md)** - 架构设计、代码规范、技术要点
- 📋 **[模块文档总目录](docs/INDEX.md)** - 按模块分类的详细文档
- 🔧 **[Folia 线程安全指引](docs/modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md)** - Folia 开发规范
- 🌐 **[WebBridge 文档](docs/modules/WebBridge/)** - Web 通信桥完整说明
- 📝 **[开发历程](docs/DEVELOPMENT_HISTORY.md)** - 项目开发里程碑

### 快速定位
想要了解或修改某个模块？查看 [模块文档索引](docs/INDEX.md)，每个模块都有独立的 README。

---

## 🏗️ 项目结构

```
TSLplugins/
├── src/main/kotlin/org/tsl/tSLplugins/
│   ├── TSLplugins.kt              # 主类
│   ├── TSLCommand.kt              # 统一命令分发
│   ├── ReloadCommand.kt           # 重载命令
│   ├── ConfigUpdateManager.kt     # 配置版本控制
│   ├── PlayerDataManager.kt       # 玩家数据管理
│   ├── WebBridge/                 # Web 通信桥模块
│   ├── Maintenance/               # 维护模式模块
│   ├── Visitor/                   # 访客模式模块
│   ├── Phantom/                   # 幻翼控制模块
│   ├── Spec/                      # 观察模式模块
│   └── ...                        # 其他模块
├── docs/                          # 文档目录
│   ├── INDEX.md                   # 文档总目录
│   ├── WIKI.md                    # 功能详细说明
│   ├── DEVELOPER_GUIDE.md         # 开发者指南
│   └── modules/                   # 按模块分类的文档
│       ├── Core/                  # 核心文档
│       ├── WebBridge/             # WebBridge 模块文档
│       └── ...                    # 其他模块文档
├── README.md                      # 项目说明（本文件）
└── 需求.md                        # 功能需求文档
```

---

## 🛠️ 开发与构建

### 克隆项目

```bash
git clone https://github.com/your-repo/TSLplugins.git
cd TSLplugins
```

### 构建插件

```bash
# Windows
.\gradlew.bat shadowJar

# Linux/Mac
./gradlew shadowJar
```

构建产物：`build/libs/TSLplugins-1.0.jar`

### 开发环境

- **IDE**: IntelliJ IDEA 2024+
- **Kotlin**: 1.9.21
- **Gradle**: 8.5
- **Java**: 21

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 开发前必读

1. 阅读 [开发者指南](docs/DEVELOPER_GUIDE.md)
2. 了解 [Folia 线程安全规范](docs/modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md)
3. 查看对应模块的文档（[模块索引](docs/INDEX.md)）

### 代码规范

- 使用 Kotlin 编码规范
- 所有实体/世界操作必须在正确的 RegionScheduler 上执行
- 配置驱动，避免硬编码
- 保持模块独立性
- UTF-8 编码

### 提交 Pull Request

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目使用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- [Paper](https://papermc.io/) - 高性能 Minecraft 服务端
- [Folia](https://papermc.io/software/folia) - 多线程区域调度器
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - 变量占位符支持
- [LuckPerms](https://luckperms.net/) - 权限管理系统

---

## 📧 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/your-repo/TSLplugins/issues)
- **功能建议**: [GitHub Discussions](https://github.com/your-repo/TSLplugins/discussions)

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**

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

