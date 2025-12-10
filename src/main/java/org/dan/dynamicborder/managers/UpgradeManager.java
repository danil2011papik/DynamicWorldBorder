package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.data.WorldBorderData;
import org.bukkit.entity.Player;

public class UpgradeManager {

    private final DynamicBorderPlugin plugin;

    public UpgradeManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canUpgrade(Player player, String worldName, String upgradeType) {
        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data == null || !data.isEnabled() || !data.isUpgradable()) {
            return false;
        }

        // Здесь можно добавить дополнительные проверки
        return true;
    }

    public double calculateUpgradeCost(String worldName, String upgradeType, int level) {
        // Базовая реализация
        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data == null) return 0;

        switch (upgradeType.toLowerCase()) {
            case "expand":
                return data.getExpandCost() * (1 + level * 0.01);
            case "shrink":
                return data.getShrinkCost() * (1 + level * 0.01);
            case "speed-up":
                return data.getSpeedUpCost() * (1 + level * 0.01);
            case "speed-down":
                return data.getSpeedDownCost() * (1 + level * 0.01);
            case "damage-up":
                return data.getDamageUpCost() * (1 + level * 0.01);
            case "damage-down":
                return data.getDamageDownCost() * (1 + level * 0.01);
            default:
                return 0;
        }
    }
}