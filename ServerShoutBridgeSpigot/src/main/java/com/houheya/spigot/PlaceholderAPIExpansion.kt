package com.houheya.spigot

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

class PlaceholderAPIExpansion(private val plugin: ServerShoutBridgeSpigot) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "servershout"
    override fun getAuthor(): String = "houheya"
    override fun getVersion(): String = plugin.description.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null

        return when {
            params.startsWith("balance_") -> {
                val tokenType = params.removePrefix("balance_")
                getFormattedBalance(player.name, tokenType)
            }
            params == "status" -> getSyncStatus(player.name)
            else -> null
        }
    }

    private fun getFormattedBalance(playerName: String, tokenType: String): String {
        val cacheKey = playerName to tokenType
        val currentTime = System.currentTimeMillis()
        val lastUpdate = plugin.lastRequestTime[cacheKey] ?: 0

        return when {
            // 情况1：已验证的真实零值
            plugin.zeroValueMarkers.containsKey(cacheKey) -> "0"

            // 情况2：数据有效（5秒内更新过）
            plugin.balanceCache.containsKey(cacheKey) &&
                    (currentTime - lastUpdate) < TimeUnit.SECONDS.toMillis(5) -> {
                plugin.balanceCache[cacheKey]?.toString() ?: "0"
            }

            // 情况3：首次请求
            !plugin.balanceCache.containsKey(cacheKey) && lastUpdate == 0L -> {
                plugin.requestBalanceUpdate(playerName, tokenType)
                "⌛ 首次同步中..."
            }

            // 情况4：响应超时（超过10秒）
            (currentTime - lastUpdate) > TimeUnit.SECONDS.toMillis(10) -> {
                plugin.requestBalanceUpdate(playerName, tokenType)
                "⚠ 同步超时"
            }

            // 情况5：正常同步中
            else -> {
                if (!plugin.pendingRequests.containsKey(cacheKey)) {
                    plugin.requestBalanceUpdate(playerName, tokenType)
                }
                "↻ 同步中..."
            }
        }.also {
            //plugin.logger.info("[$playerName] $tokenType 余额查询: $it")
        }
    }

    private fun getSyncStatus(playerName: String): String {
        val activeSyncs = plugin.lastRequestTime.count { (key, time) ->
            key.first == playerName &&
                    System.currentTimeMillis() - time < TimeUnit.MINUTES.toMillis(1)
        }
        return "活跃同步数: $activeSyncs"
    }
}