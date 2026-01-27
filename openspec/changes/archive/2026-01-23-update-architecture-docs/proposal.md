# Proposal: Update Architecture Documentation

## Why

经过架构优化（Phase A/B/C），项目结构发生了重大变化：
- 48 个模块统一迁移到 `modules/` 目录
- 新增 `service/` 服务层和 `core/` 框架层
- 引入 `AbstractModule` 生命周期管理

当前 `project.md` 和 `core/spec.md` 未反映这些变化，可能导致：
- 新开发者不了解正确的模块创建流程
- AI 助手可能生成不符合新架构的代码
- Folia 兼容性要求未被强调

## What Changes

### 1. 更新 `project.md`

- 更新架构模式部分，添加新目录结构
- 添加 AbstractModule 模式说明
- 添加服务层（service/）说明
- 强化 Folia 兼容性指南
- 添加模块创建清单

### 2. 更新 `core/spec.md`

- 添加模块目录结构要求
- 添加 AbstractModule 生命周期要求
- 添加服务层架构要求
- 添加 Folia 调度器要求

## Impact

- **开发效率提升**：清晰的文档指导新模块开发
- **代码一致性**：统一的架构规范减少代码审查成本
- **AI 协作改善**：AI 助手可生成符合架构的代码
- **零运行时影响**：仅文档更新，不涉及代码变更
