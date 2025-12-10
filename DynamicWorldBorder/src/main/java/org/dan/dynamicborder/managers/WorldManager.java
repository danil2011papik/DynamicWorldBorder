package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.data.WorldBorderData;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class WorldManager {

    private final DynamicBorderPlugin plugin;
    private final Map<String, WorldBorderData> worldCache = new HashMap<>();

    public WorldManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public int getLoadedWorldsCount() {
        return worldCache.size();
    }

    public void saveAllWorlds() {
        for (WorldBorderData data : worldCache.values()) {
            plugin.getBorderManager().saveWorldData(data);
        }
    }

    public boolean loadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data != null) {
            worldCache.put(worldName, data);
        }

        return true;
    }
}