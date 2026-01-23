# Change: Fix Stacked Entity Pickup

## Why
当玩家举起多个生物后传送走，生物会掉落并相互堆叠（一个骑在另一个上面）。当前的 `isEntityInPassengerChain` 检查过于严格，阻止了玩家抱起任何已在乘客链中的实体，即使这些实体没有被任何玩家持有。这导致堆叠的生物永远无法被分开。

## What Changes
- 修改 `isEntityInPassengerChain` 检查逻辑，只阻止抱起**被玩家持有**的实体
- 允许抱起没有玩家持有的堆叠实体（自动将其从当前乘客链中移除后再抱起）

## Impact
- Affected specs: `toss`
- Affected code: `TossListener.kt`
