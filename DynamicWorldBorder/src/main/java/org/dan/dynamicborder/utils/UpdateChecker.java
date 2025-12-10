package org.dan.dynamicborder.utils;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final DynamicBorderPlugin plugin;
    private final int resourceId;

    public UpdateChecker(DynamicBorderPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(
                        "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId
                ).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                String latestVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
                String currentVersion = plugin.getDescription().getVersion();

                if (!latestVersion.equals(currentVersion)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().info("§6══════════════════════════════════════");
                        plugin.getLogger().info("§eДоступно обновление!");
                        plugin.getLogger().info("§7Текущая версия: §c" + currentVersion);
                        plugin.getLogger().info("§7Новая версия: §a" + latestVersion);
                        plugin.getLogger().info("§6══════════════════════════════════════");
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось проверить обновления", e);
            }
        });
    }
}