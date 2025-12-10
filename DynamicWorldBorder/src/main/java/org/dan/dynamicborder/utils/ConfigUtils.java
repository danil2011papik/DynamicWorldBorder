package org.dan.dynamicborder.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.dan.dynamicborder.DynamicBorderPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigUtils {

    /**
     * Загрузка YAML конфигурации
     */
    public static YamlConfiguration loadYaml(File file) {
        if (!file.exists()) {
            return new YamlConfiguration();
        }

        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            return new YamlConfiguration();
        }
    }

    /**
     * Сохранение YAML конфигурации
     */
    public static boolean saveYaml(YamlConfiguration config, File file) {
        try {
            // Создаем папки если нужно
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            config.save(file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Получить значение с дефолтом
     */
    public static <T> T getWithDefault(YamlConfiguration config, String path, T defaultValue) {
        if (config == null || !config.contains(path)) {
            return defaultValue;
        }

        try {
            Object value = config.get(path);
            if (value != null) {
                return (T) value;
            }
        } catch (ClassCastException e) {
            // Игнорируем и возвращаем дефолт
        }

        return defaultValue;
    }

    /**
     * Получить список строк с дефолтом
     */
    public static List<String> getStringList(YamlConfiguration config, String path, List<String> defaultValue) {
        if (config == null || !config.contains(path)) {
            return defaultValue != null ? defaultValue : new ArrayList<>();
        }

        List<String> list = config.getStringList(path);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Получить карту из конфига
     */
    public static Map<String, Object> getMap(YamlConfiguration config, String path) {
        Map<String, Object> map = new HashMap<>();

        if (config == null || !config.contains(path)) {
            return map;
        }

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.get(key));
            }
        }

        return map;
    }

    /**
     * Получить вложенную карту
     */
    public static Map<String, Map<String, Object>> getNestedMap(YamlConfiguration config, String path) {
        Map<String, Map<String, Object>> nestedMap = new HashMap<>();

        if (config == null || !config.contains(path)) {
            return nestedMap;
        }

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection != null) {
                    Map<String, Object> subMap = new HashMap<>();
                    for (String subKey : subSection.getKeys(false)) {
                        subMap.put(subKey, subSection.get(subKey));
                    }
                    nestedMap.put(key, subMap);
                }
            }
        }

        return nestedMap;
    }

    /**
     * Установить карту в конфиг
     */
    public static void setMap(YamlConfiguration config, String path, Map<String, Object> map) {
        if (config == null || map == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Установить вложенную карту
     */
    public static void setNestedMap(YamlConfiguration config, String path, Map<String, Map<String, Object>> nestedMap) {
        if (config == null || nestedMap == null) {
            return;
        }

        for (Map.Entry<String, Map<String, Object>> entry : nestedMap.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> subMap = entry.getValue();

            for (Map.Entry<String, Object> subEntry : subMap.entrySet()) {
                config.set(path + "." + key + "." + subEntry.getKey(), subEntry.getValue());
            }
        }
    }

    /**
     * Создать дефолтную конфигурацию если файл не существует
     */
    public static void createDefaultConfig(File file, Map<String, Object> defaults) {
        if (file.exists()) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();

        // Устанавливаем дефолтные значения
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        // Сохраняем
        saveYaml(config, file);
    }

    /**
     * Миграция старой конфигурации
     */
    public static boolean migrateConfig(File oldFile, File newFile, Map<String, String> keyMappings) {
        if (!oldFile.exists() || newFile.exists()) {
            return false;
        }

        try {
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
            YamlConfiguration newConfig = new YamlConfiguration();

            // Копируем значения с преобразованием ключей
            for (Map.Entry<String, String> mapping : keyMappings.entrySet()) {
                String oldKey = mapping.getKey();
                String newKey = mapping.getValue();

                if (oldConfig.contains(oldKey)) {
                    newConfig.set(newKey, oldConfig.get(oldKey));
                }
            }

            // Сохраняем новую конфигурацию
            saveYaml(newConfig, newFile);

            // Архивируем старую конфигурацию
            File backupFile = new File(oldFile.getParentFile(), oldFile.getName() + ".backup");
            oldFile.renameTo(backupFile);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверка целостности конфигурации
     */
    public static List<String> validateConfig(YamlConfiguration config, Map<String, Object> requiredKeys) {
        List<String> errors = new ArrayList<>();

        if (config == null || requiredKeys == null) {
            errors.add("Конфигурация или требуемые ключи не заданы");
            return errors;
        }

        for (Map.Entry<String, Object> entry : requiredKeys.entrySet()) {
            String key = entry.getKey();
            Object expectedType = entry.getValue();

            if (!config.contains(key)) {
                errors.add("Отсутствует ключ: " + key);
                continue;
            }

            Object value = config.get(key);

            // Проверка типа
            if (expectedType instanceof Class) {
                Class<?> expectedClass = (Class<?>) expectedType;
                if (!expectedClass.isInstance(value)) {
                    errors.add("Неверный тип для ключа " + key +
                            ": ожидается " + expectedClass.getSimpleName() +
                            ", получено " + (value != null ? value.getClass().getSimpleName() : "null"));
                }
            }
        }

        return errors;
    }

    /**
     * Автоисправление конфигурации
     */
    public static boolean autoFixConfig(YamlConfiguration config, Map<String, Object> defaults) {
        boolean fixed = false;

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!config.contains(key)) {
                config.set(key, defaultValue);
                fixed = true;
            } else {
                // Проверка типа
                Object currentValue = config.get(key);
                if (defaultValue != null && currentValue != null &&
                        !defaultValue.getClass().isInstance(currentValue)) {
                    config.set(key, defaultValue);
                    fixed = true;
                }
            }
        }

        return fixed;
    }

    /**
     * Создание бэкапа конфигурации
     */
    public static File createBackup(File configFile) {
        if (!configFile.exists()) {
            return null;
        }

        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupDir = new File(configFile.getParentFile(), "backups");

            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            File backupFile = new File(backupDir, configFile.getName() + "_" + timestamp + ".bak");

            // Копируем файл
            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Удаляем старые бэкапы (больше 10)
            cleanupOldBackups(backupDir, 10);

            return backupFile;

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Очистка старых бэкапов
     */
    private static void cleanupOldBackups(File backupDir, int keepCount) {
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return;
        }

        File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".bak"));
        if (backupFiles == null || backupFiles.length <= keepCount) {
            return;
        }

        // Сортируем по дате изменения (старые первыми)
        Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

        // Удаляем старые файлы
        for (int i = 0; i < backupFiles.length - keepCount; i++) {
            backupFiles[i].delete();
        }
    }

    /**
     * Создание дефолтной конфигурации плагина
     */
    public static void createPluginDefaults(DynamicBorderPlugin plugin) {
        File configDir = plugin.getDataFolder();

        // Основной config.yml
        Map<String, Object> mainDefaults = new LinkedHashMap<>();
        mainDefaults.put("settings.currency-name", "Граничных блоков");
        mainDefaults.put("settings.currency-symbol", "⧈");
        mainDefaults.put("settings.default-world", "world");
        mainDefaults.put("settings.save-interval", 300);
        mainDefaults.put("settings.backup-interval", 3600);
        mainDefaults.put("settings.check-updates", true);
        mainDefaults.put("settings.auto-backup", true);
        mainDefaults.put("settings.cache-cleanup", 600);

        mainDefaults.put("border.damage.enabled", true);
        mainDefaults.put("border.warning.enabled", true);
        mainDefaults.put("border.warning.distance", 10);
        mainDefaults.put("border.damage.cooldown", 1000);
        mainDefaults.put("border.info.show-on-join", true);

        mainDefaults.put("worlds.defaults.enabled", true);
        mainDefaults.put("worlds.defaults.upgradable", true);
        mainDefaults.put("worlds.defaults.current-size", 1000.0);
        mainDefaults.put("worlds.defaults.current-speed", 1.0);
        mainDefaults.put("worlds.defaults.current-damage", 2.0);
        mainDefaults.put("worlds.defaults.warning-distance", 10.0);
        mainDefaults.put("worlds.defaults.damage-buffer", 5.0);

        mainDefaults.put("worlds.defaults.limits.size.min", 50.0);
        mainDefaults.put("worlds.defaults.limits.size.max", 30000.0);
        mainDefaults.put("worlds.defaults.limits.speed.min", 0.1);
        mainDefaults.put("worlds.defaults.limits.speed.max", 10.0);
        mainDefaults.put("worlds.defaults.limits.damage.min", 0.0);
        mainDefaults.put("worlds.defaults.limits.damage.max", 20.0);

        mainDefaults.put("worlds.defaults.costs.expand", 1.0);
        mainDefaults.put("worlds.defaults.costs.shrink", 0.5);
        mainDefaults.put("worlds.defaults.costs.speed-up", 15.0);
        mainDefaults.put("worlds.defaults.costs.speed-down", 5.0);
        mainDefaults.put("worlds.defaults.costs.damage-down", 12.0);
        mainDefaults.put("worlds.defaults.costs.damage-up", 8.0);

        createDefaultConfig(new File(configDir, "config.yml"), mainDefaults);

        // multipliers.yml
        Map<String, Object> multiplierDefaults = new LinkedHashMap<>();
        multiplierDefaults.put("multipliers.expand.enabled", true);
        multiplierDefaults.put("multipliers.expand.type", "LINEAR");
        multiplierDefaults.put("multipliers.expand.base-value", 1.0);
        multiplierDefaults.put("multipliers.expand.step", 0.01);
        multiplierDefaults.put("multipliers.expand.custom-formula", "");
        multiplierDefaults.put("multipliers.expand.limits.min", 0.1);
        multiplierDefaults.put("multipliers.expand.limits.max", 10.0);
        multiplierDefaults.put("multipliers.expand.reset-schedule", "never");

        multiplierDefaults.put("multipliers.speed-up.enabled", true);
        multiplierDefaults.put("multipliers.speed-up.type", "EXPONENTIAL");
        multiplierDefaults.put("multipliers.speed-up.base-value", 1.05);
        multiplierDefaults.put("multipliers.speed-up.limits.min", 1.0);
        multiplierDefaults.put("multipliers.speed-up.limits.max", 5.0);

        createDefaultConfig(new File(configDir, "multipliers.yml"), multiplierDefaults);

        // messages.yml
        Map<String, Object> messageDefaults = new LinkedHashMap<>();
        messageDefaults.put("prefix", "&6[&eГраница&6]&f");
        messageDefaults.put("no-permission", "&cУ вас нет прав для этой команды!");
        messageDefaults.put("player-only", "&cЭта команда только для игроков!");
        messageDefaults.put("world-not-found", "&cМир не найден!");
        messageDefaults.put("world-disabled", "&cСистема границы отключена для этого мира!");
        messageDefaults.put("not-enough-currency", "&cНедостаточно %currency%! Нужно: %cost%");
        messageDefaults.put("upgrade-disabled", "&cЭто улучшение отключено администратором!");
        messageDefaults.put("limit-reached", "&cДостигнут лимит улучшения!");
        messageDefaults.put("purchase-success", "&aУспешная покупка! Потрачено: %cost%");
        messageDefaults.put("border-expanded", "&aГраница расширена на %blocks% блоков");
        messageDefaults.put("border-shrunk", "&aГраница сужена на %blocks% блоков");
        messageDefaults.put("speed-increased", "&aСкорость границы увеличена");
        messageDefaults.put("speed-decreased", "&aСкорость границы уменьшена");
        messageDefaults.put("damage-increased", "&aУрон границы увеличен");
        messageDefaults.put("damage-decreased", "&aУрон границы уменьшен");

        createDefaultConfig(new File(configDir, "messages.yml"), messageDefaults);

        // items.yml (предметы для продажи)
        Map<String, Object> itemDefaults = new LinkedHashMap<>();
        itemDefaults.put("items.DIAMOND", 100.0);
        itemDefaults.put("items.EMERALD", 50.0);
        itemDefaults.put("items.GOLD_INGOT", 25.0);
        itemDefaults.put("items.IRON_INGOT", 10.0);
        itemDefaults.put("items.COAL", 5.0);
        itemDefaults.put("items.NETHERITE_INGOT", 500.0);
        itemDefaults.put("items.NETHERITE_SCRAP", 100.0);
        itemDefaults.put("items.ANCIENT_DEBRIS", 200.0);
        itemDefaults.put("last-update", System.currentTimeMillis());

        createDefaultConfig(new File(configDir, "items.yml"), itemDefaults);

        plugin.logInfo("Созданы конфигурационные файлы по умолчанию");
    }
}