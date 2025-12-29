# MC 账号绑定验证 API 文档

## 概述

本文档说明 MC 插件如何实现 `/tsl bind <验证码>` 命令，用于验证玩家的网站账号绑定。

## 用户流程

```
1. 用户在官网 /bind 页面点击"生成验证码"
2. 网站显示 6 位验证码，如 A3K9F2
3. 用户在游戏内输入: /tsl bind A3K9F2
4. MC 插件向 WebSocket Bridge 发送绑定请求
5. WebSocket Bridge 验证成功后，绑定完成
6. 网页实时更新显示绑定成功
```

## WebSocket 协议

### 请求格式

```json
{
  "type": "request",
  "source": "mc",
  "timestamp": 1735200000000,
  "data": {
    "action": "BIND_ACCOUNT",
    "id": "bind-1735200000000-abc123",
    "playerUuid": "02d3b2c1-f448-40a5-83a4-641f91a9a888",
    "playerName": "NanKinz1",
    "code": "A3K9F2"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | ✓ | 固定为 `request` |
| source | string | ✓ | 固定为 `mc` |
| timestamp | number | ✓ | 请求时间戳（毫秒） |
| data.action | string | ✓ | 固定为 `BIND_ACCOUNT` |
| data.id | string | ✓ | 唯一请求 ID |
| data.playerUuid | string | ✓ | 玩家 UUID（带横杠格式） |
| data.playerName | string | ✓ | 玩家游戏名 |
| data.code | string | ✓ | 6 位验证码（大写） |

### 成功响应

```json
{
  "type": "response",
  "source": "web",
  "timestamp": 1735200000100,
  "data": {
    "action": "BIND_ACCOUNT",
    "id": "bind-1735200000000-abc123",
    "success": true,
    "message": "绑定成功！已关联到用户：似龠",
    "userId": "123456789012345678",
    "userName": "似龠"
  }
}
```

### 失败响应

```json
{
  "type": "response",
  "source": "web",
  "timestamp": 1735200000100,
  "data": {
    "action": "BIND_ACCOUNT",
    "id": "bind-1735200000000-abc123",
    "success": false,
    "error": "invalid_code",
    "message": "验证码无效"
  }
}
```

### 错误码

| error | 含义 | 建议提示 |
|-------|------|---------|
| `invalid_code` | 验证码不存在或已使用 | 验证码无效，请检查是否输入正确 |
| `expired_code` | 验证码已过期 | 验证码已过期，请在网站重新获取 |
| `already_bound` | 该 MC 账号已被其他用户绑定 | 该账号已绑定到其他用户 |
| `self_bound` | 该 MC 账号已绑定到同一用户 | 该账号已绑定到你的账户 |
| `server_error` | 服务器内部错误 | 服务器错误，请稍后重试 |

## Java 实现示例

### 命令处理器

```java
public class BindCommand implements CommandExecutor {
    private final WebSocketClient wsClient;
    
    public BindCommand(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 1) {
            player.sendMessage("§c用法: /tsl bind <验证码>");
            player.sendMessage("§7请先在官网 §bhttps://zenoxs.cn/bind §7获取验证码");
            return true;
        }
        
        String code = args[0].toUpperCase().trim();
        
        // 验证码格式检查（6位字母数字，排除 0,O,I,L,1）
        if (!code.matches("^[A-HJ-NP-Z2-9]{6}$")) {
            player.sendMessage("§c验证码格式无效，请检查是否输入正确");
            return true;
        }
        
        player.sendMessage("§e正在验证...");
        
        // 发送绑定请求
        wsClient.sendBindRequest(player, code);
        
        return true;
    }
}
```

### WebSocket 请求发送

```java
public class WebSocketClient {
    private WebSocket webSocket;
    private Map<String, Consumer<JsonObject>> pendingRequests = new ConcurrentHashMap<>();
    
    public void sendBindRequest(Player player, String code) {
        String requestId = "bind-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        JsonObject request = new JsonObject();
        request.addProperty("type", "request");
        request.addProperty("source", "mc");
        request.addProperty("timestamp", System.currentTimeMillis());
        
        JsonObject data = new JsonObject();
        data.addProperty("action", "BIND_ACCOUNT");
        data.addProperty("id", requestId);
        data.addProperty("playerUuid", player.getUniqueId().toString());
        data.addProperty("playerName", player.getName());
        data.addProperty("code", code);
        request.add("data", data);
        
        // 注册响应回调
        pendingRequests.put(requestId, response -> {
            boolean success = response.get("success").getAsBoolean();
            String message = response.get("message").getAsString();
            
            // 确保在主线程发送消息
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage("§a✓ " + message);
                    player.sendMessage("§7现在你可以在官网查看个人资料并设置称号了！");
                } else {
                    player.sendMessage("§c✗ " + message);
                    String error = response.has("error") ? response.get("error").getAsString() : "";
                    if ("expired_code".equals(error)) {
                        player.sendMessage("§7请在官网重新获取验证码: §bhttps://zenoxs.cn/bind");
                    }
                }
            });
        });
        
        // 设置超时（10秒）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.remove(requestId) != null) {
                player.sendMessage("§c绑定请求超时，请稍后重试");
            }
        }, 200L); // 10秒 = 200 ticks
        
        webSocket.send(request.toString());
    }
    
    // 处理响应
    public void onMessage(String message) {
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
        
        if ("response".equals(json.get("type").getAsString())) {
            JsonObject data = json.getAsJsonObject("data");
            String action = data.get("action").getAsString();
            String id = data.get("id").getAsString();
            
            if ("BIND_ACCOUNT".equals(action)) {
                Consumer<JsonObject> callback = pendingRequests.remove(id);
                if (callback != null) {
                    callback.accept(data);
                }
            }
        }
    }
}
```

## 验证码规则

- **格式**: 6 位大写字母 + 数字
- **字符集**: `ABCDEFGHJKMNPQRSTUVWXYZ23456789`（排除易混淆字符 0,O,I,L,1）
- **有效期**: 5 分钟
- **使用次数**: 一次性，使用后立即失效

## 绑定规则

1. **同一 MC 账号只能绑定一个网站用户**
   - 已验证绑定的 MC 账号不能被其他用户绑定
   - 未验证的绑定记录会被覆盖

2. **一个用户可以绑定多个 MC 账号**
   - 每个账号需要单独验证
   - 第一个验证成功的账号自动设为主账号

3. **验证状态**
   - 只有验证过的账号才能使用称号、查看游戏数据等功能
   - 未验证的账号在个人资料页会显示"待验证"标记

## 测试

### 使用 wscat 测试

```bash
# 连接 WebSocket
wscat -c "ws://127.0.0.1:4001/mc-bridge?from=mc"

# 发送绑定请求（替换实际的 UUID、名字和验证码）
{"type":"request","source":"mc","timestamp":1735200000000,"data":{"action":"BIND_ACCOUNT","id":"test-1","playerUuid":"02d3b2c1-f448-40a5-83a4-641f91a9a888","playerName":"TestPlayer","code":"A3K9F2"}}
```

### 获取测试验证码

1. 登录网站
2. 访问 `/bind` 页面
3. 点击"生成验证码"
4. 使用返回的验证码进行测试