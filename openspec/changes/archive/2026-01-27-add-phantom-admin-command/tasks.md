## 1. Implementation

- [x] 1.1 修改 `PhantomModuleCommand` 添加玩家参数解析逻辑
- [x] 1.2 添加 `tsl.phantom.admin` 权限检查
- [x] 1.3 实现 `handleOnAdmin(sender, targetPlayer)` 方法处理管理员操作
- [x] 1.4 实现 `handleOffAdmin(sender, targetPlayer)` 方法处理管理员操作
- [x] 1.5 实现 `handleStatusAdmin(sender, targetPlayer)` 方法处理管理员操作
- [x] 1.6 更新 Tab 补全支持在线玩家名补全
- [x] 1.7 更新帮助信息显示管理员命令

## 2. Validation

- [x] 2.1 测试普通玩家使用 `/tsl phantom off` 修改自己状态
- [x] 2.2 测试管理员使用 `/tsl phantom off <玩家名>` 修改他人状态
- [x] 2.3 测试无权限玩家尝试修改他人状态时的错误提示
- [x] 2.4 测试目标玩家不在线时的错误提示
