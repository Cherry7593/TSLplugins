# 项目文档说明

本文档说明 TSLplugins 的文档体系结构。

## 📚 文档结构

### 根目录文档
- **README.md** - 项目介绍与快速开始
- **需求.md** - 功能需求文档

### docs/ 目录
- **INDEX.md** - 文档总目录（导航入口）
- **DEVELOPER_GUIDE.md** - 开发者指南（架构、规范、技术要点）
- **WIKI.md** - 功能详细说明（命令、权限、配置）
- **CHANGELOG.md** - 更新日志
- **DEVELOPMENT_HISTORY.md** - 开发历程总结

### docs/modules/ 目录
按模块组织的详细文档，每个模块包含：
- README.md - 模块概览
- 其他技术文档

#### 已有模块
- **Core/** - 核心功能（Folia 线程安全、配置语法）
- **WebBridge/** - Web 通信桥
- **ChatBubble/** - 聊天气泡
- **Visitor/** - 访客模式
- **Phantom/** - 幻翼控制
- **Spec/** - 观察模式
- **Kiss/** - 亲吻功能
- **PlayerData/** - 玩家数据
- **Maintenance/** - 维护模式

## 🎯 使用建议

### 新用户
1. 阅读 README.md 了解项目
2. 查看 docs/WIKI.md 学习具体功能
3. 参考 docs/modules/<模块名>/README.md 深入了解某个模块

### 开发者
1. 阅读 docs/DEVELOPER_GUIDE.md 了解架构
2. 查看 docs/modules/<模块名>/ 了解具体实现
3. 参考 docs/modules/Core/FOLIA_THREAD_SAFETY_GUIDE.md 确保线程安全

### AI 辅助开发
- docs/INDEX.md 提供快速导航
- 每个模块的 README.md 包含精简的要点
- 技术细节参考对应的子文档

## 📝 文档维护

新增或修改功能时：
1. 更新对应模块的 README.md
2. 在 docs/INDEX.md 中添加链接
3. 更新 docs/CHANGELOG.md
4. 如有重大变更，更新 docs/DEVELOPER_GUIDE.md

