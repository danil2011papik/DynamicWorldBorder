package org.dan.dynamicborder.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("WorldBorderData")
public class WorldBorderData implements ConfigurationSerializable {

    // Основные параметры
    private String worldName;
    private boolean enabled = true;
    private boolean upgradable = true;
    private boolean shopEnabled = true;

    // Текущие значения
    private double currentSize = 1000.0;
    private double currentSpeed = 1.0;
    private double currentDamage = 2.0;
    private double warningDistance = 10.0;
    private double damageBuffer = 5.0;

    // Абсолютные лимиты (устанавливаются админом)
    private double absoluteMaxSize = 30000.0;
    private double absoluteMinSize = 50.0;
    private double absoluteMaxSpeed = 10.0;
    private double absoluteMinSpeed = 0.1;
    private double absoluteMaxDamage = 20.0;
    private double absoluteMinDamage = 0.0;

    // Лимиты для игроков (в рамках абсолютных)
    private double playerMaxSize = 30000.0;
    private double playerMinSize = 50.0;
    private double playerMaxSpeed = 5.0;
    private double playerMinSpeed = 0.5;
    private double playerMaxDamage = 10.0;
    private double playerMinDamage = 0.5;

    // Цены (базовые)
    private double expandCost = 1.0;
    private double shrinkCost = 0.5;
    private double speedUpCost = 15.0;
    private double speedDownCost = 5.0;
    private double damageDownCost = 12.0;
    private double damageUpCost = 8.0;

    // Шаги улучшений
    private double upgradeStepSize = 1.0;
    private double upgradeStepSpeed = 0.1;
    private double upgradeStepDamage = 0.1;

    // Множители цен
    private double priceMultiplierSize = 1.0;
    private double priceMultiplierSpeed = 1.0;
    private double priceMultiplierDamage = 1.0;

    // Статистика
    private int totalExpansions = 0;
    private int totalShrinks = 0;
    private int totalSpeedUpgrades = 0;
    private int totalSpeedDowngrades = 0;
    private int totalDamageUpgrades = 0;
    private int totalDamageDowngrades = 0;
    private double totalCurrencySpent = 0.0;
    private double totalCurrencyEarned = 0.0;

    // Временные метки
    private long creationTime;
    private long lastModified;
    private long lastExpansion;
    private long lastShrink;

    // Настройки магазина для мира
    private Map<String, Double> worldItemPrices = new HashMap<>();

    public WorldBorderData() {
        this.creationTime = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    public WorldBorderData(String worldName) {
        this();
        this.worldName = worldName;
    }

    // ========== ГЕТТЕРЫ ==========
    public String getWorldName() { return worldName; }
    public boolean isEnabled() { return enabled; }
    public boolean isUpgradable() { return upgradable; }
    public boolean isShopEnabled() { return shopEnabled; }

    public double getCurrentSize() { return currentSize; }
    public double getCurrentSpeed() { return currentSpeed; }
    public double getCurrentDamage() { return currentDamage; }
    public double getWarningDistance() { return warningDistance; }
    public double getDamageBuffer() { return damageBuffer; }

    public double getAbsoluteMaxSize() { return absoluteMaxSize; }
    public double getAbsoluteMinSize() { return absoluteMinSize; }
    public double getAbsoluteMaxSpeed() { return absoluteMaxSpeed; }
    public double getAbsoluteMinSpeed() { return absoluteMinSpeed; }
    public double getAbsoluteMaxDamage() { return absoluteMaxDamage; }
    public double getAbsoluteMinDamage() { return absoluteMinDamage; }

    public double getPlayerMaxSize() { return playerMaxSize; }
    public double getPlayerMinSize() { return playerMinSize; }
    public double getPlayerMaxSpeed() { return playerMaxSpeed; }
    public double getPlayerMinSpeed() { return playerMinSpeed; }
    public double getPlayerMaxDamage() { return playerMaxDamage; }
    public double getPlayerMinDamage() { return playerMinDamage; }

    public double getExpandCost() { return expandCost; }
    public double getShrinkCost() { return shrinkCost; }
    public double getSpeedUpCost() { return speedUpCost; }
    public double getSpeedDownCost() { return speedDownCost; }
    public double getDamageDownCost() { return damageDownCost; }
    public double getDamageUpCost() { return damageUpCost; }

    public double getUpgradeStepSize() { return upgradeStepSize; }
    public double getUpgradeStepSpeed() { return upgradeStepSpeed; }
    public double getUpgradeStepDamage() { return upgradeStepDamage; }

    public double getPriceMultiplierSize() { return priceMultiplierSize; }
    public double getPriceMultiplierSpeed() { return priceMultiplierSpeed; }
    public double getPriceMultiplierDamage() { return priceMultiplierDamage; }

    public int getTotalExpansions() { return totalExpansions; }
    public int getTotalShrinks() { return totalShrinks; }
    public int getTotalSpeedUpgrades() { return totalSpeedUpgrades; }
    public int getTotalSpeedDowngrades() { return totalSpeedDowngrades; }
    public int getTotalDamageUpgrades() { return totalDamageUpgrades; }
    public int getTotalDamageDowngrades() { return totalDamageDowngrades; }
    public double getTotalCurrencySpent() { return totalCurrencySpent; }
    public double getTotalCurrencyEarned() { return totalCurrencyEarned; }

    public long getCreationTime() { return creationTime; }
    public long getLastModified() { return lastModified; }
    public long getLastExpansion() { return lastExpansion; }
    public long getLastShrink() { return lastShrink; }

    public Map<String, Double> getWorldItemPrices() { return worldItemPrices; }
    public Double getItemPrice(String item) { return worldItemPrices.get(item); }

    // ========== СЕТТЕРЫ ==========
    public void setWorldName(String worldName) {
        this.worldName = worldName;
        updateModified();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateModified();
    }

    public void setUpgradable(boolean upgradable) {
        this.upgradable = upgradable;
        updateModified();
    }

    public void setShopEnabled(boolean shopEnabled) {
        this.shopEnabled = shopEnabled;
        updateModified();
    }

    public void setCurrentSize(double currentSize) {
        this.currentSize = Math.max(absoluteMinSize, Math.min(absoluteMaxSize, currentSize));
        updateModified();
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = Math.max(absoluteMinSpeed, Math.min(absoluteMaxSpeed, currentSpeed));
        updateModified();
    }

    public void setCurrentDamage(double currentDamage) {
        this.currentDamage = Math.max(absoluteMinDamage, Math.min(absoluteMaxDamage, currentDamage));
        updateModified();
    }

    public void setWarningDistance(double warningDistance) {
        this.warningDistance = Math.max(1, Math.min(100, warningDistance));
        updateModified();
    }

    public void setDamageBuffer(double damageBuffer) {
        this.damageBuffer = Math.max(0, Math.min(50, damageBuffer));
        updateModified();
    }

    public void setAbsoluteMaxSize(double absoluteMaxSize) {
        this.absoluteMaxSize = Math.max(10, Math.min(1000000, absoluteMaxSize));
        if (playerMaxSize > absoluteMaxSize) playerMaxSize = absoluteMaxSize;
        if (currentSize > absoluteMaxSize) currentSize = absoluteMaxSize;
        updateModified();
    }

    public void setAbsoluteMinSize(double absoluteMinSize) {
        this.absoluteMinSize = Math.max(1, Math.min(10000, absoluteMinSize));
        if (playerMinSize < absoluteMinSize) playerMinSize = absoluteMinSize;
        if (currentSize < absoluteMinSize) currentSize = absoluteMinSize;
        updateModified();
    }

    public void setAbsoluteMaxSpeed(double absoluteMaxSpeed) {
        this.absoluteMaxSpeed = Math.max(0.1, Math.min(100, absoluteMaxSpeed));
        if (playerMaxSpeed > absoluteMaxSpeed) playerMaxSpeed = absoluteMaxSpeed;
        if (currentSpeed > absoluteMaxSpeed) currentSpeed = absoluteMaxSpeed;
        updateModified();
    }

    public void setAbsoluteMinSpeed(double absoluteMinSpeed) {
        this.absoluteMinSpeed = Math.max(0.01, Math.min(10, absoluteMinSpeed));
        if (playerMinSpeed < absoluteMinSpeed) playerMinSpeed = absoluteMinSpeed;
        if (currentSpeed < absoluteMinSpeed) currentSpeed = absoluteMinSpeed;
        updateModified();
    }

    public void setAbsoluteMaxDamage(double absoluteMaxDamage) {
        this.absoluteMaxDamage = Math.max(0, Math.min(500, absoluteMaxDamage));
        if (playerMaxDamage > absoluteMaxDamage) playerMaxDamage = absoluteMaxDamage;
        if (currentDamage > absoluteMaxDamage) currentDamage = absoluteMaxDamage;
        updateModified();
    }

    public void setAbsoluteMinDamage(double absoluteMinDamage) {
        this.absoluteMinDamage = Math.max(0, Math.min(50, absoluteMinDamage));
        if (playerMinDamage < absoluteMinDamage) playerMinDamage = absoluteMinDamage;
        if (currentDamage < absoluteMinDamage) currentDamage = absoluteMinDamage;
        updateModified();
    }

    public void setPlayerMaxSize(double playerMaxSize) {
        this.playerMaxSize = Math.max(absoluteMinSize, Math.min(absoluteMaxSize, playerMaxSize));
        updateModified();
    }

    public void setPlayerMinSize(double playerMinSize) {
        this.playerMinSize = Math.max(absoluteMinSize, Math.min(absoluteMaxSize, playerMinSize));
        updateModified();
    }

    public void setPlayerMaxSpeed(double playerMaxSpeed) {
        this.playerMaxSpeed = Math.max(absoluteMinSpeed, Math.min(absoluteMaxSpeed, playerMaxSpeed));
        updateModified();
    }

    public void setPlayerMinSpeed(double playerMinSpeed) {
        this.playerMinSpeed = Math.max(absoluteMinSpeed, Math.min(absoluteMaxSpeed, playerMinSpeed));
        updateModified();
    }

    public void setPlayerMaxDamage(double playerMaxDamage) {
        this.playerMaxDamage = Math.max(absoluteMinDamage, Math.min(absoluteMaxDamage, playerMaxDamage));
        updateModified();
    }

    public void setPlayerMinDamage(double playerMinDamage) {
        this.playerMinDamage = Math.max(absoluteMinDamage, Math.min(absoluteMaxDamage, playerMinDamage));
        updateModified();
    }

    public void setExpandCost(double expandCost) {
        this.expandCost = Math.max(0.01, Math.min(1000, expandCost));
        updateModified();
    }

    public void setShrinkCost(double shrinkCost) {
        this.shrinkCost = Math.max(0.01, Math.min(1000, shrinkCost));
        updateModified();
    }

    public void setSpeedUpCost(double speedUpCost) {
        this.speedUpCost = Math.max(0.01, Math.min(1000, speedUpCost));
        updateModified();
    }

    public void setSpeedDownCost(double speedDownCost) {
        this.speedDownCost = Math.max(0.01, Math.min(1000, speedDownCost));
        updateModified();
    }

    public void setDamageDownCost(double damageDownCost) {
        this.damageDownCost = Math.max(0.01, Math.min(1000, damageDownCost));
        updateModified();
    }

    public void setDamageUpCost(double damageUpCost) {
        this.damageUpCost = Math.max(0.01, Math.min(1000, damageUpCost));
        updateModified();
    }

    public void setUpgradeStepSize(double upgradeStepSize) {
        this.upgradeStepSize = Math.max(0.1, Math.min(1000, upgradeStepSize));
        updateModified();
    }

    public void setUpgradeStepSpeed(double upgradeStepSpeed) {
        this.upgradeStepSpeed = Math.max(0.01, Math.min(10, upgradeStepSpeed));
        updateModified();
    }

    public void setUpgradeStepDamage(double upgradeStepDamage) {
        this.upgradeStepDamage = Math.max(0.01, Math.min(10, upgradeStepDamage));
        updateModified();
    }

    public void setPriceMultiplierSize(double priceMultiplierSize) {
        this.priceMultiplierSize = Math.max(0.1, Math.min(10, priceMultiplierSize));
        updateModified();
    }

    public void setPriceMultiplierSpeed(double priceMultiplierSpeed) {
        this.priceMultiplierSpeed = Math.max(0.1, Math.min(10, priceMultiplierSpeed));
        updateModified();
    }

    public void setPriceMultiplierDamage(double priceMultiplierDamage) {
        this.priceMultiplierDamage = Math.max(0.1, Math.min(10, priceMultiplierDamage));
        updateModified();
    }

    // ========== МЕТОДЫ СТАТИСТИКИ ==========
    public void incrementExpansions() {
        totalExpansions++;
        lastExpansion = System.currentTimeMillis();
        updateModified();
    }

    public void incrementShrinks() {
        totalShrinks++;
        lastShrink = System.currentTimeMillis();
        updateModified();
    }

    public void incrementSpeedUpgrades() {
        totalSpeedUpgrades++;
        updateModified();
    }

    public void incrementSpeedDowngrades() {
        totalSpeedDowngrades++;
        updateModified();
    }

    public void incrementDamageUpgrades() {
        totalDamageUpgrades++;
        updateModified();
    }

    public void incrementDamageDowngrades() {
        totalDamageDowngrades++;
        updateModified();
    }

    public void addCurrencySpent(double amount) {
        totalCurrencySpent += amount;
        updateModified();
    }

    public void addCurrencyEarned(double amount) {
        totalCurrencyEarned += amount;
        updateModified();
    }

    public void addItemPrice(String item, double price) {
        worldItemPrices.put(item.toUpperCase(), Math.max(0.01, price));
        updateModified();
    }

    public void removeItemPrice(String item) {
        worldItemPrices.remove(item.toUpperCase());
        updateModified();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    private void updateModified() {
        this.lastModified = System.currentTimeMillis();
    }

    public boolean canExpand(double blocks) {
        double newSize = currentSize + blocks;
        return newSize <= playerMaxSize && newSize <= absoluteMaxSize;
    }

    public boolean canShrink(double blocks) {
        double newSize = currentSize - blocks;
        return newSize >= playerMinSize && newSize >= absoluteMinSize;
    }

    public boolean canUpgradeSpeed(boolean up) {
        if (up) {
            double newSpeed = currentSpeed + upgradeStepSpeed;
            return newSpeed <= playerMaxSpeed && newSpeed <= absoluteMaxSpeed;
        } else {
            double newSpeed = currentSpeed - upgradeStepSpeed;
            return newSpeed >= playerMinSpeed && newSpeed >= absoluteMinSpeed;
        }
    }

    public boolean canUpgradeDamage(boolean down) {
        if (down) {
            double newDamage = currentDamage - upgradeStepDamage;
            return newDamage >= playerMinDamage && newDamage >= absoluteMinDamage;
        } else {
            double newDamage = currentDamage + upgradeStepDamage;
            return newDamage <= playerMaxDamage && newDamage <= absoluteMaxDamage;
        }
    }

    public double getExpandCostFor(double blocks) {
        return expandCost * blocks * priceMultiplierSize;
    }

    public double getShrinkCostFor(double blocks) {
        return shrinkCost * blocks * priceMultiplierSize;
    }

    public double getSpeedUpgradeCost(boolean up) {
        return (up ? speedUpCost : speedDownCost) * priceMultiplierSpeed;
    }

    public double getDamageUpgradeCost(boolean down) {
        return (down ? damageDownCost : damageUpCost) * priceMultiplierDamage;
    }

    // ========== СЕРИАЛИЗАЦИЯ ==========
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        // Основные параметры
        data.put("worldName", worldName);
        data.put("enabled", enabled);
        data.put("upgradable", upgradable);
        data.put("shopEnabled", shopEnabled);

        // Текущие значения
        data.put("currentSize", currentSize);
        data.put("currentSpeed", currentSpeed);
        data.put("currentDamage", currentDamage);
        data.put("warningDistance", warningDistance);
        data.put("damageBuffer", damageBuffer);

        // Абсолютные лимиты
        data.put("absoluteMaxSize", absoluteMaxSize);
        data.put("absoluteMinSize", absoluteMinSize);
        data.put("absoluteMaxSpeed", absoluteMaxSpeed);
        data.put("absoluteMinSpeed", absoluteMinSpeed);
        data.put("absoluteMaxDamage", absoluteMaxDamage);
        data.put("absoluteMinDamage", absoluteMinDamage);

        // Лимиты игроков
        data.put("playerMaxSize", playerMaxSize);
        data.put("playerMinSize", playerMinSize);
        data.put("playerMaxSpeed", playerMaxSpeed);
        data.put("playerMinSpeed", playerMinSpeed);
        data.put("playerMaxDamage", playerMaxDamage);
        data.put("playerMinDamage", playerMinDamage);

        // Цены
        data.put("expandCost", expandCost);
        data.put("shrinkCost", shrinkCost);
        data.put("speedUpCost", speedUpCost);
        data.put("speedDownCost", speedDownCost);
        data.put("damageDownCost", damageDownCost);
        data.put("damageUpCost", damageUpCost);

        // Шаги улучшений
        data.put("upgradeStepSize", upgradeStepSize);
        data.put("upgradeStepSpeed", upgradeStepSpeed);
        data.put("upgradeStepDamage", upgradeStepDamage);

        // Множители
        data.put("priceMultiplierSize", priceMultiplierSize);
        data.put("priceMultiplierSpeed", priceMultiplierSpeed);
        data.put("priceMultiplierDamage", priceMultiplierDamage);

        // Статистика
        data.put("totalExpansions", totalExpansions);
        data.put("totalShrinks", totalShrinks);
        data.put("totalSpeedUpgrades", totalSpeedUpgrades);
        data.put("totalSpeedDowngrades", totalSpeedDowngrades);
        data.put("totalDamageUpgrades", totalDamageUpgrades);
        data.put("totalDamageDowngrades", totalDamageDowngrades);
        data.put("totalCurrencySpent", totalCurrencySpent);
        data.put("totalCurrencyEarned", totalCurrencyEarned);

        // Временные метки
        data.put("creationTime", creationTime);
        data.put("lastModified", lastModified);
        data.put("lastExpansion", lastExpansion);
        data.put("lastShrink", lastShrink);

        // Цены предметов
        data.put("worldItemPrices", worldItemPrices);

        return data;
    }

    public static WorldBorderData deserialize(Map<String, Object> data) {
        WorldBorderData borderData = new WorldBorderData();

        // Основные параметры
        borderData.worldName = (String) data.get("worldName");
        borderData.enabled = (boolean) data.getOrDefault("enabled", true);
        borderData.upgradable = (boolean) data.getOrDefault("upgradable", true);
        borderData.shopEnabled = (boolean) data.getOrDefault("shopEnabled", true);

        // Текущие значения
        borderData.currentSize = (double) data.getOrDefault("currentSize", 1000.0);
        borderData.currentSpeed = (double) data.getOrDefault("currentSpeed", 1.0);
        borderData.currentDamage = (double) data.getOrDefault("currentDamage", 2.0);
        borderData.warningDistance = (double) data.getOrDefault("warningDistance", 10.0);
        borderData.damageBuffer = (double) data.getOrDefault("damageBuffer", 5.0);

        // Абсолютные лимиты
        borderData.absoluteMaxSize = (double) data.getOrDefault("absoluteMaxSize", 30000.0);
        borderData.absoluteMinSize = (double) data.getOrDefault("absoluteMinSize", 50.0);
        borderData.absoluteMaxSpeed = (double) data.getOrDefault("absoluteMaxSpeed", 10.0);
        borderData.absoluteMinSpeed = (double) data.getOrDefault("absoluteMinSpeed", 0.1);
        borderData.absoluteMaxDamage = (double) data.getOrDefault("absoluteMaxDamage", 20.0);
        borderData.absoluteMinDamage = (double) data.getOrDefault("absoluteMinDamage", 0.0);

        // Лимиты игроков
        borderData.playerMaxSize = (double) data.getOrDefault("playerMaxSize", 30000.0);
        borderData.playerMinSize = (double) data.getOrDefault("playerMinSize", 50.0);
        borderData.playerMaxSpeed = (double) data.getOrDefault("playerMaxSpeed", 5.0);
        borderData.playerMinSpeed = (double) data.getOrDefault("playerMinSpeed", 0.5);
        borderData.playerMaxDamage = (double) data.getOrDefault("playerMaxDamage", 10.0);
        borderData.playerMinDamage = (double) data.getOrDefault("playerMinDamage", 0.5);

        // Цены
        borderData.expandCost = (double) data.getOrDefault("expandCost", 1.0);
        borderData.shrinkCost = (double) data.getOrDefault("shrinkCost", 0.5);
        borderData.speedUpCost = (double) data.getOrDefault("speedUpCost", 15.0);
        borderData.speedDownCost = (double) data.getOrDefault("speedDownCost", 5.0);
        borderData.damageDownCost = (double) data.getOrDefault("damageDownCost", 12.0);
        borderData.damageUpCost = (double) data.getOrDefault("damageUpCost", 8.0);

        // Шаги улучшений
        borderData.upgradeStepSize = (double) data.getOrDefault("upgradeStepSize", 1.0);
        borderData.upgradeStepSpeed = (double) data.getOrDefault("upgradeStepSpeed", 0.1);
        borderData.upgradeStepDamage = (double) data.getOrDefault("upgradeStepDamage", 0.1);

        // Множители
        borderData.priceMultiplierSize = (double) data.getOrDefault("priceMultiplierSize", 1.0);
        borderData.priceMultiplierSpeed = (double) data.getOrDefault("priceMultiplierSpeed", 1.0);
        borderData.priceMultiplierDamage = (double) data.getOrDefault("priceMultiplierDamage", 1.0);

        // Статистика
        borderData.totalExpansions = (int) data.getOrDefault("totalExpansions", 0);
        borderData.totalShrinks = (int) data.getOrDefault("totalShrinks", 0);
        borderData.totalSpeedUpgrades = (int) data.getOrDefault("totalSpeedUpgrades", 0);
        borderData.totalSpeedDowngrades = (int) data.getOrDefault("totalSpeedDowngrades", 0);
        borderData.totalDamageUpgrades = (int) data.getOrDefault("totalDamageUpgrades", 0);
        borderData.totalDamageDowngrades = (int) data.getOrDefault("totalDamageDowngrades", 0);
        borderData.totalCurrencySpent = (double) data.getOrDefault("totalCurrencySpent", 0.0);
        borderData.totalCurrencyEarned = (double) data.getOrDefault("totalCurrencyEarned", 0.0);

        // Временные метки
        borderData.creationTime = (long) data.getOrDefault("creationTime", System.currentTimeMillis());
        borderData.lastModified = (long) data.getOrDefault("lastModified", System.currentTimeMillis());
        borderData.lastExpansion = (long) data.getOrDefault("lastExpansion", 0L);
        borderData.lastShrink = (long) data.getOrDefault("lastShrink", 0L);

        // Цены предметов
        if (data.containsKey("worldItemPrices")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> itemPrices = (Map<String, Double>) data.get("worldItemPrices");
            borderData.worldItemPrices = itemPrices != null ? itemPrices : new HashMap<>();
        }

        return borderData;
    }
}