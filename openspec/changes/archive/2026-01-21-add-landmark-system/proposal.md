# Change: Add Landmark System

## Why

服务器需要一个持久化的交通骨架系统，让玩家通过探索解锁地标，并在地标区域内传送到已解锁的目标地标。当前没有此类功能。

## What Changes

- 新增 Landmark 模块，包含地标管理、区域检测、解锁系统、GUI菜单和传送功能
- 新增管理员命令：创建/编辑/删除地标、设置区域范围、授权维护者
- 新增玩家命令：打开地标GUI、传送到已解锁地标
- 新增区域进入检测与解锁逻辑
- 新增数据持久化（地标数据、玩家解锁记录）
- 新增配置项：区域体积上限、传送规则、提示消息等

## Impact

- Affected specs: `landmark` (new capability)
- Affected code:
  - `src/main/kotlin/org/tsl/tSLplugins/landmark/` (new module)
  - `src/main/resources/config.yml` (new section)
  - Database/file storage for landmark and unlock data
