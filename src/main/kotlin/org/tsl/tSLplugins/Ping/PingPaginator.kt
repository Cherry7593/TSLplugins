package org.tsl.tSLplugins.Ping

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

/**
 * Ping 分页显示器
 * 负责处理延迟信息的分页显示逻辑
 */
class PingPaginator(private val manager: PingManager) {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 显示分页的 ping 列表
     */
    fun showPingList(player: Player, page: Int) {
        val allPlayers = manager.getAllPlayersPing()

        if (allPlayers.isEmpty()) {
            player.sendMessage(serializer.deserialize(manager.getMessage("no_players_online")))
            return
        }

        val entriesPerPage = manager.getEntriesPerPage()
        val totalPages = (allPlayers.size + entriesPerPage - 1) / entriesPerPage
        val currentPage = page.coerceIn(1, totalPages)

        // 显示标题分割线
        player.sendMessage(serializer.deserialize("&7&m                                                "))

        // 显示平均延迟
        val averagePing = manager.getAveragePing()
        val avgColorCode = manager.getPingColorCode(averagePing.toInt())
        val avgMessage = manager.getMessage(
            "average_ping",
            "ping" to String.format("%.1f", averagePing)
        ).replace("{color}", avgColorCode)
        player.sendMessage(serializer.deserialize(avgMessage))

        // 显示页码标题
        val headerMessage = manager.getMessage(
            "page_header",
            "page" to currentPage.toString(),
            "total_pages" to totalPages.toString()
        )
        player.sendMessage(serializer.deserialize(headerMessage))
        // 计算当前页的数据范围
        val startIndex = (currentPage - 1) * entriesPerPage
        val endIndex = minOf(startIndex + entriesPerPage, allPlayers.size)

        // 显示当前页的延迟信息
        for (i in startIndex until endIndex) {
            val info = allPlayers[i]
            val rank = i + 1
            val entry = createFormattedPingEntry(rank, info.playerName, info.ping)
            player.sendMessage(entry)
        }

        // 显示分页按钮
        if (totalPages > 1) {
            val paginationButtons = createPaginationButtons(currentPage, totalPages)
            player.sendMessage(paginationButtons)
        }

        // 显示底部分割线
        player.sendMessage(serializer.deserialize("&7&m                                                "))
    }

    /**
     * 创建格式化的 ping 条目
     */
    private fun createFormattedPingEntry(rank: Int, playerName: String, ping: Int): Component {
        val colorCode = manager.getPingColorCode(ping)

        // 格式化排名（灰色）
        val rankText = "&7$rank."

        // 格式化玩家名称（白色，固定宽度）
        val paddedName = String.format("%-16s", playerName)
        val nameText = "&f$paddedName"

        // 格式化延迟（带颜色）
        val pingText = "$colorCode${ping}ms"

        // 组合完整文本
        val fullText = "$rankText $nameText     $pingText"

        return serializer.deserialize(fullText)
    }

    /**
     * 创建分页按钮
     */
    private fun createPaginationButtons(currentPage: Int, totalPages: Int): Component {
        var result = Component.empty()

        // 上一页按钮
        if (currentPage > 1) {
            val prevButton = serializer.deserialize("&c[← 上一页]")
                .clickEvent(ClickEvent.runCommand("/tsl ping all ${currentPage - 1}"))
                .hoverEvent(HoverEvent.showText(serializer.deserialize("&7点击查看上一页")))
            result = result.append(prevButton)
        }

        // 页码信息
        val pageInfo = serializer.deserialize(" &7| 第 $currentPage/$totalPages 页 | ")
        result = result.append(pageInfo)

        // 下一页按钮
        if (currentPage < totalPages) {
            val nextButton = serializer.deserialize("&a[下一页 →]")
                .clickEvent(ClickEvent.runCommand("/tsl ping all ${currentPage + 1}"))
                .hoverEvent(HoverEvent.showText(serializer.deserialize("&7点击查看下一页")))
            result = result.append(nextButton)
        }

        return result
    }

    /**
     * 解析页码参数
     */
    fun parsePage(args: Array<out String>, startIndex: Int, defaultPage: Int = 1): Int {
        if (args.size > startIndex) {
            return args[startIndex].toIntOrNull() ?: defaultPage
        }
        return defaultPage
    }
}


