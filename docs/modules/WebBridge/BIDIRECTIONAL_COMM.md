# Web ↔ MC 双向通信实现

- MC→Web：监听聊天事件，构建 JSON，入队发送。
- Web→MC：onMessage 解析 `type=chat, source=web`，使用 Adventure 颜色广播。

