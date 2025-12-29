# MC 插件称号系统对接完成说明

> 本文档供 Web 端开发参考，说明 MC 插件已完成的称号系统对接内容

## 概述

MC 插件已完成称号系统的全部对接工作，可以：
- 接收 Web 端推送的称号更新事件
- 玩家上线时自动请求称号
- 支持游戏内兑换码验证
- 通过 LuckPerms 设置玩家 prefix/suffix

## WebSocket 消息格式

### 1. MC → Web: 获取称号请求 (GET_TITLE)

**触发时机**: 玩家加入服务器后 1 秒

```json
{
  "type": "request",
  "source": "mc",
  "timestamp": 1735123456789,
  "data": {
    "action": "GET_TITLE",
    "id": "gt-1735123456789",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888"
  }
}
```

### 2. Web → MC: 获取称号响应

**成功响应**:
```json
{
  "type": "response",
  "source": "web",
  "timestamp": 1735123456789,
  "data": {
    "action": "GET_TITLE",
    "id": "gt-1735123456789",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888",
    "title": "<gradient:#ff0000:#ffff00>大佬</gradient>",
    "position": "prefix",
    "tier": 2,
    "found": true
  }
}
```

**未找到响应**:
```json
{
  "type": "response",
  "source": "web",
  "data": {
    "action": "GET_TITLE",
    "id": "gt-1735123456789",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888",
    "found": false
  }
}
```

### 3. Web → MC: 称号更新事件 (TITLE_UPDATE)

**触发时机**: 玩家在官网修改称号后主动推送

```json
{
  "type": "event",
  "source": "web",
  "timestamp": 1735123456789,
  "data": {
    "event": "TITLE_UPDATE",
    "id": "tu-1735123456789",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888",
    "playerName": "NanKinz1",
    "title": "<gradient:#ff0000:#ffff00>大佬</gradient>",
    "position": "prefix",
    "tier": 2
  }
}
```

**清除称号时 title 为 null**:
```json
{
  "data": {
    "event": "TITLE_UPDATE",
    "playerUuid": "...",
    "title": null,
    "position": "prefix",
    "tier": 0
  }
}
```

### 4. MC → Web: 兑换码验证 (REDEEM_CODE)

```json
{
  "type": "request",
  "source": "mc",
  "timestamp": 1735123456789,
  "data": {
    "action": "REDEEM_CODE",
    "id": "rc-1735123456789",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888",
    "playerName": "NanKinz1",
    "code": "TITLE-ABC12345"
  }
}
```

### 5. Web → MC: 兑换码响应

**成功**:
```json
{
  "type": "response",
  "source": "web",
  "timestamp": 1735123456789,
  "data": {
    "action": "REDEEM_CODE",
    "id": "rc-1735123456789",
    "success": true,
    "message": "兑换成功！已解锁全彩称号权限",
    "grantedTier": 2
  }
}
```

**失败**:
```json
{
  "type": "response",
  "source": "web",
  "data": {
    "action": "REDEEM_CODE",
    "id": "rc-1735123456789",
    "success": false,
    "error": "invalid_code",
    "message": "兑换码无效或已被使用"
  }
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `playerUuid` | string | ✅ | 玩家 UUID，带横线格式 |
| `playerName` | string | ❌ | 玩家名称 |
| `title` | string/null | ✅ | 称号内容，null 表示清除 |
| `position` | string | ✅ | `"prefix"` 或 `"suffix"` |
| `tier` | number | ✅ | 付费等级 0/1/2 |
| `found` | boolean | ✅ | GET_TITLE 响应专用 |
| `id` | string | ✅ | 请求 ID，用于匹配响应 |

## 颜色格式支持

MC 插件直接将 `title` 字段内容设置到 LuckPerms，支持以下格式：

- **传统代码**: `&6[&e大佬&6]`
- **MiniMessage**: `<gradient:#ff0000:#00ff00>大佬</gradient>`
- **十六进制**: `<#FF5733>大佬`

> 最终显示效果取决于服务器的聊天插件是否支持对应格式

## LuckPerms 集成细节

- **权重**: 默认 100（可配置）
- **识别方式**: 按权重识别 TSL 设置的称号
- **清除逻辑**: 清除该权重的所有 prefix/suffix 后再设置新值
- **异步操作**: 所有 LP 操作异步执行，不阻塞主线程

## 游戏内命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/tsl title redeem <code>` | 使用兑换码 | `tsl.title.redeem` |
| `/tsl title info` | 查看当前称号 | 无 |
| `/tsl title help` | 显示帮助 | 无 |

## 配置项

```yaml
# config.yml
title:
  enabled: true
  luckperms-priority: 100
  join-delay: 20
```

## Web 端需要实现

1. **GET_TITLE 接口**: 根据 playerUuid 查询玩家称号数据
2. **TITLE_UPDATE 推送**: 玩家在官网修改称号时推送事件
3. **REDEEM_CODE 接口**: 验证兑换码并返回结果
4. **数据存储**: 存储玩家称号、位置、等级信息

## 测试建议

1. **基础流程**:
   - 玩家上线 → MC 发送 GET_TITLE → Web 返回数据 → 称号生效
   
2. **实时更新**:
   - 官网修改称号 → Web 推送 TITLE_UPDATE → MC 即时更新
   
3. **兑换码**:
   - 游戏内输入 `/tsl title redeem CODE` → 验证 → 反馈结果

## 完成状态

| 功能 | 状态 |
|------|------|
| 接收 TITLE_UPDATE 事件 | ✅ |
| 发送 GET_TITLE 请求 | ✅ |
| 处理 GET_TITLE 响应 | ✅ |
| 发送 REDEEM_CODE 请求 | ✅ |
| 处理 REDEEM_CODE 响应 | ✅ |
| LuckPerms prefix 设置 | ✅ |
| LuckPerms suffix 设置 | ✅ |
| 玩家上线自动请求 | ✅ |
| 称号缓存 | ✅ |
| 游戏内命令 | ✅ |

---

**MC 插件端已完成全部对接，等待 Web 端实现对应接口即可联调。**
