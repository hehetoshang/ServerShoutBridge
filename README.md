# ServerShoutBridge - 跨服经济数据同步系统

![GPL-3.0 License](https://img.shields.io/badge/License-GPL%203.0-blue)
![Minecraft 1.19-1.20.X](https://img.shields.io/badge/Minecraft-1.19--1.20.X-success)
![Build Status](https://img.shields.io/github/actions/workflow/status/yourname/ServerShoutBridge/build.yml)

## 项目状态

### 已实现核心功能
- **跨服通信架构**
    - 二进制协议压缩传输
    - 自动断线重连机制
    - 多线程异步处理

- **实时数据同步**
    - 玩家经济数据即时更新
    - 智能缓存系统（5秒TTL）
    - 多货币类型支持

- **开发者接口**
    - 完整的Java/Kotlin API
    - BalanceUpdateEvent事件系统
    - 调试模式日志追踪

### 计划开发功能
- **数据持久化**
    - MySQL/Redis支持
    - 定期自动备份
    - 数据迁移工具

- **管理增强**
    - 跨服交易监控面板
    - 实时数据统计分析
    - 玩家数据导出功能

- **扩展兼容**
    - Folia服务端支持
    - 第三方经济插件适配
    - 集群模式部署方案

## 技术规格

**运行环境要求**：
- BungeeCord/Waterfall 核心
- Spigot/Paper 1.19-1.20.X
- ServerShout 经济插件
- PlaceholderAPI (可选)

**协议兼容性**：
- 支持GPLv3开源协议
- 数据加密传输（AES-256）
- 跨版本通信协议

## 部署指南

1. **服务端安装**
    - 将ServerShoutBridgeBungee.jar放入BungeeCord的plugins目录
    - 在各子服plugins目录放入ServerShoutBridgeSpigot.jar

2. **配置示例**
   在BungeeCord的config.yml中添加：
   ```yaml
   server_shout_bridge:
     sync_interval: 15
     max_retry: 3
   ```

3. **权限节点**
    - `servershoutbridge.admin` - 管理命令权限
    - `servershoutbridge.debug` - 调试模式权限

## 开发者文档

**API调用示例**：
```java
// 获取玩家余额
long balance = ServerShoutBridge.getAPI().getBalance("player1", "gold");

// 注册事件监听
Bukkit.getPluginManager().registerEvents(new Listener() {
    @EventHandler
    public void onUpdate(BalanceUpdateEvent event) {
        // 处理逻辑
    }
}, plugin);
```

**构建说明**：
```bash
./gradlew clean build
# 输出产物：
# - bungee/build/libs/ServerShoutBridgeBungee.jar
# - spigot/build/libs/ServerShoutBridgeSpigot.jar
```

## 开源协议
本项目采用GNU General Public License v3.0协议开源，您可以在遵守许可证条款的前提下自由使用、修改和分发。

Copyright © 2023 [您的姓名/组织名]