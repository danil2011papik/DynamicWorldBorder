package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.data.PlayerMultiplierData;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MultiplierManager {

    private final DynamicBorderPlugin plugin;
    private final Map<String, MultiplierConfig> configs = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, PlayerMultiplierData>> playerData = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerCacheTime = new ConcurrentHashMap<>();
    private File multipliersFile;
    private File playerDataFile;

    private static final long CACHE_TIMEOUT = 300000; // 5 минут
    private static final double MIN_MULTIPLIER = 0.1;
    private static final double MAX_MULTIPLIER = 10.0;

    public enum MultiplierType {
        FIXED,      // Постоянный множитель: baseValue
        LINEAR,     // Линейный рост: baseValue + step * level
        EXPONENTIAL,// Экспоненциальный: baseValue ^ level
        CUSTOM      // Пользовательская формула
    }

    public static class MultiplierConfig {
        private String priceType;
        private boolean enabled = true;
        private MultiplierType type = MultiplierType.FIXED;
        private double baseValue = 1.0;
        private double step = 0.0;
        private String customFormula = "";
        private double minMultiplier = MIN_MULTIPLIER;
        private double maxMultiplier = MAX_MULTIPLIER;
        private String resetSchedule = "never"; // never, daily, weekly, monthly

        // Геттеры
        public String getPriceType() { return priceType; }
        public boolean isEnabled() { return enabled; }
        public MultiplierType getType() { return type; }
        public double getBaseValue() { return baseValue; }
        public double getStep() { return step; }
        public String getCustomFormula() { return customFormula; }
        public double getMinMultiplier() { return minMultiplier; }
        public double getMaxMultiplier() { return maxMultiplier; }
        public String getResetSchedule() { return resetSchedule; }

        // Сеттеры
        public void setPriceType(String priceType) { this.priceType = priceType; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setType(MultiplierType type) { this.type = type; }
        public void setBaseValue(double baseValue) { this.baseValue = baseValue; }
        public void setStep(double step) { this.step = step; }
        public void setCustomFormula(String customFormula) { this.customFormula = customFormula; }
        public void setMinMultiplier(double minMultiplier) { this.minMultiplier = minMultiplier; }
        public void setMaxMultiplier(double maxMultiplier) { this.maxMultiplier = maxMultiplier; }
        public void setResetSchedule(String resetSchedule) { this.resetSchedule = resetSchedule; }

        /**
         * Рассчитать множитель для уровня
         */
        public double calculateMultiplier(int level) {
            if (!enabled) return 1.0;

            double multiplier;
            switch (type) {
                case FIXED:
                    multiplier = baseValue;
                    break;

                case LINEAR:
                    multiplier = baseValue + (step * level);
                    break;

                case EXPONENTIAL:
                    multiplier = Math.pow(baseValue, level);
                    break;

                case CUSTOM:
                    multiplier = evaluateCustomFormula(level);
                    break;

                default:
                    multiplier = 1.0;
            }

            // Ограничение мин/макс
            return Math.max(minMultiplier, Math.min(maxMultiplier, multiplier));
        }

        private double evaluateCustomFormula(int level) {
            if (customFormula == null || customFormula.isEmpty()) {
                return 1.0;
            }

            try {
                String expression = customFormula
                        .replace("{level}", String.valueOf(level))
                        .replace("{base}", String.valueOf(baseValue))
                        .replace("{step}", String.valueOf(step))
                        .replace("{min}", String.valueOf(minMultiplier))
                        .replace("{max}", String.valueOf(maxMultiplier));

                return org.dan.dynamicborder.utils.MathUtils.evaluateExpression(expression);
            } catch (Exception e) {
                return 1.0;
            }
        }

        /**
         * Проверка необходимости сброса
         */
        public boolean shouldReset(long lastReset) {
            if ("never".equals(resetSchedule)) return false;

            long now = System.currentTimeMillis();
            long diff = now - lastReset;

            switch (resetSchedule) {
                case "daily":
                    return diff >= 86400000L; // 24 часа
                case "weekly":
                    return diff >= 604800000L; // 7 дней
                case "monthly":
                    return diff >= 2592000000L; // 30 дней
                default:
                    return false;
            }
        }

        /**
         * Симуляция множителей для N уровней
         */
        public List<Double> simulate(int levels) {
            List<Double> multipliers = new ArrayList<>();
            for (int i = 0; i < levels; i++) {
                multipliers.add(calculateMultiplier(i));
            }
            return multipliers;
        }
    }

    public MultiplierManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;

        // Регистрация сериализации
        ConfigurationSerialization.registerClass(PlayerMultiplierData.class);

        loadConfigs();
        loadPlayerData();
    }

    /**
     * Получить цену с учетом множителя для игрока
     */
    public double getPrice(Player player, String worldName, String priceType, double basePrice) {
        if (player == null) return basePrice;

        MultiplierConfig config = configs.get(priceType);
        if (config == null || !config.isEnabled()) {
            return basePrice;
        }

        UUID uuid = player.getUniqueId();
        PlayerMultiplierData data = getPlayerMultiplierData(uuid, worldName, priceType);
        if (data == null) {
            // Первая покупка
            return basePrice * config.calculateMultiplier(0);
        }

        // Проверка сброса
        if (config.shouldReset(data.getLastResetTime())) {
            resetPlayerProgress(player, worldName, priceType);
            return basePrice * config.calculateMultiplier(0);
        }

        return basePrice * config.calculateMultiplier(data.getLevel());
    }

    /**
     * Обновить прогресс после покупки
     */
    public void updateAfterPurchase(Player player, String worldName, String priceType, double paidPrice) {
        UUID uuid = player.getUniqueId();
        String key = createKey(worldName, priceType);

        Map<String, PlayerMultiplierData> playerMultipliers = playerData
                .computeIfAbsent(uuid, k -> new HashMap<>());

        PlayerMultiplierData data = playerMultipliers.get(key);
        if (data == null) {
            data = new PlayerMultiplierData();
            data.setPriceType(priceType);
            data.setWorldName(worldName);
            data.setLevel(0);
            data.setLastResetTime(System.currentTimeMillis());
        }

        data.setLevel(data.getLevel() + 1);
        data.setLastPrice(paidPrice);
        data.setTotalSpent(data.getTotalSpent() + paidPrice);
        data.setLastPurchaseTime(System.currentTimeMillis());

        playerMultipliers.put(key, data);
        playerCacheTime.put(uuid, System.currentTimeMillis());

        // Автосохранение через 5 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> savePlayerData(uuid), 100L);
    }

    /**
     * Получить данные множителя игрока
     */
    private PlayerMultiplierData getPlayerMultiplierData(UUID uuid, String worldName, String priceType) {
        // Проверка кэша
        Long cacheTime = playerCacheTime.get(uuid);
        if (cacheTime != null && System.currentTimeMillis() - cacheTime > CACHE_TIMEOUT) {
            // Кэш устарел, загружаем заново
            loadPlayerData(uuid);
        }

        Map<String, PlayerMultiplierData> playerMultipliers = playerData.get(uuid);
        if (playerMultipliers == null) {
            return null;
        }

        String key = createKey(worldName, priceType);
        return playerMultipliers.get(key);
    }

    /**
     * Сбросить прогресс игрока
     */
    public boolean resetPlayerProgress(Player player, String worldName, String priceType) {
        UUID uuid = player.getUniqueId();
        String key = createKey(worldName, priceType);

        Map<String, PlayerMultiplierData> playerMultipliers = playerData.get(uuid);
        if (playerMultipliers == null) return false;

        PlayerMultiplierData data = playerMultipliers.get(key);
        if (data == null) return false;

        data.setLevel(0);
        data.setLastResetTime(System.currentTimeMillis());
        playerCacheTime.put(uuid, System.currentTimeMillis());

        savePlayerData(uuid);
        return true;
    }

    /**
     * Сбросить все прогрессы игрока в мире
     */
    public boolean resetAllPlayerProgress(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerMultiplierData> playerMultipliers = playerData.get(uuid);
        if (playerMultipliers == null) return false;

        boolean reset = false;
        String prefix = worldName + ":";

        for (Map.Entry<String, PlayerMultiplierData> entry : playerMultipliers.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().setLevel(0);
                entry.getValue().setLastResetTime(System.currentTimeMillis());
                reset = true;
            }
        }

        if (reset) {
            playerCacheTime.put(uuid, System.currentTimeMillis());
            savePlayerData(uuid);
        }

        return reset;
    }

    /**
     * Получить информацию о множителе
     */
    public MultiplierInfo getMultiplierInfo(String priceType) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return null;

        return new MultiplierInfo(
                config.getPriceType(),
                config.isEnabled(),
                config.getType(),
                config.getBaseValue(),
                config.getStep(),
                config.getCustomFormula(),
                config.getMinMultiplier(),
                config.getMaxMultiplier(),
                config.getResetSchedule()
        );
    }

    /**
     * Установить тип множителя
     */
    public boolean setMultiplierType(String priceType, MultiplierType type) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) {
            config = new MultiplierConfig();
            config.setPriceType(priceType);
            configs.put(priceType, config);
        }

        config.setType(type);
        saveConfigs();
        return true;
    }

    /**
     * Установить значение множителя
     */
    public boolean setMultiplierValue(String priceType, double value) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setBaseValue(Math.max(0.1, Math.min(10.0, value)));
        saveConfigs();
        return true;
    }

    /**
     * Установить шаг роста
     */
    public boolean setMultiplierStep(String priceType, double step) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setStep(Math.max(0.0, Math.min(1.0, step)));
        saveConfigs();
        return true;
    }

    /**
     * Установить кастомную формулу
     */
    public boolean setMultiplierFormula(String priceType, String formula) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setCustomFormula(formula);
        saveConfigs();
        return true;
    }

    /**
     * Установить лимиты множителя
     */
    public boolean setMultiplierLimits(String priceType, double min, double max) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setMinMultiplier(Math.max(0.1, Math.min(1.0, min)));
        config.setMaxMultiplier(Math.max(1.0, Math.min(10.0, max)));
        saveConfigs();
        return true;
    }

    /**
     * Установить расписание сброса
     */
    public boolean setMultiplierResetSchedule(String priceType, String schedule) {
        List<String> validSchedules = Arrays.asList("never", "daily", "weekly", "monthly");
        if (!validSchedules.contains(schedule.toLowerCase())) {
            return false;
        }

        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setResetSchedule(schedule.toLowerCase());
        saveConfigs();
        return true;
    }

    /**
     * Включить/выключить множитель
     */
    public boolean setMultiplierEnabled(String priceType, boolean enabled) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) return false;

        config.setEnabled(enabled);
        saveConfigs();
        return true;
    }

    /**
     * Симуляция роста цен
     */
    public List<String> simulatePrices(String priceType, int levels, double basePrice) {
        MultiplierConfig config = configs.get(priceType);
        if (config == null) {
            return Collections.singletonList("§cМножитель не найден: " + priceType);
        }

        List<String> result = new ArrayList<>();
        result.add("§6══════════════════════════════════════");
        result.add("§eСимуляция множителей: §a" + priceType);
        result.add("§7Тип: §f" + config.getType());
        result.add("§7Базовый множитель: §f" + config.getBaseValue());

        if (config.getType() == MultiplierType.LINEAR) {
            result.add("§7Шаг роста: §f" + config.getStep());
        } else if (config.getType() == MultiplierType.CUSTOM) {
            result.add("§7Формула: §f" + config.getCustomFormula());
        }

        result.add("§7Лимиты: §f" + config.getMinMultiplier() + " - " + config.getMaxMultiplier());
        result.add("§7Сброс: §f" + config.getResetSchedule());
        result.add("§6──────────────────────────────────────");

        double totalSpent = 0;
        for (int i = 0; i < levels; i++) {
            double multiplier = config.calculateMultiplier(i);
            double price = basePrice * multiplier;
            totalSpent += price;

            String line = String.format("§eУр. %2d: §f×%.3f §7= §a%.2f §7(Итого: §a%.2f§7)",
                    i + 1, multiplier, price, totalSpent);
            result.add(line);
        }

        result.add("§6══════════════════════════════════════");
        return result;
    }

    /**
     * Получить статистику игрока
     */
    public PlayerMultiplierStats getPlayerStats(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerMultiplierData> playerMultipliers = playerData.get(uuid);
        if (playerMultipliers == null) return new PlayerMultiplierStats();

        PlayerMultiplierStats stats = new PlayerMultiplierStats();
        String prefix = worldName + ":";

        for (Map.Entry<String, PlayerMultiplierData> entry : playerMultipliers.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                stats.addMultiplierData(entry.getValue());
            }
        }

        return stats;
    }

    // ========== ЗАГРУЗКА И СОХРАНЕНИЕ ==========

    private void loadConfigs() {
        multipliersFile = new File(plugin.getDataFolder(), "multipliers.yml");
        if (!multipliersFile.exists()) {
            plugin.saveResource("multipliers.yml", false);
            plugin.logInfo("Создан файл multipliers.yml");
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(multipliersFile);

            if (config.contains("multipliers")) {
                for (String priceType : config.getConfigurationSection("multipliers").getKeys(false)) {
                    String path = "multipliers." + priceType;

                    MultiplierConfig multiplierConfig = new MultiplierConfig();
                    multiplierConfig.setPriceType(priceType);
                    multiplierConfig.setEnabled(config.getBoolean(path + ".enabled", true));

                    String typeStr = config.getString(path + ".type", "FIXED");
                    try {
                        multiplierConfig.setType(MultiplierType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        multiplierConfig.setType(MultiplierType.FIXED);
                    }

                    multiplierConfig.setBaseValue(config.getDouble(path + ".base-value", 1.0));
                    multiplierConfig.setStep(config.getDouble(path + ".step", 0.0));
                    multiplierConfig.setCustomFormula(config.getString(path + ".custom-formula", ""));
                    multiplierConfig.setMinMultiplier(config.getDouble(path + ".limits.min", MIN_MULTIPLIER));
                    multiplierConfig.setMaxMultiplier(config.getDouble(path + ".limits.max", MAX_MULTIPLIER));
                    multiplierConfig.setResetSchedule(config.getString(path + ".reset-schedule", "never"));

                    configs.put(priceType, multiplierConfig);
                }
            }

            plugin.logInfo("Загружено множителей: " + configs.size());

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки multipliers.yml: " + e.getMessage());
        }
    }

    private void loadPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "data/player_multipliers.yml");
        if (!playerDataFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);

            if (config.contains("players")) {
                for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Map<String, PlayerMultiplierData> playerMultipliers = new HashMap<>();

                        if (config.contains("players." + uuidStr + ".multipliers")) {
                            for (String key : config.getConfigurationSection("players." + uuidStr + ".multipliers").getKeys(false)) {
                                PlayerMultiplierData data = (PlayerMultiplierData) config.get(
                                        "players." + uuidStr + ".multipliers." + key
                                );
                                if (data != null) {
                                    playerMultipliers.put(key, data);
                                }
                            }
                        }

                        playerData.put(uuid, playerMultipliers);
                    } catch (IllegalArgumentException e) {
                        plugin.logWarning("Неверный UUID в player_multipliers.yml: " + uuidStr);
                    }
                }
            }

            plugin.logInfo("Загружено данных игроков: " + playerData.size());

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки player_multipliers.yml: " + e.getMessage());
        }
    }

    private void loadPlayerData(UUID uuid) {
        if (playerDataFile == null || !playerDataFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            String uuidStr = uuid.toString();

            if (config.contains("players." + uuidStr + ".multipliers")) {
                Map<String, PlayerMultiplierData> playerMultipliers = new HashMap<>();

                for (String key : config.getConfigurationSection("players." + uuidStr + ".multipliers").getKeys(false)) {
                    PlayerMultiplierData data = (PlayerMultiplierData) config.get(
                            "players." + uuidStr + ".multipliers." + key
                    );
                    if (data != null) {
                        playerMultipliers.put(key, data);
                    }
                }

                playerData.put(uuid, playerMultipliers);
            }

            playerCacheTime.put(uuid, System.currentTimeMillis());

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки данных игрока " + uuid + ": " + e.getMessage());
        }
    }

    private void saveConfigs() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<String, MultiplierConfig> entry : configs.entrySet()) {
                String path = "multipliers." + entry.getKey();
                MultiplierConfig mc = entry.getValue();

                config.set(path + ".enabled", mc.isEnabled());
                config.set(path + ".type", mc.getType().name());
                config.set(path + ".base-value", mc.getBaseValue());
                config.set(path + ".step", mc.getStep());
                config.set(path + ".custom-formula", mc.getCustomFormula());
                config.set(path + ".limits.min", mc.getMinMultiplier());
                config.set(path + ".limits.max", mc.getMaxMultiplier());
                config.set(path + ".reset-schedule", mc.getResetSchedule());
            }

            config.save(multipliersFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения multipliers.yml: " + e.getMessage());
        }
    }

    public void savePlayerData(UUID uuid) {
        if (playerDataFile == null) {
            playerDataFile = new File(plugin.getDataFolder(), "data/player_multipliers.yml");
        }

        try {
            YamlConfiguration config = playerDataFile.exists() ?
                    YamlConfiguration.loadConfiguration(playerDataFile) : new YamlConfiguration();

            Map<String, PlayerMultiplierData> playerMultipliers = playerData.get(uuid);
            if (playerMultipliers != null) {
                String uuidStr = uuid.toString();

                for (Map.Entry<String, PlayerMultiplierData> entry : playerMultipliers.entrySet()) {
                    config.set("players." + uuidStr + ".multipliers." + entry.getKey(), entry.getValue());
                }

                config.set("players." + uuidStr + ".last-save", System.currentTimeMillis());
            }

            config.save(playerDataFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения данных игрока " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAllData() {
        saveConfigs();

        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<UUID, Map<String, PlayerMultiplierData>> playerEntry : playerData.entrySet()) {
                String uuidStr = playerEntry.getKey().toString();

                for (Map.Entry<String, PlayerMultiplierData> multiplierEntry : playerEntry.getValue().entrySet()) {
                    config.set("players." + uuidStr + ".multipliers." + multiplierEntry.getKey(),
                            multiplierEntry.getValue());
                }

                config.set("players." + uuidStr + ".last-save", System.currentTimeMillis());
            }

            if (playerDataFile != null) {
                config.save(playerDataFile);
            }

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения всех данных множителей: " + e.getMessage());
        }
    }

    public void cleanupCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = playerCacheTime.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now - entry.getValue() > CACHE_TIMEOUT) {
                // Сохраняем данные перед очисткой
                savePlayerData(entry.getKey());
                iterator.remove();
            }
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private String createKey(String worldName, String priceType) {
        return worldName + ":" + priceType;
    }

    // ========== КЛАССЫ ДАННЫХ ==========

    public static class MultiplierInfo {
        private final String priceType;
        private final boolean enabled;
        private final MultiplierType type;
        private final double baseValue;
        private final double step;
        private final String customFormula;
        private final double minMultiplier;
        private final double maxMultiplier;
        private final String resetSchedule;

        public MultiplierInfo(String priceType, boolean enabled, MultiplierType type,
                              double baseValue, double step, String customFormula,
                              double minMultiplier, double maxMultiplier, String resetSchedule) {
            this.priceType = priceType;
            this.enabled = enabled;
            this.type = type;
            this.baseValue = baseValue;
            this.step = step;
            this.customFormula = customFormula;
            this.minMultiplier = minMultiplier;
            this.maxMultiplier = maxMultiplier;
            this.resetSchedule = resetSchedule;
        }

        public String getPriceType() { return priceType; }
        public boolean isEnabled() { return enabled; }
        public MultiplierType getType() { return type; }
        public double getBaseValue() { return baseValue; }
        public double getStep() { return step; }
        public String getCustomFormula() { return customFormula; }
        public double getMinMultiplier() { return minMultiplier; }
        public double getMaxMultiplier() { return maxMultiplier; }
        public String getResetSchedule() { return resetSchedule; }
    }

    public static class PlayerMultiplierStats {
        private final Map<String, PlayerMultiplierData> multipliers = new HashMap<>();
        private double totalSpent = 0;
        private int totalPurchases = 0;

        public void addMultiplierData(PlayerMultiplierData data) {
            multipliers.put(data.getPriceType(), data);
            totalSpent += data.getTotalSpent();
            totalPurchases += data.getLevel();
        }

        public Map<String, PlayerMultiplierData> getMultipliers() { return multipliers; }
        public double getTotalSpent() { return totalSpent; }
        public int getTotalPurchases() { return totalPurchases; }

        public PlayerMultiplierData getMultiplierData(String priceType) {
            return multipliers.get(priceType);
        }

        public int getLevel(String priceType) {
            PlayerMultiplierData data = multipliers.get(priceType);
            return data != null ? data.getLevel() : 0;
        }

        public double getLastPrice(String priceType) {
            PlayerMultiplierData data = multipliers.get(priceType);
            return data != null ? data.getLastPrice() : 0;
        }
    }
}