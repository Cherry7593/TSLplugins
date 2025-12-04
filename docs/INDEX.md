# TSLplugins 文档总目录

目标：按模块分类整理，便于 AI 和开发者快速定位说明、方案与指令。

---

## 📚 核心文档

### 项目文档
- [README](../README.md) - 项目说明与快速开始
- [需求文档](../需求.md) - 功能需求与规划
- [更新日志](CHANGELOG.md) - 版本更新记录
- [文档说明](README.md) - 文档体系结构说明

### 开发文档
- [开发者指南](DEVELOPER_GUIDE.md) - 架构设计、代码规范、技术要点
- [开发历程](DEVELOPMENT_HISTORY.md) - 项目开发里程碑

### 用户文档
- [功能 WIKI](WIKI.md) - 详细的功能说明、配置指南和权限列表

---

## 🎯 模块索引

### 核心 (Core)
- [Folia 线程安全指引](modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md) - Folia 开发规范
- [配置 YAML 语法修复说明](modules/Core/CONFIG_YAML_SYNTAX_FIX.md) - YAML 语法注意事项

### WebBridge（Web 通信桥）
- [README](modules/WebBridge/README.md) - 模块概览、配置、指令
- [快速参考](modules/WebBridge/QUICK_REFERENCE.md) - 常用指令与错误码
- [架构说明](modules/WebBridge/ARCHITECTURE.md) - 技术架构
- [构建与部署指南](modules/WebBridge/BUILD_GUIDE.md) - 编译部署流程
- [故障排查](modules/WebBridge/TROUBLESHOOTING.md) - 常见问题解决
- [双向通信实现](modules/WebBridge/BIDIRECTIONAL_COMM.md) - MC ↔ Web 消息同步
- [手动连接模式重构](modules/WebBridge/MANUAL_CONNECTION.md) - 连接模式说明

### ChatBubble（聊天气泡）
- [README](modules/ChatBubble/README.md) - 跨区、乘客、调度修复整合

### Visitor（游客/访客模式）
- [README](modules/Visitor/README.md) - 白名单联动、权限驱动、提示优化

### Phantom（幻翼控制）
- [README](modules/Phantom/README.md) - 指令、定时任务与热重载

### Spec（观察模式）
- [README](modules/Spec/README.md) - 观察命令、恢复玩家状态、去重策略

### Kiss（亲吻功能）
- [README](modules/Kiss/README.md) - Kiss 变量持久化与数据迁移

### PlayerData（玩家数据）
- [README](modules/PlayerData/README.md) - PDC→YAML 迁移、NewbieTag、PlaceholderAPI

### Maintenance（维护模式）
- [README](modules/Maintenance/README.md) - 维护模式指令、Folia 兼容、命令补全

---

## 🎯 使用建议

### 新用户快速上手
1. 阅读 [README](../README.md) 了解项目
2. 查看 [功能 WIKI](WIKI.md) 学习具体功能
3. 参考对应模块的 README 深入了解

### 开发者开发指引
1. 阅读 [开发者指南](DEVELOPER_GUIDE.md) 了解架构
2. 查看 [Folia 线程安全指引](modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md)
3. 参考对应模块的文档进行开发

### AI 辅助开发
- 本文件 (INDEX.md) 提供快速导航
- 每个模块的 README.md 包含精简的核心要点
- 技术细节参考对应的子文档
- 代码路径：`src/main/kotlin/org/tsl/tSLplugins/<模块名>/`

---

## 📝 文档维护规范

新增或修改功能时：
1. 更新对应模块的 `modules/<模块名>/README.md`
2. 在本文件 (INDEX.md) 中添加/更新链接
3. 更新 `CHANGELOG.md` 记录变更
4. 如有重大架构变更，更新 `DEVELOPER_GUIDE.md`
5. 如影响用户使用，更新 `WIKI.md`

---

## 🔗 快速链接

**最常用的文档**：
- 📖 [功能详细说明](WIKI.md)
- 🏗️ [开发者指南](DEVELOPER_GUIDE.md)
- 🌐 [WebBridge 完整文档](modules/WebBridge/)
- 🔧 [Folia 线程安全](modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md)

**快速定位模块**：
- 想要修改某个模块？→ 查看 `modules/<模块名>/README.md`
- 遇到线程问题？→ 查看 `modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md`
- 需要添加新功能？→ 参考 `DEVELOPER_GUIDE.md` 的模块开发章节

