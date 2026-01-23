# Tasks

## 1. 项目初始化

- [x] 1.1 创建 `FabricMod/TSLWebBridge/` 目录结构
- [x] 1.2 配置 `build.gradle` (Fabric Loom, Shadow plugin)
- [x] 1.3 配置 `gradle.properties` (MC 版本 1.21.11, Fabric 版本)
- [x] 1.4 创建 `fabric.mod.json` 元数据

## 2. 配置系统

- [x] 2.1 创建 `ModConfig.java` 配置类
- [x] 2.2 实现配置文件读写 (`config/tsl-webbridge.json`)
- [ ] 2.3 添加配置热重载命令 (延后实现，非核心功能)

## 3. WebSocket 客户端

- [x] 3.1 添加 Java-WebSocket 依赖
- [x] 3.2 创建 `WebBridgeClient.java` (连接管理、消息队列)
- [x] 3.3 实现自动重连逻辑
- [x] 3.4 实现心跳保活

## 4. 玩家列表上报

- [x] 4.1 创建 `PlayerListReporter.java`
- [x] 4.2 实现 `PLAYER_LIST` 事件消息构建
- [x] 4.3 获取服务器 TPS/MSPT (MinecraftServer.getTickTimes)
- [x] 4.4 注册定时任务上报

## 5. Mod 入口

- [x] 5.1 创建 `TSLWebBridgeMod.java` 入口类
- [x] 5.2 实现 `onInitializeServer()` 初始化
- [x] 5.3 注册玩家进出事件监听
- [x] 5.4 实现优雅关闭

## 6. 验证

- [ ] 6.1 本地启动 Fabric 服务器测试
- [ ] 6.2 验证 WebSocket 连接成功
- [ ] 6.3 验证 PLAYER_LIST 消息格式与 Paper 一致
- [ ] 6.4 验证 Web 端正确接收并区分 serverId
