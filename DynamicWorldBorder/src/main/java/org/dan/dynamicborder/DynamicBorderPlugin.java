package org.dan.dynamicborder;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.dan.dynamicborder.managers.*;
import org.dan.dynamicborder.commands.BorderCommand;
import org.dan.dynamicborder.commands.BorderAdminCommand;
import org.dan.dynamicborder.listeners.PlayerListener;
import org.dan.dynamicborder.managers.ConfigManager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicBorderPlugin extends JavaPlugin {

    private static DynamicBorderPlugin instance;
    private Logger logger;

    // Менеджеры
    private BorderManager borderManager;
    private CurrencyManager currencyManager;
    private MultiplierManager multiplierManager;
    private WorldManager worldManager;
    private LimitManager limitManager;
    private EconomyManager economyManager;

    // Конфигурации
    private ConfigManager configManager;

    // Слушатели
    private PlayerListener playerListener;

    // Настройки
    private boolean debugMode = false;
    private long startupTime;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        startupTime = System.currentTimeMillis();

        try {
            logger.info("§a╔══════════════════════════════════════╗");
            logger.info("§a║    DynamicWorldBorder v3.0           ║");
            logger.info("§a║    Загрузка...                       ║");
            logger.info("§a╚══════════════════════════════════════╝");

            // Создание папок
            createFolders();

            // Загрузка конфигураций
            configManager = new ConfigManager(this);
            configManager.loadAllConfigs();

            // Инициализация менеджеров в правильном порядке
            worldManager = new WorldManager(this);
            borderManager = new BorderManager(this);
            currencyManager = new CurrencyManager(this);
            multiplierManager = new MultiplierManager(this);
            limitManager = new LimitManager(this);
            economyManager = new EconomyManager(this);

            // Регистрация команд
            BorderCommand borderCommand = new BorderCommand(this);
            BorderAdminCommand borderAdminCommand = new BorderAdminCommand(this);

            // Устанавливаем команды через plugin.yml
            this.getCommand("border").setExecutor(borderCommand);
            this.getCommand("border").setTabCompleter(borderCommand);
            this.getCommand("borderadmin").setExecutor(borderAdminCommand);
            this.getCommand("borderadmin").setTabCompleter(borderAdminCommand);

            // Регистрация слушателей
            playerListener = new PlayerListener(this);
            Bukkit.getPluginManager().registerEvents(playerListener, this);

            // Запуск задач
            startTasks();

            // Проверка обновлений (опционально)
            if (configManager.getMainConfig().getBoolean("settings.check-updates", true)) {
                // UpdateChecker может отсутствовать - делаем проверку
                try {
                    Class.forName("org.dan.dynamicborder.utils.UpdateChecker");
                    new org.dan.dynamicborder.utils.UpdateChecker(this, 123456).check();
                } catch (ClassNotFoundException e) {
                    logger.warning("UpdateChecker не найден, пропускаем проверку обновлений");
                }
            }

            // Инициализация миров
            borderManager.initializeAllWorlds();

            long loadTime = System.currentTimeMillis() - startupTime;
            logger.info("§a══════════════════════════════════════");
            logger.info("§aПлагин успешно загружен!");
            logger.info("§aВремя загрузки: §e" + loadTime + "мс");
            logger.info("§aМиров загружено: §e" + borderManager.getLoadedWorldsCount());
            logger.info("§a══════════════════════════════════════");

        } catch (Exception e) {
            logger.severe("§c╔══════════════════════════════════════╗");
            logger.severe("§c║    КРИТИЧЕСКАЯ ОШИБКА!              ║");
            logger.severe("§c╚══════════════════════════════════════╝");
            logger.log(Level.SEVERE, "Ошибка при загрузке плагина:", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        logger.info("§c╔══════════════════════════════════════╗");
        logger.info("§c║    DynamicWorldBorder v3.0           ║");
        logger.info("§c║    Выключение...                     ║");
        logger.info("§c╚══════════════════════════════════════╝");

        try {
            // Сохранение всех данных
            saveAllData();

            // Очистка слушателей
            if (playerListener != null) {
                HandlerList.unregisterAll(playerListener);
            }

            // Отмена всех задач
            Bukkit.getScheduler().cancelTasks(this);

            // Очистка менеджеров
            if (borderManager != null) {
                borderManager.cleanup();
            }
            if (currencyManager != null) {
                currencyManager.saveData();
            }
            if (multiplierManager != null) {
                multiplierManager.saveAllData();
            }

            logger.info("§aВсе данные успешно сохранены!");
            logger.info("§cПлагин выключен.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при выключении плагина:", e);
        }
    }

    private void createFolders() {
        // Основные папки
        String[] folders = {
                "worlds",
                "data/players",
                "data/statistics",
                "data/backups",
                "logs"
        };

        for (String folder : folders) {
            File dir = new File(getDataFolder(), folder);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logger.info("§aСоздана папка: §e" + folder);
                }
            }
        }
    }

    private void startTasks() {
        // Автосохранение
        int saveInterval = configManager.getMainConfig().getInt("settings.save-interval", 300);
        if (saveInterval > 0) {
            Bukkit.getScheduler().runTaskTimer(this, this::saveAllData,
                    saveInterval * 20L, saveInterval * 20L);
            logger.info("§aАвтосохранение каждые §e" + saveInterval + " §асекунд");
        }

        // Автобэкап
        int backupInterval = configManager.getMainConfig().getInt("settings.backup-interval", 3600);
        if (backupInterval > 0) {
            Bukkit.getScheduler().runTaskTimer(this, this::createAutoBackup,
                    backupInterval * 20L, backupInterval * 20L);
        }

        // Очистка кэша
        int cacheCleanup = configManager.getMainConfig().getInt("settings.cache-cleanup", 600);
        if (cacheCleanup > 0) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (multiplierManager != null) multiplierManager.cleanupCache();
                if (currencyManager != null) currencyManager.cleanupCache();
            }, cacheCleanup * 20L, cacheCleanup * 20L);
        }
    }

    private void saveAllData() {
        long start = System.currentTimeMillis();
        try {
            if (borderManager != null) borderManager.saveAllWorlds();
            if (currencyManager != null) currencyManager.saveData();
            if (multiplierManager != null) multiplierManager.saveAllData();
            if (configManager != null) configManager.saveAllConfigs();

            long time = System.currentTimeMillis() - start;
            if (debugMode) {
                logger.info("§aДанные сохранены за §e" + time + "мс");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Ошибка при автосохранении:", e);
        }
    }

    private void createAutoBackup() {
        if (configManager.getMainConfig().getBoolean("settings.auto-backup", true)) {
            try {
                String backupName = "auto_" + System.currentTimeMillis();
                // Здесь будет вызов метода бэкапа
                if (debugMode) {
                    logger.info("§aСоздан авто-бэкап: §e" + backupName);
                }
            } catch (Exception e) {
                logger.warning("§cОшибка создания авто-бэкапа");
            }
        }
    }

    // Геттеры
    public static DynamicBorderPlugin getInstance() {
        return instance;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public MultiplierManager getMultiplierManager() {
        return multiplierManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public LimitManager getLimitManager() {
        return limitManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        logger.info("§eРежим отладки " + (debugMode ? "§aвключен" : "§cвыключен"));
    }

    /**
     * Форматированное сообщение в лог
     */
    public void logInfo(String message) {
        logger.info("§7[§bDWB§7] §f" + message);
    }

    public void logWarning(String message) {
        logger.warning("§7[§bDWB§7] §e" + message);
    }

    public void logError(String message) {
        logger.severe("§7[§bDWB§7] §c" + message);
    }

    /**
     * Проверка разрешений с логированием
     */
    public boolean checkPermission(org.bukkit.command.CommandSender sender, String permission, boolean log) {
        boolean hasPerm = sender.hasPermission(permission);
        if (!hasPerm && log && debugMode) {
            logInfo("§cОтказ в доступе: " + sender.getName() + " -> " + permission);
        }
        return hasPerm;
    }
}