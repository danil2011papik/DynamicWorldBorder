package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final DynamicBorderPlugin plugin;
    private final Map<String, YamlConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    // Основные конфиги
    private YamlConfiguration mainConfig;
    private YamlConfiguration multipliersConfig;
    private YamlConfiguration worldsConfig;
    private YamlConfiguration messagesConfig;
    private YamlConfiguration itemsConfig;

    public ConfigManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        plugin.logInfo("Загрузка конфигураций...");

        // Основной config.yml
        loadConfig("config.yml");
        mainConfig = configs.get("config.yml");

        // multipliers.yml
        loadConfig("multipliers.yml");
        multipliersConfig = configs.get("multipliers.yml");

        // worlds.yml (если есть)
        loadConfig("worlds.yml");
        worldsConfig = configs.get("worlds.yml");

        // messages.yml
        loadConfig("messages.yml");
        messagesConfig = configs.get("messages.yml");

        // items.yml (цены предметов)
        loadConfig("items.yml");
        itemsConfig = configs.get("items.yml");

        // Загрузка мировых конфигов
        loadWorldConfigs();

        plugin.logInfo("Конфигурации загружены: " + configs.size() + " файлов");
    }

    private void loadConfig(String fileName) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);

            // Создание файла если нет
            if (!configFile.exists()) {
                plugin.saveResource(fileName, false);
                plugin.logInfo("Создан файл: " + fileName);
            }

            // Загрузка конфига
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Загрузка дефолтных значений из ресурсов
            InputStreamReader defaultStream = new InputStreamReader(
                    plugin.getResource(fileName), StandardCharsets.UTF_8
            );
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultStream);
                config.setDefaults(defaultConfig);
                config.options().copyDefaults(true);
            }

            configs.put(fileName, config);
            configFiles.put(fileName, configFile);

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки конфига: " + fileName);
            e.printStackTrace();
        }
    }

    private void loadWorldConfigs() {
        File worldsFolder = new File(plugin.getDataFolder(), "worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
            return;
        }

        File[] worldFiles = worldsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (worldFiles != null) {
            for (File worldFile : worldFiles) {
                try {
                    String worldName = worldFile.getName().replace(".yml", "");
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(worldFile);
                    configs.put("worlds/" + worldName + ".yml", config);
                    configFiles.put("worlds/" + worldName + ".yml", worldFile);
                } catch (Exception e) {
                    plugin.logError("Ошибка загрузки конфига мира: " + worldFile.getName());
                }
            }
        }
    }

    public void saveAllConfigs() {
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            try {
                File file = configFiles.get(entry.getKey());
                if (file != null) {
                    entry.getValue().save(file);
                }
            } catch (Exception e) {
                plugin.logError("Ошибка сохранения конфига: " + entry.getKey());
            }
        }
    }

    public void saveConfig(String configName) {
        try {
            YamlConfiguration config = configs.get(configName);
            File file = configFiles.get(configName);
            if (config != null && file != null) {
                config.save(file);
            }
        } catch (Exception e) {
            plugin.logError("Ошибка сохранения конфига: " + configName);
        }
    }

    public void reloadConfig(String configName) {
        File file = configFiles.get(configName);
        if (file != null && file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                configs.put(configName, config);
                plugin.logInfo("Конфиг перезагружен: " + configName);
            } catch (Exception e) {
                plugin.logError("Ошибка перезагрузки конфига: " + configName);
            }
        }
    }

    public void reloadAllConfigs() {
        loadAllConfigs();
        plugin.logInfo("Все конфиги перезагружены");
    }

    // Геттеры для конфигов
    public YamlConfiguration getMainConfig() {
        return mainConfig;
    }

    public YamlConfiguration getMultipliersConfig() {
        return multipliersConfig;
    }

    public YamlConfiguration getWorldsConfig() {
        return worldsConfig;
    }

    public YamlConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public YamlConfiguration getItemsConfig() {
        return itemsConfig;
    }

    // ConfigManager.java:
    public org.bukkit.configuration.ConfigurationSection getWorldConfig(String worldName) {
        YamlConfiguration config = configs.get("worlds/" + worldName + ".yml");
        if (config != null) {
            return config;  // ← Возвращаем саму конфигурацию
        }
        return null;
    }

    public FileConfiguration getConfig(String configName) {
        return configs.get(configName);
    }

    public void setConfig(String configName, YamlConfiguration config) {
        configs.put(configName, config);
    }

    /**
     * Получить сообщение из messages.yml с заменой переменных
     */
    public String getMessage(String path, Map<String, String> replacements) {
        String message = messagesConfig.getString(path, "");
        if (message.isEmpty()) return path;

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return message.replace("&", "§");
    }

    public String getMessage(String path) {
        return getMessage(path, null);
    }

    /**
     * Получить список сообщений
     */
    public java.util.List<String> getMessageList(String path) {
        java.util.List<String> messages = messagesConfig.getStringList(path);
        messages.replaceAll(msg -> msg.replace("&", "§"));
        return messages;
    }
}