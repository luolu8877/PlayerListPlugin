package com.applepeo.playerlistplugin;

import com.google.gson.Gson;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import spark.Spark;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlayerListPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Spark.stop();
        Bukkit.getScheduler().runTaskLater(this, this::startWebAPI, 1L);
        getLogger().info("PlayerListPlugin enabled! WebAPI on port " + getConfig().getInt("api-port", 9960));
    }

    @Override
    public void onDisable() {
        Spark.stop();
        getLogger().info("PlayerListPlugin disabled!");
    }

    /* ---------------- 命令处理 ---------------- */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("online")) {
            List<String> list = getOnlinePlayers();
            if (list.isEmpty()) {
                sender.sendMessage("当前没有在线玩家。");
            } else {
                sender.sendMessage("当前在线玩家 (" + list.size() + ")：");
                sender.sendMessage(String.join(", ", list));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("plp")) {
            if (!sender.hasPermission("plp.admin")) {
                sender.sendMessage("§c你没有权限使用该命令。");
                return true;
            }
            if (args.length < 3 || !args[0].equalsIgnoreCase("api") || !args[1].equalsIgnoreCase("port")) {
                sender.sendMessage("§e用法：/plp api port <端口号>");
                return true;
            }
            try {
                int newPort = Integer.parseInt(args[2]);
                if (newPort < 1024 || newPort > 65535) {
                    sender.sendMessage("§c端口号必须在 1024-65535 之间。");
                    return true;
                }
                Spark.stop();
                getConfig().set("api-port", newPort);
                saveConfig();
                Bukkit.getScheduler().runTaskLater(this, this::startWebAPI, 1L);
                sender.sendMessage("§aAPI 端口已切换为 " + newPort + "，已重新启动。");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c端口号必须是数字！");
            }
            return true;
        }
        return false;
    }

    /* ---------------- WebAPI ---------------- */
    private void startWebAPI() {
        int port = getConfig().getInt("api-port", 9960);
        Spark.port(port);
        Spark.threadPool(8, 2, 30_000);

        Gson gson = new Gson();
        Spark.get("/api/status", (req, res) -> {
            res.type("application/json");
            List<String> players = getOnlinePlayers();
            Map<String, Object> sys = sampleSystem();
            return gson.toJson(Map.of(
                    "players", players,
                    "count", players.size(),
                    "system", sys
            ));
        });
    }

    /* -------------- 工具 -------------- */
    private List<String> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /* -------------- 系统采样（访问瞬间执行） -------------- */
    private Map<String, Object> sampleSystem() {
        SystemInfo si = new SystemInfo();
        CentralProcessor cpu = si.getHardware().getProcessor();

        /* 1. CPU：手动采集两次 ticks */
        long[] prevTicks = cpu.getSystemCpuLoadTicks();   // 第一次
        try { Thread.sleep(1000); } catch (InterruptedException ignore) {}
        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100; // ✅ 传参版本

        /* 2. 内存 */
        GlobalMemory mem = si.getHardware().getMemory();
        long totalMem = mem.getTotal();
        long availMem = mem.getAvailable();

        /* 3. 磁盘：取根分区 */
        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getUsableSpace();

        /* 4. 网络：第一块网卡 200 ms 瞬时速率 */
        NetworkIF net = si.getHardware().getNetworkIFs().get(0);
        long rx = net.getBytesRecv();
        long tx = net.getBytesSent();
        try { Thread.sleep(200); } catch (InterruptedException ignore) {}
        net.updateAttributes();
        double rxRate = (net.getBytesRecv() - rx) * 8 / 0.2 / 1_000_000;
        double txRate = (net.getBytesSent() - tx) * 8 / 0.2 / 1_000_000;

        return Map.of(
                "cpu", String.format("%.2f", cpuLoad),
                "memory", Map.of(
                        "totalMB", totalMem / 1024 / 1024,
                        "usedMB", (totalMem - availMem) / 1024 / 1024,
                        "usage", String.format("%.2f", (totalMem - availMem) * 100.0 / totalMem)
                ),
                "disk", Map.of(
                        "totalGB", totalDisk / 1024 / 1024 / 1024,
                        "usedGB", (totalDisk - freeDisk) / 1024 / 1024 / 1024,
                        "usage", String.format("%.2f", (totalDisk - freeDisk) * 100.0 / totalDisk)
                ),
                "network", Map.of(
                        "rxMbps", String.format("%.2f", rxRate),
                        "txMbps", String.format("%.2f", txRate)
                )
        );
    }
}