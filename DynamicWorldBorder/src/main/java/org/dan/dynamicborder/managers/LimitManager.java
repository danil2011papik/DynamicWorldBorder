package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.data.WorldBorderData;

public class LimitManager {

    private final DynamicBorderPlugin plugin;

    public LimitManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean validateSizeLimit(String worldName, double newSize) {
        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data == null) return false;

        return newSize >= data.getAbsoluteMinSize() &&
                newSize <= data.getAbsoluteMaxSize() &&
                newSize >= data.getPlayerMinSize() &&
                newSize <= data.getPlayerMaxSize();
    }

    public boolean validateSpeedLimit(String worldName, double newSpeed) {
        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data == null) return false;

        return newSpeed >= data.getAbsoluteMinSpeed() &&
                newSpeed <= data.getAbsoluteMaxSpeed() &&
                newSpeed >= data.getPlayerMinSpeed() &&
                newSpeed <= data.getPlayerMaxSpeed();
    }

    public boolean validateDamageLimit(String worldName, double newDamage) {
        WorldBorderData data = plugin.getBorderManager().getWorldData(worldName);
        if (data == null) return false;

        return newDamage >= data.getAbsoluteMinDamage() &&
                newDamage <= data.getAbsoluteMaxDamage() &&
                newDamage >= data.getPlayerMinDamage() &&
                newDamage <= data.getPlayerMaxDamage();
    }
}