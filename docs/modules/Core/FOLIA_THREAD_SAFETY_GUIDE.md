# Folia 线程安全指引（核心）

来源合并自 archive/FOLIA_THREAD_SAFETY_GUIDE.md 及相关说明，统一放置。

要点：
- 仅在合适的 RegionScheduler 上操作实体与世界对象。
- 使用 Bukkit.getGlobalRegionScheduler() 进行全局无实体任务。
- 玩家相关操作优先使用 Player.getScheduler().
- Teleport 必须使用 teleportAsync。

