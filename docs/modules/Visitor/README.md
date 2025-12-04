# Visitor（访客模式）模块 README

合并自：VISITOR_* 与 SUMMARY_Visitor_*。

- 逻辑说明：白名单变量联动 Permission；不在白名单则默认访客权限。
- 优化建议：减少重复提示（例如压力板事件只提示一次或禁用提示）。
- 配置建议：移除单独 visitor 权限组的强依赖，转为变量驱动。

