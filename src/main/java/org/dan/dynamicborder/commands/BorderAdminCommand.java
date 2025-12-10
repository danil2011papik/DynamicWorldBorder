package org.dan.dynamicborder.commands;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.managers.BorderManager;
import org.dan.dynamicborder.managers.CurrencyManager;
import org.dan.dynamicborder.managers.MultiplierManager;
import org.dan.dynamicborder.data.WorldBorderData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class BorderAdminCommand implements CommandExecutor, TabCompleter {

    private final DynamicBorderPlugin plugin;
    private final BorderManager borderManager;
    private final CurrencyManager currencyManager;
    private final MultiplierManager multiplierManager;

    // Доступные подкоманды для админов
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "world", "limit", "multiplier", "price", "item", "shop",
            "player", "balance", "reload", "save", "stats", "debug",
            "backup", "version", "help"
    );

    // Подкоманды для world
    private static final List<String> WORLD_SUBCOMMANDS = Arrays.asList(
            "enable", "disable", "upgradable", "set", "speed", "damage",
            "info", "list", "reset", "copy", "create", "remove"
    );

    // Подкоманды для limit
    private static final List<String> LIMIT_SUBCOMMANDS = Arrays.asList(
            "size", "speed", "damage", "info", "all", "reset", "apply",
            "compare", "validate", "player"
    );

    // Подкоманды для multiplier
    private static final List<String> MULTIPLIER_SUBCOMMANDS = Arrays.asList(
            "type", "value", "step", "formula", "limits", "info",
            "simulate", "reset", "enable", "disable"
    );

    // Типы множителей
    private static final List<String> MULTIPLIER_TYPES = Arrays.asList(
            "FIXED", "LINEAR", "EXPONENTIAL", "CUSTOM"
    );

    // Типы цен для множителей
    private static final List<String> PRICE_TYPES = Arrays.asList(
            "expand", "shrink", "speed-up", "speed-down",
            "damage-down", "damage-up"
    );

    public BorderAdminCommand(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        this.borderManager = plugin.getBorderManager();
        this.currencyManager = plugin.getCurrencyManager();
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("dynamicborder.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды!");
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender, 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            switch (subCommand) {
                case "world":
                    handleWorld(sender, args);
                    break;

                case "limit":
                    handleLimit(sender, args);
                    break;

                case "multiplier":
                    handleMultiplier(sender, args);
                    break;

                case "price":
                    handlePrice(sender, args);
                    break;

                case "item":
                    handleItem(sender, args);
                    break;

                case "shop":
                    handleShop(sender, args);
                    break;

                case "player":
                    handlePlayer(sender, args);
                    break;

                case "balance":
                    handleBalanceAdmin(sender, args);
                    break;

                case "reload":
                    handleReload(sender);
                    break;

                case "save":
                    handleSave(sender);
                    break;

                case "stats":
                    handleStatsAdmin(sender, args);
                    break;

                case "debug":
                    handleDebug(sender, args);
                    break;

                case "backup":
                    handleBackup(sender, args);
                    break;

                case "version":
                    handleVersionAdmin(sender);
                    break;

                case "help":
                    handleHelpAdmin(sender, args);
                    break;

                default:
                    sender.sendMessage("§cНеизвестная команда. Используйте §e/borderadmin help");
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage("§cПроизошла ошибка при выполнении команды!");
            plugin.logError("Ошибка в команде /borderadmin " + subCommand + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return true;
    }

    // ========== ОБРАБОТЧИКИ КОМАНД ==========

    private void handleWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world <enable|disable|upgradable|set|speed|damage|info|list|reset|copy>");
            return;
        }

        String worldSubCmd = args[1].toLowerCase();

        switch (worldSubCmd) {
            case "enable":
                handleWorldEnable(sender, args);
                break;

            case "disable":
                handleWorldDisable(sender, args);
                break;

            case "upgradable":
                handleWorldUpgradable(sender, args);
                break;

            case "set":
                handleWorldSet(sender, args);
                break;

            case "speed":
                handleWorldSpeed(sender, args);
                break;

            case "damage":
                handleWorldDamage(sender, args);
                break;

            case "info":
                handleWorldInfo(sender, args);
                break;

            case "list":
                handleWorldList(sender);
                break;

            case "reset":
                handleWorldReset(sender, args);
                break;

            case "copy":
                handleWorldCopy(sender, args);
                break;

            case "create":
                handleWorldCreate(sender, args);
                break;

            case "remove":
                handleWorldRemove(sender, args);
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + worldSubCmd);
                break;
        }
    }

    private void handleWorldEnable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world enable <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.setEnabled(true);
        borderManager.saveWorldData(data);
        sender.sendMessage("§aСистема границы включена для мира §e" + worldName);
    }

    private void handleWorldDisable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world disable <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.setEnabled(false);
        borderManager.saveWorldData(data);
        sender.sendMessage("§aСистема границы отключена для мира §e" + worldName);
    }

    private void handleWorldUpgradable(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world upgradable <мир> <on/off>");
            return;
        }

        String worldName = args[2];
        String state = args[3].toLowerCase();
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        boolean upgradable = state.equals("on");
        data.setUpgradable(upgradable);
        borderManager.saveWorldData(data);

        sender.sendMessage("§aУлучшения " + (upgradable ? "включены" : "отключены") +
                " для мира §e" + worldName);
    }

    private void handleWorldSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world set <мир> <размер>");
            return;
        }

        String worldName = args[2];
        double size;

        try {
            size = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для размера!");
            return;
        }

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.setCurrentSize(size);
        borderManager.saveWorldData(data);

        // Применение к миру
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getWorldBorder().setSize(size);
        }

        sender.sendMessage("§aРазмер границы установлен на §e" + size + " §aблоков для мира §e" + worldName);
    }

    private void handleWorldSpeed(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world speed <мир> <скорость>");
            return;
        }

        String worldName = args[2];
        double speed;

        try {
            speed = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для скорости!");
            return;
        }

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.setCurrentSpeed(speed);
        borderManager.saveWorldData(data);

        sender.sendMessage("§aСкорость границы установлена на §e" + speed + " §aблоков/сек для мира §e" + worldName);
    }

    private void handleWorldDamage(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world damage <мир> <урон>");
            return;
        }

        String worldName = args[2];
        double damage;

        try {
            damage = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для урона!");
            return;
        }

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.setCurrentDamage(damage);
        borderManager.saveWorldData(data);

        // Применение к миру
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getWorldBorder().setDamageAmount(damage);
        }

        sender.sendMessage("§aУрон границы установлен на §e" + damage + " §aурона/сек для мира §e" + worldName);
    }

    private void handleWorldInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world info <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendWorldInfo(sender, data);
    }

    private void sendWorldInfo(CommandSender sender, WorldBorderData data) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eИнформация о мире: §a" + data.getWorldName());
        sender.sendMessage("§6══════════════════════════════════════");

        sender.sendMessage("§7Статус: " + (data.isEnabled() ? "§aВключен" : "§cВыключен"));
        sender.sendMessage("§7Улучшения: " + (data.isUpgradable() ? "§aРазрешены" : "§cЗапрещены"));
        sender.sendMessage("§7Магазин: " + (data.isShopEnabled() ? "§aОткрыт" : "§cЗакрыт"));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Текущие значения:");
        sender.sendMessage("  §7Размер: §e" + String.format("%.1f", data.getCurrentSize()));
        sender.sendMessage("  §7Скорость: §e" + String.format("%.2f", data.getCurrentSpeed()));
        sender.sendMessage("  §7Урон: §e" + String.format("%.1f", data.getCurrentDamage()));
        sender.sendMessage("  §7Предупреждение: §e" + String.format("%.0f", data.getWarningDistance()));
        sender.sendMessage("  §7Буфер урона: §e" + String.format("%.1f", data.getDamageBuffer()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Абсолютные лимиты:");
        sender.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getAbsoluteMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getAbsoluteMaxSize()));
        sender.sendMessage("  §7Скорость: §a" + String.format("%.2f", data.getAbsoluteMinSpeed()) +
                "§7 - §a" + String.format("%.2f", data.getAbsoluteMaxSpeed()));
        sender.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getAbsoluteMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getAbsoluteMaxDamage()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Лимиты для игроков:");
        sender.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getPlayerMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getPlayerMaxSize()));
        sender.sendMessage("  §7Скорость: §a" + String.format("%.1f", data.getPlayerMinSpeed()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxSpeed()));
        sender.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getPlayerMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxDamage()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Цены:");
        sender.sendMessage("  §7Расширение: §a" + data.getExpandCost() + "/блок");
        sender.sendMessage("  §7Сужение: §a" + data.getShrinkCost() + "/блок");
        sender.sendMessage("  §7Ускорение: §a" + data.getSpeedUpCost());
        sender.sendMessage("  §7Замедление: §a" + data.getSpeedDownCost());
        sender.sendMessage("  §7-Урон: §a" + data.getDamageDownCost());
        sender.sendMessage("  §7+Урон: §a" + data.getDamageUpCost());

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleWorldList(CommandSender sender) {
        Map<String, WorldBorderData> allWorlds = borderManager.getAllWorldData();

        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eСписок миров §7(" + allWorlds.size() + ")");
        sender.sendMessage("§6══════════════════════════════════════");

        for (Map.Entry<String, WorldBorderData> entry : allWorlds.entrySet()) {
            WorldBorderData data = entry.getValue();
            String status = data.isEnabled() ? "§a✓" : "§c✗";
            String upgradable = data.isUpgradable() ? "§aУ" : "§cН";

            sender.sendMessage(String.format("%s %s§7: §e%.0f§7б §8| %s §8| §7С:§e%.1f §8| §7У:§e%.1f",
                    status, entry.getKey(), data.getCurrentSize(), upgradable,
                    data.getCurrentSpeed(), data.getCurrentDamage()));
        }

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleWorldReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world reset <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        // Сброс к дефолтным значениям
        data.setCurrentSize(1000.0);
        data.setCurrentSpeed(1.0);
        data.setCurrentDamage(2.0);
        data.setEnabled(true);
        data.setUpgradable(true);
        data.setShopEnabled(true);

        // Сброс статистики
        // Примечание: В реальной реализации нужно добавить методы сброса статистики в WorldBorderData

        borderManager.saveWorldData(data);

        // Применение к миру
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getWorldBorder().setSize(1000.0);
            world.getWorldBorder().setDamageAmount(2.0);
        }

        sender.sendMessage("§aМир §e" + worldName + "§a сброшен к настройкам по умолчанию");
    }

    private void handleWorldCopy(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world copy <исходный_мир> <целевой_мир>");
            return;
        }

        String sourceWorld = args[2];
        String targetWorld = args[3];

        WorldBorderData sourceData = borderManager.getWorldData(sourceWorld);
        if (sourceData == null) {
            sender.sendMessage("§cИсходный мир §e" + sourceWorld + "§c не найден!");
            return;
        }

        // Создаем копию данных
        WorldBorderData targetData = new WorldBorderData(targetWorld);

        // Копируем все настройки
        targetData.setEnabled(sourceData.isEnabled());
        targetData.setUpgradable(sourceData.isUpgradable());
        targetData.setShopEnabled(sourceData.isShopEnabled());

        targetData.setCurrentSize(sourceData.getCurrentSize());
        targetData.setCurrentSpeed(sourceData.getCurrentSpeed());
        targetData.setCurrentDamage(sourceData.getCurrentDamage());
        targetData.setWarningDistance(sourceData.getWarningDistance());
        targetData.setDamageBuffer(sourceData.getDamageBuffer());

        // Копируем лимиты
        targetData.setAbsoluteMaxSize(sourceData.getAbsoluteMaxSize());
        targetData.setAbsoluteMinSize(sourceData.getAbsoluteMinSize());
        targetData.setAbsoluteMaxSpeed(sourceData.getAbsoluteMaxSpeed());
        targetData.setAbsoluteMinSpeed(sourceData.getAbsoluteMinSpeed());
        targetData.setAbsoluteMaxDamage(sourceData.getAbsoluteMaxDamage());
        targetData.setAbsoluteMinDamage(sourceData.getAbsoluteMinDamage());

        targetData.setPlayerMaxSize(sourceData.getPlayerMaxSize());
        targetData.setPlayerMinSize(sourceData.getPlayerMinSize());
        targetData.setPlayerMaxSpeed(sourceData.getPlayerMaxSpeed());
        targetData.setPlayerMinSpeed(sourceData.getPlayerMinSpeed());
        targetData.setPlayerMaxDamage(sourceData.getPlayerMaxDamage());
        targetData.setPlayerMinDamage(sourceData.getPlayerMinDamage());

        // Копируем цены
        targetData.setExpandCost(sourceData.getExpandCost());
        targetData.setShrinkCost(sourceData.getShrinkCost());
        targetData.setSpeedUpCost(sourceData.getSpeedUpCost());
        targetData.setSpeedDownCost(sourceData.getSpeedDownCost());
        targetData.setDamageDownCost(sourceData.getDamageDownCost());
        targetData.setDamageUpCost(sourceData.getDamageUpCost());

        // Копируем шаги улучшений
        targetData.setUpgradeStepSize(sourceData.getUpgradeStepSize());
        targetData.setUpgradeStepSpeed(sourceData.getUpgradeStepSpeed());
        targetData.setUpgradeStepDamage(sourceData.getUpgradeStepDamage());

        // Копируем множители
        targetData.setPriceMultiplierSize(sourceData.getPriceMultiplierSize());
        targetData.setPriceMultiplierSpeed(sourceData.getPriceMultiplierSpeed());
        targetData.setPriceMultiplierDamage(sourceData.getPriceMultiplierDamage());

        // Копируем цены предметов
        targetData.getWorldItemPrices().putAll(sourceData.getWorldItemPrices());

        // Сохраняем
        borderManager.saveWorldData(targetData);

        // Применяем к миру если он существует
        org.bukkit.World world = Bukkit.getWorld(targetWorld);
        if (world != null && targetData.isEnabled()) {
            world.getWorldBorder().setSize(targetData.getCurrentSize());
            world.getWorldBorder().setDamageAmount(targetData.getCurrentDamage());
        }

        sender.sendMessage("§aНастройки скопированы из §e" + sourceWorld + "§a в §e" + targetWorld);
    }

    private void handleWorldCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world create <имя_мира>");
            return;
        }

        String worldName = args[2];

        // Проверяем, не существует ли уже
        if (borderManager.getWorldData(worldName) != null) {
            sender.sendMessage("§cМир §e" + worldName + "§c уже существует!");
            return;
        }

        // Создаем новый мир с настройками по умолчанию
        WorldBorderData newWorld = new WorldBorderData(worldName);
        borderManager.saveWorldData(newWorld);

        sender.sendMessage("§aСоздан новый мир: §e" + worldName);
        sender.sendMessage("§7Используйте §e/borderadmin world set " + worldName + " <размер>§7 для настройки");
    }

    private void handleWorldRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin world remove <имя_мира>");
            return;
        }

        String worldName = args[2];

        // Проверяем существование
        if (borderManager.getWorldData(worldName) == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        // Удаляем данные
        // В реальной реализации нужно удалить файл конфига
        sender.sendMessage("§aМир §e" + worldName + "§a удален");
        sender.sendMessage("§cВнимание: Функция удаления требует дополнительной реализации!");
    }

    private void handleLimit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit <size|speed|damage|info|all|reset|apply|compare|validate|player>");
            return;
        }

        String limitSubCmd = args[1].toLowerCase();

        switch (limitSubCmd) {
            case "size":
                handleLimitSize(sender, args);
                break;

            case "speed":
                handleLimitSpeed(sender, args);
                break;

            case "damage":
                handleLimitDamage(sender, args);
                break;

            case "info":
                handleLimitInfo(sender, args);
                break;

            case "all":
                handleLimitAll(sender);
                break;

            case "reset":
                handleLimitReset(sender, args);
                break;

            case "apply":
                handleLimitApply(sender, args);
                break;

            case "compare":
                handleLimitCompare(sender, args);
                break;

            case "validate":
                handleLimitValidate(sender, args);
                break;

            case "player":
                handleLimitPlayer(sender, args);
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + limitSubCmd);
                break;
        }
    }

    private void handleLimitSize(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit size <maxabs|minabs|max|min|playermax|playermin> <мир> <значение>");
            return;
        }

        String type = args[2].toLowerCase();
        String worldName = args[3];
        double value;

        try {
            value = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
            return;
        }

        boolean success = false;
        String message = "";

        switch (type) {
            case "maxabs":
                success = borderManager.setAbsoluteSizeLimit(worldName, true, value);
                message = "Абсолютный максимальный размер";
                break;

            case "minabs":
                success = borderManager.setAbsoluteSizeLimit(worldName, false, value);
                message = "Абсолютный минимальный размер";
                break;

            case "max":
                success = borderManager.setPlayerSizeLimit(worldName, true, value);
                message = "Максимальный размер для игроков";
                break;

            case "min":
                success = borderManager.setPlayerSizeLimit(worldName, false, value);
                message = "Минимальный размер для игроков";
                break;

            case "playermax":
                success = borderManager.setPlayerSizeLimit(worldName, true, value);
                message = "Максимальный размер для игроков";
                break;

            case "playermin":
                success = borderManager.setPlayerSizeLimit(worldName, false, value);
                message = "Минимальный размер для игроков";
                break;

            default:
                sender.sendMessage("§cНеизвестный тип: §e" + type);
                return;
        }

        if (success) {
            sender.sendMessage("§a" + message + " установлен на §e" + value + " §aдля мира §e" + worldName);
        } else {
            sender.sendMessage("§cОшибка установки лимита!");
        }
    }

    private void handleLimitSpeed(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit speed <maxabs|minabs|max|min|playermax|playermin> <мир> <значение>");
            return;
        }

        String type = args[2].toLowerCase();
        String worldName = args[3];
        double value;

        try {
            value = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
            return;
        }

        boolean success = false;
        String message = "";

        switch (type) {
            case "maxabs":
                success = borderManager.setAbsoluteSpeedLimit(worldName, true, value);
                message = "Абсолютная максимальная скорость";
                break;

            case "minabs":
                success = borderManager.setAbsoluteSpeedLimit(worldName, false, value);
                message = "Абсолютная минимальная скорость";
                break;

            case "max":
            case "playermax":
                success = borderManager.setPlayerSpeedLimit(worldName, true, value);
                message = "Максимальная скорость для игроков";
                break;

            case "min":
            case "playermin":
                success = borderManager.setPlayerSpeedLimit(worldName, false, value);
                message = "Минимальная скорость для игроков";
                break;

            default:
                sender.sendMessage("§cНеизвестный тип: §e" + type);
                return;
        }

        if (success) {
            sender.sendMessage("§a" + message + " установлена на §e" + value + " §aдля мира §e" + worldName);
        } else {
            sender.sendMessage("§cОшибка установки лимита!");
        }
    }

    private void handleLimitDamage(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit damage <maxabs|minabs|max|min|playermax|playermin> <мир> <значение>");
            return;
        }

        String type = args[2].toLowerCase();
        String worldName = args[3];
        double value;

        try {
            value = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
            return;
        }

        boolean success = false;
        String message = "";

        switch (type) {
            case "maxabs":
                success = borderManager.setAbsoluteDamageLimit(worldName, true, value);
                message = "Абсолютный максимальный урон";
                break;

            case "minabs":
                success = borderManager.setAbsoluteDamageLimit(worldName, false, value);
                message = "Абсолютный минимальный урон";
                break;

            case "max":
            case "playermax":
                success = borderManager.setPlayerDamageLimit(worldName, true, value);
                message = "Максимальный урон для игроков";
                break;

            case "min":
            case "playermin":
                success = borderManager.setPlayerDamageLimit(worldName, false, value);
                message = "Минимальный урон для игроков";
                break;

            default:
                sender.sendMessage("§cНеизвестный тип: §e" + type);
                return;
        }

        if (success) {
            sender.sendMessage("§a" + message + " установлен на §e" + value + " §aдля мира §e" + worldName);
        } else {
            sender.sendMessage("§cОшибка установки лимита!");
        }
    }

    private void handleLimitInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit info <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendLimitInfo(sender, data);
    }

    private void sendLimitInfo(CommandSender sender, WorldBorderData data) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eЛимиты мира: §a" + data.getWorldName());
        sender.sendMessage("§6══════════════════════════════════════");

        sender.sendMessage("§cАбсолютные лимиты (невозможно превысить):");
        sender.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getAbsoluteMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getAbsoluteMaxSize()));
        sender.sendMessage("  §7Скорость: §a" + String.format("%.2f", data.getAbsoluteMinSpeed()) +
                "§7 - §a" + String.format("%.2f", data.getAbsoluteMaxSpeed()));
        sender.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getAbsoluteMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getAbsoluteMaxDamage()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§eЛимиты для игроков:");
        sender.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getPlayerMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getPlayerMaxSize()));
        sender.sendMessage("  §7Скорость: §a" + String.format("%.1f", data.getPlayerMinSpeed()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxSpeed()));
        sender.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getPlayerMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxDamage()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Текущие значения:");
        sender.sendMessage("  §7Размер: §e" + String.format("%.0f", data.getCurrentSize()) +
                " §7(" + getPercentage(data.getCurrentSize(), data.getPlayerMinSize(), data.getPlayerMaxSize()) + "%)");
        sender.sendMessage("  §7Скорость: §e" + String.format("%.2f", data.getCurrentSpeed()) +
                " §7(" + getPercentage(data.getCurrentSpeed(), data.getPlayerMinSpeed(), data.getPlayerMaxSpeed()) + "%)");
        sender.sendMessage("  §7Урон: §e" + String.format("%.1f", data.getCurrentDamage()) +
                " §7(" + getPercentage(data.getCurrentDamage(), data.getPlayerMinDamage(), data.getPlayerMaxDamage()) + "%)");

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private String getPercentage(double current, double min, double max) {
        if (max <= min) return "100";
        double percentage = ((current - min) / (max - min)) * 100;
        return String.format("%.0f", Math.min(100, Math.max(0, percentage)));
    }

    private void handleLimitAll(CommandSender sender) {
        Map<String, WorldBorderData> allWorlds = borderManager.getAllWorldData();

        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eЛимиты всех миров §7(" + allWorlds.size() + ")");
        sender.sendMessage("§6══════════════════════════════════════");

        for (Map.Entry<String, WorldBorderData> entry : allWorlds.entrySet()) {
            WorldBorderData data = entry.getValue();

            sender.sendMessage("§e" + entry.getKey() + ":");
            sender.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getPlayerMinSize()) +
                    "§7-§a" + String.format("%.0f", data.getPlayerMaxSize()) +
                    " §7(Абс: §a" + String.format("%.0f", data.getAbsoluteMinSize()) +
                    "§7-§a" + String.format("%.0f", data.getAbsoluteMaxSize()) + "§7)");
            sender.sendMessage("  §7Скорость: §a" + String.format("%.1f", data.getPlayerMinSpeed()) +
                    "§7-§a" + String.format("%.1f", data.getPlayerMaxSpeed()));
            sender.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getPlayerMinDamage()) +
                    "§7-§a" + String.format("%.1f", data.getPlayerMaxDamage()));

            if (!entry.getKey().equals(allWorlds.keySet().toArray()[allWorlds.size() - 1])) {
                sender.sendMessage("§6──────────────────────────────────────");
            }
        }

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleLimitReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit reset <мир> [тип]");
            sender.sendMessage("§cТипы: size, speed, damage, all");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        String type = args.length >= 4 ? args[3].toLowerCase() : "all";

        switch (type) {
            case "size":
                data.setPlayerMinSize(50.0);
                data.setPlayerMaxSize(30000.0);
                data.setAbsoluteMinSize(1.0);
                data.setAbsoluteMaxSize(1000000.0);
                sender.sendMessage("§aLimits размера сброшены для мира §e" + worldName);
                break;

            case "speed":
                data.setPlayerMinSpeed(0.5);
                data.setPlayerMaxSpeed(10.0);
                data.setAbsoluteMinSpeed(0.01);
                data.setAbsoluteMaxSpeed(100.0);
                sender.sendMessage("§aLimits скорости сброшены для мира §e" + worldName);
                break;

            case "damage":
                data.setPlayerMinDamage(0.5);
                data.setPlayerMaxDamage(20.0);
                data.setAbsoluteMinDamage(0.0);
                data.setAbsoluteMaxDamage(500.0);
                sender.sendMessage("§aLimits урона сброшены для мира §e" + worldName);
                break;

            case "all":
                data.setPlayerMinSize(50.0);
                data.setPlayerMaxSize(30000.0);
                data.setAbsoluteMinSize(1.0);
                data.setAbsoluteMaxSize(1000000.0);

                data.setPlayerMinSpeed(0.5);
                data.setPlayerMaxSpeed(10.0);
                data.setAbsoluteMinSpeed(0.01);
                data.setAbsoluteMaxSpeed(100.0);

                data.setPlayerMinDamage(0.5);
                data.setPlayerMaxDamage(20.0);
                data.setAbsoluteMinDamage(0.0);
                data.setAbsoluteMaxDamage(500.0);

                sender.sendMessage("§aВсе лимиты сброшены для мира §e" + worldName);
                break;

            default:
                sender.sendMessage("§cНеизвестный тип: §e" + type);
                return;
        }

        borderManager.saveWorldData(data);
    }

    private void handleLimitApply(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit apply <исходный_мир> <целевой_мир>");
            return;
        }

        String sourceWorld = args[2];
        String targetWorld = args[3];

        WorldBorderData sourceData = borderManager.getWorldData(sourceWorld);
        WorldBorderData targetData = borderManager.getWorldData(targetWorld);

        if (sourceData == null) {
            sender.sendMessage("§cИсходный мир §e" + sourceWorld + "§c не найден!");
            return;
        }

        if (targetData == null) {
            sender.sendMessage("§cЦелевой мир §e" + targetWorld + "§c не найден!");
            return;
        }

        // Копируем лимиты
        targetData.setAbsoluteMaxSize(sourceData.getAbsoluteMaxSize());
        targetData.setAbsoluteMinSize(sourceData.getAbsoluteMinSize());
        targetData.setAbsoluteMaxSpeed(sourceData.getAbsoluteMaxSpeed());
        targetData.setAbsoluteMinSpeed(sourceData.getAbsoluteMinSpeed());
        targetData.setAbsoluteMaxDamage(sourceData.getAbsoluteMaxDamage());
        targetData.setAbsoluteMinDamage(sourceData.getAbsoluteMinDamage());

        targetData.setPlayerMaxSize(sourceData.getPlayerMaxSize());
        targetData.setPlayerMinSize(sourceData.getPlayerMinSize());
        targetData.setPlayerMaxSpeed(sourceData.getPlayerMaxSpeed());
        targetData.setPlayerMinSpeed(sourceData.getPlayerMinSpeed());
        targetData.setPlayerMaxDamage(sourceData.getPlayerMaxDamage());
        targetData.setPlayerMinDamage(sourceData.getPlayerMinDamage());

        borderManager.saveWorldData(targetData);

        sender.sendMessage("§aLimits применены из §e" + sourceWorld + "§a в §e" + targetWorld);
    }

    private void handleLimitCompare(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit compare <мир1> <мир2>");
            return;
        }

        String world1 = args[2];
        String world2 = args[3];

        WorldBorderData data1 = borderManager.getWorldData(world1);
        WorldBorderData data2 = borderManager.getWorldData(world2);

        if (data1 == null || data2 == null) {
            sender.sendMessage("§cОдин из миров не найден!");
            return;
        }

        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eСравнение лимитов: §a" + world1 + " §7vs §a" + world2);
        sender.sendMessage("§6══════════════════════════════════════");

        compareAndSend(sender, "Размер (игр.)",
                data1.getPlayerMinSize(), data1.getPlayerMaxSize(),
                data2.getPlayerMinSize(), data2.getPlayerMaxSize());

        compareAndSend(sender, "Скорость (игр.)",
                data1.getPlayerMinSpeed(), data1.getPlayerMaxSpeed(),
                data2.getPlayerMinSpeed(), data2.getPlayerMaxSpeed());

        compareAndSend(sender, "Урон (игр.)",
                data1.getPlayerMinDamage(), data1.getPlayerMaxDamage(),
                data2.getPlayerMinDamage(), data2.getPlayerMaxDamage());

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void compareAndSend(CommandSender sender, String name,
                                double min1, double max1, double min2, double max2) {
        String color1 = (min1 == min2 && max1 == max2) ? "§a" : "§e";
        String color2 = (min1 == min2 && max1 == max2) ? "§a" : "§e";

        sender.sendMessage("§7" + name + ":");
        sender.sendMessage("  §7Мир 1: " + color1 + String.format("%.1f", min1) + "§7-§a" + String.format("%.1f", max1));
        sender.sendMessage("  §7Мир 2: " + color2 + String.format("%.1f", min2) + "§7-§a" + String.format("%.1f", max2));
    }

    private void handleLimitValidate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit validate <мир>");
            return;
        }

        String worldName = args[2];
        WorldBorderData data = borderManager.getWorldData(worldName);

        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        List<String> errors = new ArrayList<>();

        // Проверка абсолютных лимитов
        if (data.getAbsoluteMinSize() >= data.getAbsoluteMaxSize()) {
            errors.add("Абс. мин. размер ≥ абс. макс. размера");
        }

        if (data.getAbsoluteMinSpeed() >= data.getAbsoluteMaxSpeed()) {
            errors.add("Абс. мин. скорость ≥ абс. макс. скорости");
        }

        if (data.getAbsoluteMinDamage() > data.getAbsoluteMaxDamage()) {
            errors.add("Абс. мин. урон > абс. макс. урона");
        }

        // Проверка лимитов игроков
        if (data.getPlayerMinSize() < data.getAbsoluteMinSize()) {
            errors.add("Игр. мин. размер < абс. мин. размера");
        }

        if (data.getPlayerMaxSize() > data.getAbsoluteMaxSize()) {
            errors.add("Игр. макс. размер > абс. макс. размера");
        }

        if (data.getPlayerMinSize() >= data.getPlayerMaxSize()) {
            errors.add("Игр. мин. размер ≥ игр. макс. размера");
        }

        // Проверка текущих значений
        if (data.getCurrentSize() < data.getPlayerMinSize()) {
            errors.add("Текущий размер < игр. мин. размера");
        }

        if (data.getCurrentSize() > data.getPlayerMaxSize()) {
            errors.add("Текущий размер > игр. макс. размера");
        }

        if (errors.isEmpty()) {
            sender.sendMessage("§a✓ Все лимиты корректны для мира §e" + worldName);
        } else {
            sender.sendMessage("§cОбнаружены ошибки в лимитах мира §e" + worldName + ":");
            for (String error : errors) {
                sender.sendMessage("§7- §c" + error);
            }
        }
    }

    private void handleLimitPlayer(CommandSender sender, String[] args) {
        if (args.length < 6) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin limit player <size|speed|damage> <max|min> <мир> <значение>");
            return;
        }

        String param = args[2].toLowerCase();
        String limitType = args[3].toLowerCase();
        String worldName = args[4];
        double value;

        try {
            value = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
            return;
        }

        boolean max = limitType.equals("max");
        boolean success = false;
        String message = "";

        switch (param) {
            case "size":
                success = borderManager.setPlayerSizeLimit(worldName, max, value);
                message = "Размер для игроков";
                break;

            case "speed":
                success = borderManager.setPlayerSpeedLimit(worldName, max, value);
                message = "Скорость для игроков";
                break;

            case "damage":
                success = borderManager.setPlayerDamageLimit(worldName, max, value);
                message = "Урон для игроков";
                break;

            default:
                sender.sendMessage("§cНеизвестный параметр: §e" + param);
                return;
        }

        if (success) {
            sender.sendMessage("§a" + message + " " + (max ? "максимум" : "минимум") +
                    " установлен на §e" + value + " §aдля мира §e" + worldName);
        } else {
            sender.sendMessage("§cОшибка установки лимита!");
        }
    }

    private void handleMultiplier(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier <type|value|step|formula|limits|info|simulate|reset|enable|disable> <тип_цены> [значение]");
            return;
        }

        String multiplierSubCmd = args[1].toLowerCase();
        String priceType = args[2].toLowerCase();

        switch (multiplierSubCmd) {
            case "type":
                handleMultiplierType(sender, args, priceType);
                break;

            case "value":
                handleMultiplierValue(sender, args, priceType);
                break;

            case "step":
                handleMultiplierStep(sender, args, priceType);
                break;

            case "formula":
                handleMultiplierFormula(sender, args, priceType);
                break;

            case "limits":
                handleMultiplierLimits(sender, args, priceType);
                break;

            case "info":
                handleMultiplierInfo(sender, priceType);
                break;

            case "simulate":
                handleMultiplierSimulate(sender, args, priceType);
                break;

            case "reset":
                handleMultiplierReset(sender, args, priceType);
                break;

            case "enable":
                handleMultiplierEnable(sender, priceType, true);
                break;

            case "disable":
                handleMultiplierEnable(sender, priceType, false);
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + multiplierSubCmd);
                break;
        }
    }

    private void handleMultiplierType(CommandSender sender, String[] args, String priceType) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier type <тип_цены> <FIXED|LINEAR|EXPONENTIAL|CUSTOM>");
            return;
        }

        String typeStr = args[3].toUpperCase();

        try {
            MultiplierManager.MultiplierType type = MultiplierManager.MultiplierType.valueOf(typeStr);

            if (multiplierManager.setMultiplierType(priceType, type)) {
                sender.sendMessage("§aТип множителя для §e" + priceType + " §aустановлен: §e" + type);
            } else {
                sender.sendMessage("§cОшибка установки типа множителя!");
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cНеверный тип множителя! Доступные: §eFIXED, LINEAR, EXPONENTIAL, CUSTOM");
        }
    }

    private void handleMultiplierValue(CommandSender sender, String[] args, String priceType) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier value <тип_цены> <значение>");
            return;
        }

        try {
            double value = Double.parseDouble(args[3]);

            if (multiplierManager.setMultiplierValue(priceType, value)) {
                sender.sendMessage("§aЗначение множителя для §e" + priceType + " §aустановлено: §e" + value);
            } else {
                sender.sendMessage("§cОшибка установки значения множителя!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
        }
    }

    private void handleMultiplierStep(CommandSender sender, String[] args, String priceType) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier step <тип_цены> <шаг>");
            return;
        }

        try {
            double step = Double.parseDouble(args[3]);

            if (multiplierManager.setMultiplierStep(priceType, step)) {
                sender.sendMessage("§aШаг роста для §e" + priceType + " §aустановлен: §e" + step);
            } else {
                sender.sendMessage("§cОшибка установки шага роста!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для шага!");
        }
    }

    private void handleMultiplierFormula(CommandSender sender, String[] args, String priceType) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier formula <тип_цены> \"<формула>\"");
            return;
        }

        // Собираем формулу из оставшихся аргументов
        StringBuilder formulaBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            formulaBuilder.append(args[i]).append(" ");
        }

        String formula = formulaBuilder.toString().trim();

        if (multiplierManager.setMultiplierFormula(priceType, formula)) {
            sender.sendMessage("§aФормула для §e" + priceType + " §aустановлена: §e" + formula);
        } else {
            sender.sendMessage("§cОшибка установки формулы!");
        }
    }

    private void handleMultiplierLimits(CommandSender sender, String[] args, String priceType) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier limits <тип_цены> <мин> <макс>");
            return;
        }

        try {
            double min = Double.parseDouble(args[3]);
            double max = Double.parseDouble(args[4]);

            if (multiplierManager.setMultiplierLimits(priceType, min, max)) {
                sender.sendMessage("§aLimits для §e" + priceType + " §aустановлены: §e" + min + " - " + max);
            } else {
                sender.sendMessage("§cОшибка установки лимитов!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите числа для лимитов!");
        }
    }

    private void handleMultiplierInfo(CommandSender sender, String priceType) {
        MultiplierManager.MultiplierInfo info = multiplierManager.getMultiplierInfo(priceType);

        if (info == null) {
            sender.sendMessage("§cМножитель не найден для типа: §e" + priceType);
            return;
        }

        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eИнформация о множителе: §a" + priceType);
        sender.sendMessage("§6══════════════════════════════════════");

        sender.sendMessage("§7Статус: " + (info.isEnabled() ? "§aВключен" : "§cВыключен"));
        sender.sendMessage("§7Тип: §a" + info.getType());
        sender.sendMessage("§7Базовое значение: §a" + info.getBaseValue());

        if (info.getType() == MultiplierManager.MultiplierType.LINEAR) {
            sender.sendMessage("§7Шаг роста: §a" + info.getStep());
        } else if (info.getType() == MultiplierManager.MultiplierType.CUSTOM) {
            sender.sendMessage("§7Формула: §a" + info.getCustomFormula());
        }

        sender.sendMessage("§7Лимиты: §a" + info.getMinMultiplier() + " - " + info.getMaxMultiplier());
        sender.sendMessage("§7Сброс: §a" + info.getResetSchedule());

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleMultiplierSimulate(CommandSender sender, String[] args, String priceType) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier simulate <тип_цены> <уровней>");
            return;
        }

        try {
            int levels = Integer.parseInt(args[3]);
            double basePrice = 1.0; // Базовая цена для симуляции

            List<String> simulation = multiplierManager.simulatePrices(priceType, levels, basePrice);
            simulation.forEach(sender::sendMessage);

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для уровней!");
        }
    }

    private void handleMultiplierReset(CommandSender sender, String[] args, String priceType) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin multiplier reset <тип_цены> <мир> <игрок>");
            return;
        }

        String worldName = args[3];
        String playerName = args[4];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return;
        }

        if (multiplierManager.resetPlayerProgress(target, worldName, priceType)) {
            sender.sendMessage("§aПрогресс множителя сброшен для §e" + playerName + "§a в мире §e" + worldName);
        } else {
            sender.sendMessage("§cИгрок не имеет прогресса для этого множителя!");
        }
    }

    private void handleMultiplierEnable(CommandSender sender, String priceType, boolean enable) {
        if (multiplierManager.setMultiplierEnabled(priceType, enable)) {
            sender.sendMessage("§aМножитель для §e" + priceType + " §a" + (enable ? "включен" : "выключен"));
        } else {
            sender.sendMessage("§cОшибка изменения статуса множителя!");
        }
    }

    private void handlePrice(CommandSender sender, String[] args) {
        // Команды для управления базовыми ценами
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin price <мир> <тип> <значение>");
            sender.sendMessage("§cТипы: expand, shrink, speed-up, speed-down, damage-down, damage-up");
            return;
        }

        String worldName = args[1];
        String priceType = args[2].toLowerCase();
        double value;

        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для значения!");
            return;
        }

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        switch (priceType) {
            case "expand":
                data.setExpandCost(value);
                break;

            case "shrink":
                data.setShrinkCost(value);
                break;

            case "speed-up":
                data.setSpeedUpCost(value);
                break;

            case "speed-down":
                data.setSpeedDownCost(value);
                break;

            case "damage-down":
                data.setDamageDownCost(value);
                break;

            case "damage-up":
                data.setDamageUpCost(value);
                break;

            default:
                sender.sendMessage("§cНеизвестный тип цены: §e" + priceType);
                return;
        }

        borderManager.saveWorldData(data);
        sender.sendMessage("§aЦена §e" + priceType + "§a установлена на §e" + value + "§a для мира §e" + worldName);
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin item <add|remove|list|price>");
            return;
        }

        String itemSubCmd = args[1].toLowerCase();

        switch (itemSubCmd) {
            case "add":
                handleItemAdd(sender, args);
                break;

            case "remove":
                handleItemRemove(sender, args);
                break;

            case "list":
                handleItemList(sender, args);
                break;

            case "price":
                handleItemPrice(sender, args);
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + itemSubCmd);
                break;
        }
    }

    private void handleItemAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin item add <мир> <предмет> <цена>");
            return;
        }

        String worldName = args[2];
        String itemName = args[3].toUpperCase();
        double price;

        try {
            price = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для цены!");
            return;
        }

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.addItemPrice(itemName, price);
        borderManager.saveWorldData(data);

        sender.sendMessage("§aПредмет §e" + itemName + "§a добавлен в магазин мира §e" + worldName);
        sender.sendMessage("§aЦена: §e" + currencyManager.formatCurrency(price));
    }

    private void handleItemRemove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin item remove <мир> <предмет>");
            return;
        }

        String worldName = args[2];
        String itemName = args[3].toUpperCase();

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        data.removeItemPrice(itemName);
        borderManager.saveWorldData(data);

        sender.sendMessage("§aПредмет §e" + itemName + "§a удален из магазина мира §e" + worldName);
    }

    private void handleItemList(CommandSender sender, String[] args) {
        String worldName = args.length >= 3 ? args[2] : null;

        if (worldName != null) {
            // Показать предметы конкретного мира
            WorldBorderData data = borderManager.getWorldData(worldName);
            if (data == null) {
                sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
                return;
            }

            Map<String, Double> items = data.getWorldItemPrices();
            showItemList(sender, worldName, items);

        } else {
            // Показать все предметы всех миров
            Map<String, WorldBorderData> allWorlds = borderManager.getAllWorldData();

            sender.sendMessage("§6══════════════════════════════════════");
            sender.sendMessage("§eВсе предметы для продажи");
            sender.sendMessage("§6══════════════════════════════════════");

            for (Map.Entry<String, WorldBorderData> entry : allWorlds.entrySet()) {
                Map<String, Double> items = entry.getValue().getWorldItemPrices();
                if (!items.isEmpty()) {
                    sender.sendMessage("§e" + entry.getKey() + ":");
                    for (Map.Entry<String, Double> itemEntry : items.entrySet()) {
                        sender.sendMessage("  §7- §f" + itemEntry.getKey() + " §7- §a" +
                                currencyManager.formatCurrency(itemEntry.getValue()));
                    }
                    sender.sendMessage("§6──────────────────────────────────────");
                }
            }

            sender.sendMessage("§6══════════════════════════════════════");
        }
    }

    private void showItemList(CommandSender sender, String worldName, Map<String, Double> items) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eПредметы для продажи: §a" + worldName);
        sender.sendMessage("§6══════════════════════════════════════");

        if (items.isEmpty()) {
            sender.sendMessage("§cНет предметов для продажи в этом мире!");
        } else {
            int i = 1;
            for (Map.Entry<String, Double> entry : items.entrySet()) {
                sender.sendMessage("§7" + i + ". §f" + entry.getKey() + " §7- §a" +
                        currencyManager.formatCurrency(entry.getValue()));
                i++;
            }
        }

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleItemPrice(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin item price <мир> <предмет>");
            return;
        }

        String worldName = args[2];
        String itemName = args[3].toUpperCase();

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null) {
            sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        Double price = data.getItemPrice(itemName);
        if (price == null) {
            sender.sendMessage("§cПредмет §e" + itemName + "§c не найден в магазине мира §e" + worldName);
        } else {
            sender.sendMessage("§aЦена предмета §e" + itemName + "§a в мире §e" + worldName + "§a: §e" +
                    currencyManager.formatCurrency(price));
        }
    }

    private void handleShop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin shop <on|off|status> [мир]");
            return;
        }

        String shopSubCmd = args[1].toLowerCase();
        String worldName = args.length >= 3 ? args[2] : null;

        switch (shopSubCmd) {
            case "on":
                handleShopEnable(sender, worldName, true);
                break;

            case "off":
                handleShopEnable(sender, worldName, false);
                break;

            case "status":
                handleShopStatus(sender, worldName);
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + shopSubCmd);
                break;
        }
    }

    private void handleShopEnable(CommandSender sender, String worldName, boolean enable) {
        if (worldName != null) {
            // Включить/выключить магазин для конкретного мира
            WorldBorderData data = borderManager.getWorldData(worldName);
            if (data == null) {
                sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
                return;
            }

            data.setShopEnabled(enable);
            borderManager.saveWorldData(data);

            sender.sendMessage("§aМагазин " + (enable ? "включен" : "выключен") + " для мира §e" + worldName);

        } else {
            // Включить/выключить глобальный магазин
            // В реальной реализации нужно добавить метод в CurrencyManager
            sender.sendMessage("§aГлобальный магазин " + (enable ? "включен" : "выключен"));
            sender.sendMessage("§cВнимание: Функция требует дополнительной реализации!");
        }
    }

    private void handleShopStatus(CommandSender sender, String worldName) {
        if (worldName != null) {
            // Статус магазина конкретного мира
            WorldBorderData data = borderManager.getWorldData(worldName);
            if (data == null) {
                sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
                return;
            }

            String status = data.isShopEnabled() ? "§aВключен" : "§cВыключен";
            sender.sendMessage("§aСтатус магазина мира §e" + worldName + "§a: " + status);

        } else {
            // Глобальный статус
            // В реальной реализации нужно получить статус из CurrencyManager
            sender.sendMessage("§aГлобальный статус магазина: §aВключен");
            sender.sendMessage("§cВнимание: Функция требует дополнительной реализации!");
        }
    }

    private void handlePlayer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin player <ник> <block|unblock|status>");
            return;
        }

        String playerName = args[2];
        String action = args[3].toLowerCase();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return;
        }

        switch (action) {
            case "block":
                // Блокировка игрока
                // В реальной реализации нужно добавить метод в CurrencyManager
                sender.sendMessage("§aИгрок §e" + playerName + "§a заблокирован в магазине");
                sender.sendMessage("§cВнимание: Функция требует дополнительной реализации!");
                break;

            case "unblock":
                // Разблокировка игрока
                sender.sendMessage("§aИгрок §e" + playerName + "§a разблокирован в магазине");
                sender.sendMessage("§cВнимание: Функция требует дополнительной реализации!");
                break;

            case "status":
                // Статус игрока
                sender.sendMessage("§aСтатус игрока §e" + playerName + "§a: §aРазблокирован");
                sender.sendMessage("§cВнимание: Функция требует дополнительной реализации!");
                break;

            default:
                sender.sendMessage("§cНеизвестное действие: §e" + action);
                break;
        }
    }

    private void handleBalanceAdmin(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin balance <ник> <set|add|take|reset> [сумма]");
            return;
        }

        String playerName = args[2];
        String action = args[3].toLowerCase();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return;
        }

        switch (action) {
            case "set":
                handleBalanceSet(sender, target, args);
                break;

            case "add":
                handleBalanceAdd(sender, target, args);
                break;

            case "take":
                handleBalanceTake(sender, target, args);
                break;

            case "reset":
                handleBalanceReset(sender, target);
                break;

            default:
                sender.sendMessage("§cНеизвестное действие: §e" + action);
                break;
        }
    }

    private void handleBalanceSet(CommandSender sender, Player target, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin balance <ник> set <сумма>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[4]);

            if (currencyManager.setBalance(target, amount)) {
                sender.sendMessage("§aБаланс игрока §e" + target.getName() + "§a установлен на §e" +
                        currencyManager.formatCurrency(amount));
            } else {
                sender.sendMessage("§cОшибка установки баланса!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для суммы!");
        }
    }

    private void handleBalanceAdd(CommandSender sender, Player target, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin balance <ник> add <сумма>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[4]);

            if (currencyManager.addBalance(target, amount)) {
                double newBalance = currencyManager.getBalance(target);
                sender.sendMessage("§aИгроку §e" + target.getName() + "§a добавлено §e" +
                        currencyManager.formatCurrency(amount));
                sender.sendMessage("§aНовый баланс: §e" + currencyManager.formatCurrency(newBalance));
            } else {
                sender.sendMessage("§cОшибка добавления баланса!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для суммы!");
        }
    }

    private void handleBalanceTake(CommandSender sender, Player target, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin balance <ник> take <сумма>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[4]);

            if (currencyManager.withdrawBalance(target, amount)) {
                double newBalance = currencyManager.getBalance(target);
                sender.sendMessage("§aУ игрока §e" + target.getName() + "§a снято §e" +
                        currencyManager.formatCurrency(amount));
                sender.sendMessage("§aНовый баланс: §e" + currencyManager.formatCurrency(newBalance));
            } else {
                sender.sendMessage("§cОшибка снятия баланса! У игрока недостаточно средств.");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cВведите число для суммы!");
        }
    }

    private void handleBalanceReset(CommandSender sender, Player target) {
        if (currencyManager.setBalance(target, 0.0)) {
            sender.sendMessage("§aБаланс игрока §e" + target.getName() + "§a сброшен на 0");
        } else {
            sender.sendMessage("§cОшибка сброса баланса!");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadAllConfigs();
        sender.sendMessage("§aВсе конфигурации перезагружены!");
    }

    private void handleSave(CommandSender sender) {
        // Сохраняем все данные через менеджеры
        if (plugin.getBorderManager() != null) {
            plugin.getBorderManager().saveAllWorlds();
        }
        if (plugin.getCurrencyManager() != null) {
            plugin.getCurrencyManager().saveData();
        }
        if (plugin.getMultiplierManager() != null) {
            plugin.getMultiplierManager().saveAllData();
        }
        if (plugin.getConfigManager() != null) {
            plugin.getConfigManager().saveAllConfigs();
        }

        sender.sendMessage("§aВсе данные успешно сохранены!");
    }

    private void handleStatsAdmin(CommandSender sender, String[] args) {
        String worldName = args.length >= 2 ? args[1] : null;

        if (worldName != null) {
            // Статистика конкретного мира
            WorldBorderData data = borderManager.getWorldData(worldName);
            if (data == null) {
                sender.sendMessage("§cМир §e" + worldName + "§c не найден!");
                return;
            }

            sendWorldStats(sender, data);

        } else {
            // Общая статистика
            sendGlobalStats(sender);
        }
    }

    private void sendWorldStats(CommandSender sender, WorldBorderData data) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eСтатистика мира: §a" + data.getWorldName());
        sender.sendMessage("§6══════════════════════════════════════");

        sender.sendMessage("§7Расширений: §a" + data.getTotalExpansions());
        sender.sendMessage("§7Сужений: §a" + data.getTotalShrinks());
        sender.sendMessage("§7Ускорений: §a" + data.getTotalSpeedUpgrades());
        sender.sendMessage("§7Замедлений: §a" + data.getTotalSpeedDowngrades());
        sender.sendMessage("§7Увеличений урона: §a" + data.getTotalDamageUpgrades());
        sender.sendMessage("§7Уменьшений урона: §a" + data.getTotalDamageDowngrades());

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Финансовая статистика:");
        sender.sendMessage("  §7Всего потрачено: §a" + currencyManager.formatCurrency(data.getTotalCurrencySpent()));
        sender.sendMessage("  §7Всего заработано: §a" + currencyManager.formatCurrency(data.getTotalCurrencyEarned()));
        sender.sendMessage("  §7Баланс: §a" + currencyManager.formatCurrency(
                data.getTotalCurrencyEarned() - data.getTotalCurrencySpent()));

        // Время создания и активности
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String created = sdf.format(new java.util.Date(data.getCreationTime()));
        String modified = sdf.format(new java.util.Date(data.getLastModified()));

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Создан: §a" + created);
        sender.sendMessage("§7Изменен: §a" + modified);

        if (data.getLastExpansion() > 0) {
            String lastExpansion = sdf.format(new java.util.Date(data.getLastExpansion()));
            sender.sendMessage("§7Последнее расширение: §a" + lastExpansion);
        }

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void sendGlobalStats(CommandSender sender) {
        Map<String, WorldBorderData> allWorlds = borderManager.getAllWorldData();

        int totalExpansions = 0;
        int totalShrinks = 0;
        double totalSpent = 0;
        double totalEarned = 0;

        for (WorldBorderData data : allWorlds.values()) {
            totalExpansions += data.getTotalExpansions();
            totalShrinks += data.getTotalShrinks();
            totalSpent += data.getTotalCurrencySpent();
            totalEarned += data.getTotalCurrencyEarned();
        }

        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eГлобальная статистика");
        sender.sendMessage("§6══════════════════════════════════════");

        sender.sendMessage("§7Всего миров: §a" + allWorlds.size());
        sender.sendMessage("§7Всего расширений: §a" + totalExpansions);
        sender.sendMessage("§7Всего сужений: §a" + totalShrinks);

        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Финансовая статистика:");
        sender.sendMessage("  §7Всего потрачено: §a" + currencyManager.formatCurrency(totalSpent));
        sender.sendMessage("  §7Всего заработано: §a" + currencyManager.formatCurrency(totalEarned));
        sender.sendMessage("  §7Общий баланс: §a" + currencyManager.formatCurrency(totalEarned - totalSpent));

        // Топ 5 миров по активности
        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§eТоп 5 миров по активности:");

        List<WorldBorderData> sortedWorlds = new ArrayList<>(allWorlds.values());
        sortedWorlds.sort((w1, w2) -> Integer.compare(
                w2.getTotalExpansions() + w2.getTotalShrinks(),
                w1.getTotalExpansions() + w1.getTotalShrinks()
        ));

        for (int i = 0; i < Math.min(5, sortedWorlds.size()); i++) {
            WorldBorderData world = sortedWorlds.get(i);
            int activity = world.getTotalExpansions() + world.getTotalShrinks();
            sender.sendMessage("§7" + (i + 1) + ". §e" + world.getWorldName() +
                    "§7: §a" + activity + " §7действий");
        }

        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleDebug(CommandSender sender, String[] args) {
        boolean newState = !plugin.isDebugMode();

        if (args.length >= 2) {
            String state = args[1].toLowerCase();
            if (state.equals("on") || state.equals("true") || state.equals("1")) {
                newState = true;
            } else if (state.equals("off") || state.equals("false") || state.equals("0")) {
                newState = false;
            }
        }

        plugin.setDebugMode(newState);
        sender.sendMessage("§aРежим отладки " + (newState ? "§aвключен" : "§cвыключен"));
    }

    private void handleBackup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользуйте: §e/borderadmin backup <create|list|restore>");
            return;
        }

        String backupSubCmd = args[1].toLowerCase();

        switch (backupSubCmd) {
            case "create":
                sender.sendMessage("§aСоздание бэкапа...");
                // Реализация создания бэкапа
                sender.sendMessage("§cВнимание: Функция бэкапа требует дополнительной реализации!");
                break;

            case "list":
                sender.sendMessage("§aСписок бэкапов:");
                // Реализация списка бэкапов
                sender.sendMessage("§cВнимание: Функция бэкапа требует дополнительной реализации!");
                break;

            case "restore":
                if (args.length < 3) {
                    sender.sendMessage("§cИспользуйте: §e/borderadmin backup restore <имя_бэкапа>");
                    return;
                }
                sender.sendMessage("§aВосстановление бэкапа " + args[2] + "...");
                // Реализация восстановления бэкапа
                sender.sendMessage("§cВнимание: Функция бэкапа требует дополнительной реализации!");
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда: §e" + backupSubCmd);
                break;
        }
    }

    private void handleVersionAdmin(CommandSender sender) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§eDynamicWorldBorder v3.0 - Админ версия");
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§7Разработчик: §aDan");
        sender.sendMessage("§7Версия API: §a" + plugin.getDescription().getAPIVersion());
        sender.sendMessage("§7Загружено миров: §a" + borderManager.getLoadedWorldsCount());
        sender.sendMessage("§7Режим отладки: §a" + (plugin.isDebugMode() ? "Включен" : "Выключен"));
        sender.sendMessage("§6──────────────────────────────────────");
        sender.sendMessage("§7Доступные команды:");
        sender.sendMessage("§7• §e/borderadmin world §7- Управление мирами");
        sender.sendMessage("§7• §e/borderadmin limit §7- Управление лимитами");
        sender.sendMessage("§7• §e/borderadmin multiplier §7- Управление множителями");
        sender.sendMessage("§7• §e/borderadmin price §7- Управление ценами");
        sender.sendMessage("§7• §e/borderadmin item §7- Управление предметами");
        sender.sendMessage("§7• §e/borderadmin shop §7- Управление магазином");
        sender.sendMessage("§7• §e/borderadmin player §7- Управление игроками");
        sender.sendMessage("§7• §e/borderadmin balance §7- Управление балансами");
        sender.sendMessage("§7• §e/borderadmin help §7- Полная справка");
        sender.sendMessage("§6══════════════════════════════════════");
    }

    private void handleHelpAdmin(CommandSender sender, String[] args) {
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // Игнорируем, оставляем страницу 1
            }
        }

        sendAdminHelp(sender, page);
    }

    private void sendAdminHelp(CommandSender sender, int page) {
        List<String> helpPages = new ArrayList<>();

        // Страница 1: Основные команды
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Админ помощь §7(Страница 1/5)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Основные команды:\n" +
                "§e/borderadmin reload §7- Перезагрузить конфиги\n" +
                "§e/borderadmin save §7- Сохранить все данные\n" +
                "§e/borderadmin stats [мир] §7- Статистика\n" +
                "§e/borderadmin debug [on/off] §7- Режим отладки\n" +
                "§e/borderadmin backup <create|list|restore> §7- Бэкапы\n" +
                "§e/borderadmin version §7- Версия плагина\n" +
                "§e/borderadmin help [страница] §7- Эта справка\n" +
                "§6══════════════════════════════════════");

        // Страница 2: Управление мирами
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Админ помощь §7(Страница 2/5)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Управление мирами:\n" +
                "§e/borderadmin world enable <мир> §7- Включить систему\n" +
                "§e/borderadmin world disable <мир> §7- Выключить систему\n" +
                "§e/borderadmin world upgradable <мир> <on/off> §7- Разрешить улучшения\n" +
                "§e/borderadmin world set <мир> <размер> §7- Установить размер\n" +
                "§e/borderadmin world speed <мир> <скорость> §7- Установить скорость\n" +
                "§e/borderadmin world damage <мир> <урон> §7- Установить урон\n" +
                "§e/borderadmin world info <мир> §7- Информация о мире\n" +
                "§e/borderadmin world list §7- Список миров\n" +
                "§e/borderadmin world reset <мир> §7- Сбросить настройки\n" +
                "§e/borderadmin world copy <из> <в> §7- Копировать настройки\n" +
                "§6══════════════════════════════════════");

        // Страница 3: Управление лимитами
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Админ помощь §7(Страница 3/5)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Управление лимитами:\n" +
                "§e/borderadmin limit size <maxabs|minabs|max|min> <мир> <значение> §7- Лимиты размера\n" +
                "§e/borderadmin limit speed <maxabs|minabs|max|min> <мир> <значение> §7- Лимиты скорости\n" +
                "§e/borderadmin limit damage <maxabs|minabs|max|min> <мир> <значение> §7- Лимиты урона\n" +
                "§e/borderadmin limit info <мир> §7- Информация о лимитах\n" +
                "§e/borderadmin limit all §7- Все лимиты\n" +
                "§e/borderadmin limit reset <мир> [тип] §7- Сбросить лимиты\n" +
                "§e/borderadmin limit apply <из> <в> §7- Применить лимиты\n" +
                "§e/borderadmin limit compare <мир1> <мир2> §7- Сравнить лимиты\n" +
                "§e/borderadmin limit validate <мир> §7- Проверить лимиты\n" +
                "§e/borderadmin limit player <size|speed|damage> <max|min> <мир> <значение> §7- Лимиты игроков\n" +
                "§6══════════════════════════════════════");

        // Страница 4: Множители и цены
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Админ помощь §7(Страница 4/5)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Управление множителями:\n" +
                "§e/borderadmin multiplier type <тип> <FIXED|LINEAR|EXPONENTIAL|CUSTOM> §7- Тип множителя\n" +
                "§e/borderadmin multiplier value <тип> <значение> §7- Значение множителя\n" +
                "§e/borderadmin multiplier step <тип> <шаг> §7- Шаг роста\n" +
                "§e/borderadmin multiplier formula <тип> \"<формула>\" §7- Кастомная формула\n" +
                "§e/borderadmin multiplier limits <тип> <мин> <макс> §7- Лимиты множителя\n" +
                "§e/borderadmin multiplier info <тип> §7- Информация\n" +
                "§e/borderadmin multiplier simulate <тип> <уровней> §7- Симуляция\n" +
                "§e/borderadmin multiplier reset <тип> <мир> <игрок> §7- Сбросить прогресс\n" +
                "§e/borderadmin multiplier enable <тип> §7- Включить множитель\n" +
                "§e/borderadmin multiplier disable <тип> §7- Выключить множитель\n" +
                "§7\n" +
                "§7Управление ценами:\n" +
                "§e/borderadmin price <мир> <тип> <значение> §7- Установить цену\n" +
                "§6══════════════════════════════════════");

        // Страница 5: Магазин и игроки
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Админ помощь §7(Страница 5/5)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Управление магазином:\n" +
                "§e/borderadmin item add <мир> <предмет> <цена> §7- Добавить предмет\n" +
                "§e/borderadmin item remove <мир> <предмет> §7- Удалить предмет\n" +
                "§e/borderadmin item list [мир] §7- Список предметов\n" +
                "§e/borderadmin item price <мир> <предмет> §7- Цена предмета\n" +
                "§e/borderadmin shop on/off [мир] §7- Включить/выключить магазин\n" +
                "§e/borderadmin shop status [мир] §7- Статус магазина\n" +
                "§7\n" +
                "§7Управление игроками:\n" +
                "§e/borderadmin player <ник> block/unblock §7- Блокировка игрока\n" +
                "§e/borderadmin player <ник> status §7- Статус игрока\n" +
                "§e/borderadmin balance <ник> set <сумма> §7- Установить баланс\n" +
                "§e/borderadmin balance <ник> add <сумма> §7- Добавить баланс\n" +
                "§e/borderadmin balance <ник> take <сумма> §7- Снять баланс\n" +
                "§e/borderadmin balance <ник> reset §7- Сбросить баланс\n" +
                "§6══════════════════════════════════════");

        // Проверяем номер страницы
        if (page < 1 || page > helpPages.size()) {
            page = 1;
        }

        // Отправляем нужную страницу
        sender.sendMessage(helpPages.get(page - 1));

        // Показываем навигацию
        if (helpPages.size() > 1) {
            sender.sendMessage("§7Используйте: §e/borderadmin help " + (page % helpPages.size() + 1) +
                    " §7для следующей страницы");
        }
    }

    // ========== АВТОДОПОЛНЕНИЕ ==========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dynamicborder.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Дополнение для основной команды
            return StringUtil.copyPartialMatches(args[0], ADMIN_SUBCOMMANDS, new ArrayList<>());
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "world":
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], WORLD_SUBCOMMANDS, new ArrayList<>());
                } else if (args.length == 3) {
                    // Для enable, disable, upgradable, set, speed, damage, info, reset, copy - нужен мир
                    if (Arrays.asList("enable", "disable", "upgradable", "set", "speed", "damage",
                            "info", "reset", "copy", "create", "remove").contains(args[1].toLowerCase())) {
                        return StringUtil.copyPartialMatches(args[2], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                } else if (args.length == 4) {
                    // Для copy нужен второй мир
                    if (args[1].equalsIgnoreCase("copy")) {
                        return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                    // Для upgradable нужен on/off
                    else if (args[1].equalsIgnoreCase("upgradable")) {
                        completions.add("on");
                        completions.add("off");
                        return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
                    }
                }
                break;

            case "limit":
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], LIMIT_SUBCOMMANDS, new ArrayList<>());
                } else if (args.length == 3) {
                    // Для size, speed, damage нужен тип
                    if (Arrays.asList("size", "speed", "damage", "player").contains(args[1].toLowerCase())) {
                        if (args[1].equalsIgnoreCase("player")) {
                            completions.add("size");
                            completions.add("speed");
                            completions.add("damage");
                        } else {
                            completions.add("maxabs");
                            completions.add("minabs");
                            completions.add("max");
                            completions.add("min");
                            completions.add("playermax");
                            completions.add("playermin");
                        }
                        return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                    }
                    // Для info, reset, validate, compare нужен мир
                    else if (Arrays.asList("info", "reset", "validate", "compare", "apply").contains(args[1].toLowerCase())) {
                        return StringUtil.copyPartialMatches(args[2], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                } else if (args.length == 4) {
                    // Для size, speed, damage нужен мир
                    if (Arrays.asList("size", "speed", "damage").contains(args[1].toLowerCase())) {
                        return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                    // Для player нужен max/min
                    else if (args[1].equalsIgnoreCase("player")) {
                        completions.add("max");
                        completions.add("min");
                        return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
                    }
                    // Для compare нужен второй мир
                    else if (args[1].equalsIgnoreCase("compare")) {
                        return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                    // Для apply нужен целевой мир
                    else if (args[1].equalsIgnoreCase("apply")) {
                        return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                    // Для reset может быть тип
                    else if (args[1].equalsIgnoreCase("reset")) {
                        completions.add("size");
                        completions.add("speed");
                        completions.add("damage");
                        completions.add("all");
                        return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
                    }
                } else if (args.length == 5) {
                    // Для player нужен мир
                    if (args[1].equalsIgnoreCase("player")) {
                        return StringUtil.copyPartialMatches(args[4], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                }
                break;

            case "multiplier":
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], MULTIPLIER_SUBCOMMANDS, new ArrayList<>());
                } else if (args.length == 3) {
                    // Нужен тип цены
                    return StringUtil.copyPartialMatches(args[2], PRICE_TYPES, new ArrayList<>());
                } else if (args.length == 4) {
                    // Для type нужен тип множителя
                    if (args[1].equalsIgnoreCase("type")) {
                        return StringUtil.copyPartialMatches(args[3], MULTIPLIER_TYPES, new ArrayList<>());
                    }
                    // Для simulate нужны уровни
                    else if (args[1].equalsIgnoreCase("simulate")) {
                        completions.add("5");
                        completions.add("10");
                        completions.add("20");
                        completions.add("50");
                        return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
                    }
                    // Для reset нужен мир
                    else if (args[1].equalsIgnoreCase("reset")) {
                        return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                    }
                } else if (args.length == 5) {
                    // Для reset нужен игрок
                    if (args[1].equalsIgnoreCase("reset")) {
                        // Возвращаем имена онлайн игроков
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[4].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
                break;

            case "price":
                if (args.length == 2) {
                    // Нужен мир
                    return StringUtil.copyPartialMatches(args[1], borderManager.getAvailableWorlds(), new ArrayList<>());
                } else if (args.length == 3) {
                    // Нужен тип цены
                    return StringUtil.copyPartialMatches(args[2], PRICE_TYPES, new ArrayList<>());
                }
                break;

            case "item":
                if (args.length == 2) {
                    completions.add("add");
                    completions.add("remove");
                    completions.add("list");
                    completions.add("price");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                } else if (args.length == 3) {
                    // Для add, remove, list, price нужен мир
                    return StringUtil.copyPartialMatches(args[2], borderManager.getAvailableWorlds(), new ArrayList<>());
                } else if (args.length == 4) {
                    // Для add и remove нужен предмет
                    if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") ||
                            args[1].equalsIgnoreCase("price")) {
                        // Здесь можно добавить автодополнение для названий предметов Minecraft
                        completions.add("DIAMOND");
                        completions.add("EMERALD");
                        completions.add("GOLD_INGOT");
                        completions.add("IRON_INGOT");
                        completions.add("NETHERITE_INGOT");
                        return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
                    }
                }
                break;

            case "shop":
                if (args.length == 2) {
                    completions.add("on");
                    completions.add("off");
                    completions.add("status");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                } else if (args.length == 3) {
                    // Опционально: мир
                    return StringUtil.copyPartialMatches(args[2], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;

            case "player":
                if (args.length == 2) {
                    // Имена онлайн игроков
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args.length == 3) {
                    completions.add("block");
                    completions.add("unblock");
                    completions.add("status");
                    return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                }
                break;

            case "balance":
                if (args.length == 2) {
                    // Имена онлайн игроков
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args.length == 3) {
                    completions.add("set");
                    completions.add("add");
                    completions.add("take");
                    completions.add("reset");
                    return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                }
                break;

            case "debug":
                if (args.length == 2) {
                    completions.add("on");
                    completions.add("off");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                }
                break;

            case "backup":
                if (args.length == 2) {
                    completions.add("create");
                    completions.add("list");
                    completions.add("restore");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                }
                break;

            case "help":
                if (args.length == 2) {
                    completions.add("1");
                    completions.add("2");
                    completions.add("3");
                    completions.add("4");
                    completions.add("5");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                }
                break;

            case "stats":
                if (args.length == 2) {
                    // Опционально: мир
                    return StringUtil.copyPartialMatches(args[1], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;
        }

        return Collections.emptyList();
    }
}