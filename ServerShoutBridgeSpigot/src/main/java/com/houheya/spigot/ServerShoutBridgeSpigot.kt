package com.houheya.spigot

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.scheduler.BukkitRunnable
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ServerShoutBridgeSpigot : JavaPlugin(), PluginMessageListener {

    // 数据存储结构（改为internal以便扩展类访问）
    internal val balanceCache = ConcurrentHashMap<Pair<String, String>, Long>()
    internal val lastRequestTime = ConcurrentHashMap<Pair<String, String>, Long>()
    internal val pendingRequests = ConcurrentHashMap<Pair<String, String>, Boolean>()
    internal val zeroValueMarkers = ConcurrentHashMap<Pair<String, String>, Boolean>()

    override fun onEnable() {
        // 注册通信通道
        server.messenger.registerOutgoingPluginChannel(this, "servershout:bridge")
        server.messenger.registerIncomingPluginChannel(this, "servershout:bridge", this)

        // 修复1：明确指定Runnable重载版本
        server.scheduler.runTaskLater(this, Runnable {
            if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
                // 修复2：正确初始化扩展类
                PlaceholderAPIExpansion(this).also {
                    it.register()
                    logger.info("PlaceholderAPI扩展注册成功")
                }

                // 修复3：使用BukkitRunnable明确任务类型
                object : BukkitRunnable() {
                    override fun run() {
                        balanceCache.keys.forEach { (player, token) ->
                            if (!pendingRequests.containsKey(player to token)) {
                                requestBalanceUpdate(player, token)
                            }
                        }
                    }
                }.runTaskTimerAsynchronously(this@ServerShoutBridgeSpigot, 600L, 600L)
            } else {
                logger.warning("未检测到PlaceholderAPI，变量功能不可用")
            }
        }, 20L)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "servershout:bridge") return

        DataInputStream(ByteArrayInputStream(message)).use { input ->
            val playerName = input.readUTF()
            val tokenType = input.readUTF()
            val balance = input.readLong()

            handleBalanceUpdate(playerName, tokenType, balance)
        }
    }

    private fun handleBalanceUpdate(playerName: String, tokenType: String, balance: Long) {
        val cacheKey = playerName to tokenType

        balanceCache[cacheKey] = balance
        lastRequestTime[cacheKey] = System.currentTimeMillis()
        pendingRequests.remove(cacheKey)

        // 标记真实零值
        if (balance == 0L) {
            zeroValueMarkers[cacheKey] = true
        } else {
            zeroValueMarkers.remove(cacheKey)
        }

        //logger.info("[$playerName] $tokenType 余额更新: $balance")
    }

    // 改为internal以便扩展类访问
    internal fun requestBalanceUpdate(playerName: String, tokenType: String) {
        val cacheKey = playerName to tokenType

        if (pendingRequests.putIfAbsent(cacheKey, true) != null) return

        server.onlinePlayers.firstOrNull()?.let { proxyPlayer ->
            // 修复4：明确指定Runnable重载
            server.scheduler.runTaskAsynchronously(this, Runnable {
                try {
                    ByteArrayOutputStream().use { byteStream ->
                        DataOutputStream(byteStream).apply {
                            writeUTF(playerName)
                            writeUTF(tokenType)
                        }
                        proxyPlayer.sendPluginMessage(
                            this@ServerShoutBridgeSpigot,
                            "servershout:bridge",
                            byteStream.toByteArray()
                        )
                    }
                } catch (e: Exception) {
                    pendingRequests.remove(cacheKey)
                    logger.warning("请求 ${playerName} 的 $tokenType 余额失败: ${e.message}")
                }
            })
        }
    }
}