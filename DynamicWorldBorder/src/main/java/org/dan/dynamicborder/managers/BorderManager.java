package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.data.WorldBorderData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BorderManager {

    private final DynamicBorderPlugin plugin;
    private final Map<String, WorldBorderData> worldData = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSelectedWorld = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> borderTasks = new ConcurrentHashMap<>();

    // Константы абсолютных лимитов
    public static final double ABSOLUTE_MIN_SIZE = 1.0;
    public static final double ABSOLUTE_MAX_SIZE = 1000000.0;
    public static final double ABSOLUTE_MIN_SPEED = 0.01;
    public static final double ABSOLUTE_MAX_SPEED = 100.0;
    public static final double ABSOLUTE_MIN_DAMAGE = 0.0;
    public static final double ABSOLUTE_MAX_DAMAGE = 500.0;

    // Дефолтные значения
    public static final double DEFAULT_SIZE = 1000.0;
    public static final double DEFAULT_SPEED = 1.0;
    public static final double DEFAULT_DAMAGE = 2.0;
    public static final double DEFAULT_WARNING = 10.0;
    public static final double DEFAULT_BUFFER = 5.0;

    public BorderManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        loadWorldData();
    }

    /**
     * Инициализация всех миров
     */
    public void initializeAllWorlds() {
        plugin.logInfo("Инициализация границ миров...");

        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            WorldBorderData data = getOrCreateWorldData(worldName);

            if (data.isEnabled()) {
                applyWorldBorder(world, data);
                plugin.logInfo("  §7- §a" + worldName + "§7: §e" + data.getCurrentSize() + "§7 блоков");
            } else {
                plugin.logInfo("  §7- §c" + worldName + "§7: §oотключено");
            }
        }
    }

    /**
     * Применить настройки границы к миру
     */
    private void applyWorldBorder(World world, WorldBorderData data) {
        WorldBorder border = world.getWorldBorder();

        try {
            // Устанавливаем центр на текущую позицию игроков или по умолчанию
            if (border.getCenter().getX() == 0 && border.getCenter().getZ() == 0) {
                // Находим первого игрока в мире для центра
                Player firstPlayer = world.getPlayers().stream().findFirst().orElse(null);
                if (firstPlayer != null) {
                    Location loc = firstPlayer.getLocation();
                    border.setCenter(loc.getX(), loc.getZ());
                }
            }

            // Устанавливаем параметры
            border.setSize(data.getCurrentSize());
            border.setDamageAmount(data.getCurrentDamage());
            border.setDamageBuffer(data.getDamageBuffer());
            border.setWarningDistance((int) data.getWarningDistance());

            // Настраиваем скорость изменения (если граница двигается)
            if (border.getSize() != data.getCurrentSize()) {
                double difference = Math.abs(border.getSize() - data.getCurrentSize());
                long time = (long) Math.max(1, difference / data.getCurrentSpeed());
                border.setSize(data.getCurrentSize(), time);
            }

        } catch (Exception e) {
            plugin.logError("Ошибка применения границы для мира " + world.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Получить или создать данные мира
     */
    private WorldBorderData getOrCreateWorldData(String worldName) {
        return worldData.computeIfAbsent(worldName, name -> {
            // Загрузка из конфига если есть
            org.bukkit.configuration.ConfigurationSection worldConfig = plugin.getConfigManager().getWorldConfig(worldName);
            if (worldConfig != null && !worldConfig.getKeys(false).isEmpty()) {
                try {
                    // Преобразуем ConfigurationSection в Map для десериализации
                    // ПРОСТОЕ РЕШЕНИЕ: используем готовый метод getValues(false)
                    WorldBorderData savedData = WorldBorderData.deserialize(worldConfig.getValues(false));
                    // Убедимся что worldName установлен
                    savedData.setWorldName(worldName);
                    return savedData;

                } catch (Exception e) {
                    plugin.logWarning("Ошибка загрузки конфига мира " + worldName + ": " + e.getMessage());
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }
                    // Продолжаем создавать новые данные
                }
            }

            // ЕСЛИ МЫ ДОШЛИ СЮДА, ЗНАЧИТ:
            // 1. Нет конфига для этого мира ИЛИ
            // 2. Не удалось загрузить конфиг
            // СОЗДАЕМ НОВЫЕ ДАННЫЕ:
            WorldBorderData data = new WorldBorderData(name);

            // Настройки по умолчанию из config.yml
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getMainConfig();
            if (config.contains("worlds.defaults")) {
                data.setCurrentSize(config.getDouble("worlds.defaults.current-size", DEFAULT_SIZE));
                data.setCurrentSpeed(config.getDouble("worlds.defaults.current-speed", DEFAULT_SPEED));
                data.setCurrentDamage(config.getDouble("worlds.defaults.current-damage", DEFAULT_DAMAGE));
                data.setEnabled(config.getBoolean("worlds.defaults.enabled", true));
                data.setUpgradable(config.getBoolean("worlds.defaults.upgradable", true));

                // Лимиты
                if (config.contains("worlds.defaults.limits.size")) {
                    data.setPlayerMinSize(config.getDouble("worlds.defaults.limits.size.min", 50.0));
                    data.setPlayerMaxSize(config.getDouble("worlds.defaults.limits.size.max", 30000.0));
                }
                if (config.contains("worlds.defaults.limits.speed")) {
                    data.setPlayerMinSpeed(config.getDouble("worlds.defaults.limits.speed.min", 0.1));
                    data.setPlayerMaxSpeed(config.getDouble("worlds.defaults.limits.speed.max", 10.0));
                }
                if (config.contains("worlds.defaults.limits.damage")) {
                    data.setPlayerMinDamage(config.getDouble("worlds.defaults.limits.damage.min", 0.0));
                    data.setPlayerMaxDamage(config.getDouble("worlds.defaults.limits.damage.max", 20.0));
                }

                // Цены
                if (config.contains("worlds.defaults.costs")) {
                    data.setExpandCost(config.getDouble("worlds.defaults.costs.expand", 1.0));
                    data.setShrinkCost(config.getDouble("worlds.defaults.costs.shrink", 0.5));
                    data.setSpeedUpCost(config.getDouble("worlds.defaults.costs.speed-up", 15.0));
                    data.setSpeedDownCost(config.getDouble("worlds.defaults.costs.speed-down", 5.0));
                    data.setDamageDownCost(config.getDouble("worlds.defaults.costs.damage-down", 12.0));
                    data.setDamageUpCost(config.getDouble("worlds.defaults.costs.damage-up", 8.0));
                }
            }

            return data;
        });
    }

    /**
     * Расширить границу мира
     */
    public ExpandResult expandWorld(String worldName, double blocks, Player player) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) {
            return new ExpandResult(false, "Мир не найден");
        }

        if (!data.isEnabled()) {
            return new ExpandResult(false, "Система границы отключена для этого мира");
        }

        if (blocks <= 0) {
            return new ExpandResult(false, "Количество блоков должно быть положительным");
        }

        // Проверка лимитов
        if (!data.canExpand(blocks)) {
            double maxBlocks = data.getPlayerMaxSize() - data.getCurrentSize();
            return new ExpandResult(false, String.format("Максимальное расширение: %.1f блоков", maxBlocks));
        }

        // Расчет цены с учетом множителя
        double baseCost = data.getExpandCostFor(blocks);
        double finalCost = plugin.getMultiplierManager().getPrice(
                player, worldName, "expand", baseCost
        );

        // Проверка баланса
        if (!plugin.getCurrencyManager().hasEnough(player, finalCost)) {
            return new ExpandResult(false, String.format(
                    "Недостаточно валюты! Нужно: %.2f", finalCost
            ));
        }

        // Списание валюты
        if (!plugin.getCurrencyManager().withdrawBalance(player, finalCost)) {
            return new ExpandResult(false, "Ошибка списания валюты");
        }

        // Обновление данных
        double newSize = data.getCurrentSize() + blocks;
        data.setCurrentSize(newSize);
        data.incrementExpansions();
        data.addCurrencySpent(finalCost);

        // Обновление множителя
        plugin.getMultiplierManager().updateAfterPurchase(
                player, worldName, "expand", finalCost
        );

        // Применение к миру
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            double difference = Math.abs(border.getSize() - newSize);
            long time = (long) Math.max(1, difference / data.getCurrentSpeed());

            if (time <= 1) {
                border.setSize(newSize);
            } else {
                border.setSize(newSize, time);
            }
        }

        // Сохранение
        saveWorldData(data);

        return new ExpandResult(true, String.format(
                "Граница расширена на %.1f блоков. Новый размер: %.1f",
                blocks, newSize
        ), newSize, blocks, finalCost);
    }

    /**
     * Сузить границу мира
     */
    public ShrinkResult shrinkWorld(String worldName, double blocks, Player player) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) {
            return new ShrinkResult(false, "Мир не найден");
        }

        if (!data.isEnabled()) {
            return new ShrinkResult(false, "Система границы отключена для этого мира");
        }

        if (blocks <= 0) {
            return new ShrinkResult(false, "Количество блоков должно быть положительным");
        }

        // Проверка лимитов
        if (!data.canShrink(blocks)) {
            double maxBlocks = data.getCurrentSize() - data.getPlayerMinSize();
            return new ShrinkResult(false, String.format("Максимальное сужение: %.1f блоков", maxBlocks));
        }

        // Расчет цены с учетом множителя
        double baseCost = data.getShrinkCostFor(blocks);
        double finalCost = plugin.getMultiplierManager().getPrice(
                player, worldName, "shrink", baseCost
        );

        // Проверка баланса
        if (!plugin.getCurrencyManager().hasEnough(player, finalCost)) {
            return new ShrinkResult(false, String.format(
                    "Недостаточно валюты! Нужно: %.2f", finalCost
            ));
        }

        // Списание валюты
        if (!plugin.getCurrencyManager().withdrawBalance(player, finalCost)) {
            return new ShrinkResult(false, "Ошибка списания валюты");
        }

        // Обновление данных
        double newSize = data.getCurrentSize() - blocks;
        data.setCurrentSize(newSize);
        data.incrementShrinks();
        data.addCurrencySpent(finalCost);

        // Обновление множителя
        plugin.getMultiplierManager().updateAfterPurchase(
                player, worldName, "shrink", finalCost
        );

        // Применение к миру
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            double difference = Math.abs(border.getSize() - newSize);
            long time = (long) Math.max(1, difference / data.getCurrentSpeed());

            if (time <= 1) {
                border.setSize(newSize);
            } else {
                border.setSize(newSize, time);
            }
        }

        // Сохранение
        saveWorldData(data);

        return new ShrinkResult(true, String.format(
                "Граница сужена на %.1f блоков. Новый размер: %.1f",
                blocks, newSize
        ), newSize, blocks, finalCost);
    }

    /**
     * Улучшить скорость границы
     */
    public UpgradeResult upgradeSpeed(String worldName, boolean up, Player player) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) {
            return new UpgradeResult(false, "Мир не найден");
        }

        if (!data.isEnabled() || !data.isUpgradable()) {
            return new UpgradeResult(false, "Улучшения отключены для этого мира");
        }

        // Проверка лимитов
        if (!data.canUpgradeSpeed(up)) {
            String limit = up ?
                    String.format("Максимальная скорость: %.1f", data.getPlayerMaxSpeed()) :
                    String.format("Минимальная скорость: %.1f", data.getPlayerMinSpeed());
            return new UpgradeResult(false, limit);
        }

        // Расчет цены с учетом множителя
        double baseCost = data.getSpeedUpgradeCost(up);
        String priceType = up ? "speed-up" : "speed-down";
        double finalCost = plugin.getMultiplierManager().getPrice(
                player, worldName, priceType, baseCost
        );

        // Проверка баланса
        if (!plugin.getCurrencyManager().hasEnough(player, finalCost)) {
            return new UpgradeResult(false, String.format(
                    "Недостаточно валюты! Нужно: %.2f", finalCost
            ));
        }

        // Списание валюты
        if (!plugin.getCurrencyManager().withdrawBalance(player, finalCost)) {
            return new UpgradeResult(false, "Ошибка списания валюты");
        }

        // Обновление данных
        double change = up ? data.getUpgradeStepSpeed() : -data.getUpgradeStepSpeed();
        double newSpeed = data.getCurrentSpeed() + change;
        data.setCurrentSpeed(newSpeed);

        if (up) {
            data.incrementSpeedUpgrades();
        } else {
            data.incrementSpeedDowngrades();
        }

        data.addCurrencySpent(finalCost);

        // Обновление множителя
        plugin.getMultiplierManager().updateAfterPurchase(
                player, worldName, priceType, finalCost
        );

        // Сохранение
        saveWorldData(data);

        return new UpgradeResult(true, String.format(
                "Скорость %s на %.1f. Новая скорость: %.1f блоков/сек",
                up ? "увеличена" : "уменьшена",
                Math.abs(change),
                newSpeed
        ), newSpeed, Math.abs(change), finalCost, up);
    }

    /**
     * Улучшить урон границы
     */
    public UpgradeResult upgradeDamage(String worldName, boolean down, Player player) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) {
            return new UpgradeResult(false, "Мир не найден");
        }

        if (!data.isEnabled() || !data.isUpgradable()) {
            return new UpgradeResult(false, "Улучшения отключены для этого мира");
        }

        // Проверка лимитов
        if (!data.canUpgradeDamage(down)) {
            String limit = down ?
                    String.format("Минимальный урон: %.1f", data.getPlayerMinDamage()) :
                    String.format("Максимальный урон: %.1f", data.getPlayerMaxDamage());
            return new UpgradeResult(false, limit);
        }

        // Расчет цены с учетом множителя
        double baseCost = data.getDamageUpgradeCost(down);
        String priceType = down ? "damage-down" : "damage-up";
        double finalCost = plugin.getMultiplierManager().getPrice(
                player, worldName, priceType, baseCost
        );

        // Проверка баланса
        if (!plugin.getCurrencyManager().hasEnough(player, finalCost)) {
            return new UpgradeResult(false, String.format(
                    "Недостаточно валюты! Нужно: %.2f", finalCost
            ));
        }

        // Списание валюты
        if (!plugin.getCurrencyManager().withdrawBalance(player, finalCost)) {
            return new UpgradeResult(false, "Ошибка списания валюты");
        }

        // Обновление данных
        double change = down ? -data.getUpgradeStepDamage() : data.getUpgradeStepDamage();
        double newDamage = data.getCurrentDamage() + change;
        data.setCurrentDamage(newDamage);

        if (down) {
            data.incrementDamageDowngrades();
        } else {
            data.incrementDamageUpgrades();
        }

        data.addCurrencySpent(finalCost);

        // Обновление множителя
        plugin.getMultiplierManager().updateAfterPurchase(
                player, worldName, priceType, finalCost
        );

        // Применение к миру
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getWorldBorder().setDamageAmount(newDamage);
        }

        // Сохранение
        saveWorldData(data);

        return new UpgradeResult(true, String.format(
                "Урон %s на %.1f. Новый урон: %.1f урона/сек",
                down ? "уменьшен" : "увеличен",
                Math.abs(change),
                newDamage
        ), newDamage, Math.abs(change), finalCost, !down);
    }

    /**
     * Установить абсолютный лимит размера
     */
    public boolean setAbsoluteSizeLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка абсолютных границ
        if (max) {
            if (value < ABSOLUTE_MIN_SIZE || value > ABSOLUTE_MAX_SIZE) {
                return false;
            }
            data.setAbsoluteMaxSize(value);
        } else {
            if (value < ABSOLUTE_MIN_SIZE || value > ABSOLUTE_MAX_SIZE) {
                return false;
            }
            data.setAbsoluteMinSize(value);
        }

        saveWorldData(data);
        return true;
    }

    /**
     * Установить абсолютный лимит скорости
     */
    public boolean setAbsoluteSpeedLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка абсолютных границ
        if (max) {
            if (value < ABSOLUTE_MIN_SPEED || value > ABSOLUTE_MAX_SPEED) {
                return false;
            }
            data.setAbsoluteMaxSpeed(value);
        } else {
            if (value < ABSOLUTE_MIN_SPEED || value > ABSOLUTE_MAX_SPEED) {
                return false;
            }
            data.setAbsoluteMinSpeed(value);
        }

        saveWorldData(data);
        return true;
    }

    /**
     * Установить абсолютный лимит урона
     */
    public boolean setAbsoluteDamageLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка абсолютных границ
        if (max) {
            if (value < ABSOLUTE_MIN_DAMAGE || value > ABSOLUTE_MAX_DAMAGE) {
                return false;
            }
            data.setAbsoluteMaxDamage(value);
        } else {
            if (value < ABSOLUTE_MIN_DAMAGE || value > ABSOLUTE_MAX_DAMAGE) {
                return false;
            }
            data.setAbsoluteMinDamage(value);
        }

        saveWorldData(data);
        return true;
    }

    /**
     * Установить лимит для игроков
     */
    public boolean setPlayerSizeLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка в рамках абсолютных лимитов
        double absMin = data.getAbsoluteMinSize();
        double absMax = data.getAbsoluteMaxSize();

        if (value < absMin || value > absMax) {
            return false;
        }

        if (max) {
            data.setPlayerMaxSize(value);
        } else {
            data.setPlayerMinSize(value);
        }

        saveWorldData(data);
        return true;
    }

    /**
     * Установить лимит скорости для игроков
     */
    public boolean setPlayerSpeedLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка в рамках абсолютных лимитов
        double absMin = data.getAbsoluteMinSpeed();
        double absMax = data.getAbsoluteMaxSpeed();

        if (value < absMin || value > absMax) {
            return false;
        }

        if (max) {
            data.setPlayerMaxSpeed(value);
        } else {
            data.setPlayerMinSpeed(value);
        }

        saveWorldData(data);
        return true;
    }

    /**
     * Установить лимит урона для игроков
     */
    public boolean setPlayerDamageLimit(String worldName, boolean max, double value) {
        WorldBorderData data = worldData.get(worldName);
        if (data == null) return false;

        // Проверка в рамках абсолютных лимитов
        double absMin = data.getAbsoluteMinDamage();
        double absMax = data.getAbsoluteMaxDamage();

        if (value < absMin || value > absMax) {
            return false;
        }

        if (max) {
            data.setPlayerMaxDamage(value);
        } else {
            data.setPlayerMinDamage(value);
        }

        saveWorldData(data);
        return true;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void loadWorldData() {
        // Загрузка будет производиться по требованию
    }

    public void saveWorldData(WorldBorderData data) {
        try {
            // Сохраняем данные мира
            java.util.Map<String, Object> serializedData = data.serialize();
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

            for (java.util.Map.Entry<String, Object> entry : serializedData.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }

            java.io.File worldFile = new java.io.File(plugin.getDataFolder(), "worlds/" + data.getWorldName() + ".yml");
            if (!worldFile.getParentFile().exists()) {
                worldFile.getParentFile().mkdirs();
            }
            config.save(worldFile);
        } catch (Exception e) {
            plugin.logError("Ошибка сохранения данных мира " + data.getWorldName() + ": " + e.getMessage());
        }
    }

    public void saveAllWorlds() {
        for (WorldBorderData data : worldData.values()) {
            saveWorldData(data);
        }
    }

    public void setPlayerSelectedWorld(Player player, String worldName) {
        playerSelectedWorld.put(player.getUniqueId(), worldName);
    }

    public String getPlayerSelectedWorld(Player player) {
        return playerSelectedWorld.getOrDefault(player.getUniqueId(), "world");
    }

    public String getPlayerSelectedWorld(UUID uuid) {
        return playerSelectedWorld.getOrDefault(uuid, "world");
    }

    public WorldBorderData getWorldData(String worldName) {
        return worldData.get(worldName);
    }

    public Map<String, WorldBorderData> getAllWorldData() {
        return new HashMap<>(worldData);
    }

    public List<String> getAvailableWorlds() {
        return new ArrayList<>(worldData.keySet());
    }

    public int getLoadedWorldsCount() {
        return worldData.size();
    }

    public void cleanup() {
        for (BukkitTask task : borderTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        borderTasks.clear();
        worldData.clear();
        playerSelectedWorld.clear();
    }

    // ========== КЛАССЫ РЕЗУЛЬТАТОВ ==========

    public static class ExpandResult {
        private final boolean success;
        private final String message;
        private final double newSize;
        private final double blocks;
        private final double cost;

        public ExpandResult(boolean success, String message) {
            this(success, message, 0, 0, 0);
        }

        public ExpandResult(boolean success, String message, double newSize, double blocks, double cost) {
            this.success = success;
            this.message = message;
            this.newSize = newSize;
            this.blocks = blocks;
            this.cost = cost;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getNewSize() { return newSize; }
        public double getBlocks() { return blocks; }
        public double getCost() { return cost; }
    }

    public static class ShrinkResult {
        private final boolean success;
        private final String message;
        private final double newSize;
        private final double blocks;
        private final double cost;

        public ShrinkResult(boolean success, String message) {
            this(success, message, 0, 0, 0);
        }

        public ShrinkResult(boolean success, String message, double newSize, double blocks, double cost) {
            this.success = success;
            this.message = message;
            this.newSize = newSize;
            this.blocks = blocks;
            this.cost = cost;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getNewSize() { return newSize; }
        public double getBlocks() { return blocks; }
        public double getCost() { return cost; }
    }

    public static class UpgradeResult {
        private final boolean success;
        private final String message;
        private final double newValue;
        private final double change;
        private final double cost;
        private final boolean upgraded; // true если улучшение (вверх), false если ухудшение

        public UpgradeResult(boolean success, String message) {
            this(success, message, 0, 0, 0, false);
        }

        public UpgradeResult(boolean success, String message, double newValue, double change, double cost, boolean upgraded) {
            this.success = success;
            this.message = message;
            this.newValue = newValue;
            this.change = change;
            this.cost = cost;
            this.upgraded = upgraded;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getNewValue() { return newValue; }
        public double getChange() { return change; }
        public double getCost() { return cost; }
        public boolean isUpgraded() { return upgraded; }
    }
}