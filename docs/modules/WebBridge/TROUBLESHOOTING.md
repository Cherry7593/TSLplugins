# WebBridge 故障排查

- 长时间无法连接：检查服务器端口与路径；使用 127.0.0.1 替代 localhost。
- Token 无效：确保 URL 中包含 token 参数或服务端允许无认证。
- Folia 报错：确保使用 GlobalRegionScheduler 广播消息。

