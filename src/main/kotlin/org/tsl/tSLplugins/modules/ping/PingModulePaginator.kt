package org.tsl.tSLplugins.modules.ping

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Ping 分页显示器
 * 负责处理延迟信息的分页显示逻辑
 */
class PingModulePaginator(private val module: PingModule) {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 显示分页的 ping 列表
     */
    fun showPingList(sender: CommandSender, page: Int) {
        val allPlayers = module.getAllPlayersPing()

        if (allPlayers.isEmpty()) {
            sender.sendMessage(serializer.deserialize(module.getModuleMessage("no_players_online")))
            return
        }

        val entriesPerPage = module.getEntriesPerPage()
        val totalPages = (allPlayers.size + entriesPerPage - 1) / entriesPerPage
        val currentPage = page.coerceIn(1, totalPages)

        // 显示标题分割线
        sender.sendMessage(serializer.deserialize("&7&m                                                "))

        // 显示平均延迟
        val averagePing = module.getAveragePing()
        val avgColorCode = module.getPingColorCode(averagePing.toInt())
        val avgMessage = module.getModuleMessage(
            "average_ping",
            "ping" to String.format("%.1f", averagePing)
        ).replace("{color}", avgColorCode)
        sender.sendMessage(serializer.deserialize(avgMessage))

        // 显示页码标题
        val headerMessage = module.getModuleMessage(
            "page_header",
            "page" to currentPage.toString(),
            "total_pages" to totalPages.toString()
        )
        sender.sendMessage(serializer.deserialize(headerMessage))

        // 计算当前页的数据范围
        val startIndex = (currentPage - 1) * entriesPerPage
        val endIndex = minOf(startIndex + entriesPerPage, allPlayers.size)

        // 显示当前页的延迟信息
        for (i in startIndex until endIndex) {
            val info = allPlayers[i]
            val rank = i + 1
            val entry = createFormattedPingEntry(rank, info.playerName, info.ping)
            sender.sendMessage(entry)
        }

        // 显示分页按钮（仅玩家可用）
        if (totalPages > 1) {
            if (sender is Player) {
                val paginationButtons = createPaginationButtons(currentPage, totalPages)
                sender.sendMessage(paginationButtons)
            } else {
                sender.sendMessage(serializer.deserialize("&7第 $currentPage/$totalPages 页 | 使用 /tsl ping all <页码> 查看其他页"))
            }
        }

        // 显示底部分割线
        sender.sendMessage(serializer.deserialize("&7&m                                                "))
    }

    /**
     * 创建格式化的 ping 条目
     */
    private fun createFormattedPingEntry(rank: Int, playerName: String, ping: Int): Component {
        val colorCode = module.getPingColorCode(ping)
        val rankText = "&7$rank."
        val paddedName = String.format("%-16s", playerName)
        val nameText = "&f$paddedName"
        val pingText = "$colorCode${ping}ms"
        val fullText = "$rankText $nameText     $pingText"
        return serializer.deserialize(fullText)
    }

    /**
     * 创建分页按钮
     */
    private fun createPaginationButtons(currentPage: Int, totalPages: Int): Component {
        var result = Component.empty()

        if (currentPage > 1) {
            val prevButton = serializer.deserialize("&c[← 上一页]")
                .clickEvent(ClickEvent.runCommand("/tsl ping all ${currentPage - 1}"))
                .hoverEvent(HoverEvent.showText(serializer.deserialize("&7点击查看上一页")))
            result = result.append(prevButton)
        }

        val pageInfo = serializer.deserialize(" &7| 第 $currentPage/$totalPages 页 | ")
        result = result.append(pageInfo)

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
