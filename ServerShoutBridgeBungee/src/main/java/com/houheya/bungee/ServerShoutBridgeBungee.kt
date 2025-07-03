package com.houheya.bungee

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ServerShoutBridgeBungee : Plugin(), Listener {

    // 服务引用
    private var balanceService: Any? = null
    private var tokenService: Any? = null

    // 状态管理
    private var isHooked = false
    private val activePlayers = ConcurrentHashMap<Pair<String, String>, Long>()

    override fun onEnable() {
        ProxyServer.getInstance().registerChannel("servershout:bridge")
        proxy.pluginManager.registerListener(this, this)
        proxy.pluginManager.registerCommand(this, ReloadCommand(this))

        // 启动定时同步任务（每15秒）
        proxy.scheduler.schedule(this, {
            if (isHooked) {
                activePlayers.keys.removeIf { (player, token) ->
                    syncBalanceToServers(player, token) == false
                }
            }
        }, 15, 15, TimeUnit.SECONDS)

        // 延迟初始化（等待ServerShout加载）
        proxy.scheduler.schedule(this, {
            checkServerShoutHook()
            logger.info(if (isHooked) "${ChatColor.GREEN}✓ 已挂钩ServerShout API"
            else "${ChatColor.RED}✗ ServerShout未加载")
        }, 5, TimeUnit.SECONDS)
    }

    private fun checkServerShoutHook() {
        try {
            val pluginClass = Class.forName("io.github.theramu.servershout.bungee.ServerShoutBungeePlugin")
            val api = pluginClass.getMethod("getAPI").invoke(null)

            balanceService = api.javaClass.getMethod("getBalanceService").invoke(api)
            tokenService = api.javaClass.getMethod("getTokenService").invoke(api)
            isHooked = true
        } catch (e: Exception) {
            logger.warning("${ChatColor.RED}挂钩失败: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != "servershout:bridge") return

        DataInputStream(ByteArrayInputStream(event.data)).use { input ->
            val playerName = input.readUTF()
            val tokenType = input.readUTF()

            // 标记为活跃玩家
            activePlayers[playerName to tokenType] = System.currentTimeMillis()

            if (isHooked) {
                handleBalanceRequest(playerName, tokenType)
            }
        }
    }

    private fun handleBalanceRequest(playerName: String, tokenType: String) {
        try {
            val token = tokenService?.javaClass
                ?.getMethod("getToken", String::class.java)
                ?.invoke(tokenService, tokenType)
                ?: throw IllegalStateException("代币不存在")

            val balance = balanceService?.javaClass
                ?.getMethod("getBalanceAmount", String::class.java, token.javaClass)
                ?.invoke(balanceService, playerName, token) as Long

            syncBalanceToServers(playerName, tokenType, balance)
        } catch (e: Exception) {
            logger.warning("${ChatColor.YELLOW}处理请求失败: ${e.message}")
        }
    }

    private fun syncBalanceToServers(playerName: String, tokenType: String, balance: Long? = null): Boolean {
        if (!isHooked) return false

        return try {
            val finalBalance = balance ?: getBalanceFromAPI(playerName, tokenType)

            ByteArrayOutputStream().use { byteStream ->
                DataOutputStream(byteStream).apply {
                    writeUTF(playerName)
                    writeUTF(tokenType)
                    writeLong(finalBalance)
                }

                // 广播到所有服务器
                ProxyServer.getInstance().servers.values.forEach { server ->
                    server.sendData("servershout:bridge", byteStream.toByteArray())
                }
                //logger.info("${ChatColor.GRAY}[同步] $playerName 的 $tokenType: $finalBalance")
            }
            true
        } catch (e: Exception) {
            logger.warning("${ChatColor.RED}同步失败: ${e.message}")
            false
        }
    }

    private fun getBalanceFromAPI(playerName: String, tokenType: String): Long {
        val token = tokenService?.javaClass
            ?.getMethod("getToken", String::class.java)
            ?.invoke(tokenService, tokenType)
            ?: throw IllegalStateException("无效代币类型")

        return balanceService?.javaClass
            ?.getMethod("getBalanceAmount", String::class.java, token.javaClass)
            ?.invoke(balanceService, playerName, token) as Long
    }

    private class ReloadCommand(private val plugin: ServerShoutBridgeBungee) :
        Command("ssbbreload", "servershoutbridge.reload", "ssbr") {

        override fun execute(sender: CommandSender, args: Array<String>) {
            plugin.checkServerShoutHook()
            sender.sendMessage(TextComponent("${ChatColor.GOLD}ServerShoutBridge 重载完成"))
            sender.sendMessage(TextComponent("API状态: ${
                if (plugin.isHooked) "${ChatColor.GREEN}已连接" else "${ChatColor.RED}未连接"
            }"))
        }
    }
}