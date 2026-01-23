# Change: Add Minecart Boost Module

## Why

玩家需要更快的矿车交通系统。原版矿车速度约 8m/s，在大型服务器上的长距离旅行耗时过长。通过在特定方块上放置动力铁轨，可以使载人矿车获得更高的速度。

## What Changes

- 新增 MinecartBoost 模块，监听矿车经过动力铁轨事件
- 当载人矿车经过动力铁轨且铁轨下方为指定方块时，提升矿车速度
- 支持 8 种方块对应 3 档速度：
  - **16m/s (2x)**: 浮冰 (PACKED_ICE)、煤炭块 (COAL_BLOCK)
  - **24m/s (3x)**: 雕纹深板岩 (CHISELED_DEEPSLATE)、磨制玄武岩 (POLISHED_BASALT)、切制铜块 (CUT_COPPER)
  - **30m/s (3.75x)**: 去皮苍白橡木 (STRIPPED_PALE_OAK_WOOD)、暗海晶石 (DARK_PRISMARINE)、雕纹凝灰�ite砖 (CHISELED_TUFF_BRICKS)
- 可通过配置文件启用/禁用模块及自定义方块-速度映射

## Impact

- Affected specs: `minecart-boost` (new capability)
- Affected code: 新建 `MinecartBoost/` 模块目录
