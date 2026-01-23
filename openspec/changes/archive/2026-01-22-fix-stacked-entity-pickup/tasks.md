## 1. Implementation
- [x] 1.1 修改 `isEntityInPassengerChain` 函数，改为 `isEntityHeldByPlayer`，只检查实体是否被玩家持有
- [x] 1.2 在 `pickupEntity` 中添加逻辑：如果实体有 vehicle，先将其从 vehicle 中移除
- [ ] 1.3 手动测试：举起多个生物 → 传送走 → 回来尝试抱起堆叠的生物
