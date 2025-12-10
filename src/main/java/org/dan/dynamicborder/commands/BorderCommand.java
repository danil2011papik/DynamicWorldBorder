package org.dan.dynamicborder.commands;

import java.util.Map;
import org.dan.dynamicborder.data.PlayerMultiplierData;
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
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class BorderCommand implements CommandExecutor, TabCompleter {

    private final DynamicBorderPlugin plugin;
    private final BorderManager borderManager;
    private final CurrencyManager currencyManager;
    private final MultiplierManager multiplierManager;

    // Доступные подкоманды для игроков
    private static final List<String> PLAYER_SUBCOMMANDS = Arrays.asList(
            "expand", "shrink", "speed", "damage", "status", "balance",
            "sell", "prices", "items", "upgrades", "effects", "worlds",
            "switch", "limits", "info", "price", "history", "stats",
            "help", "version"
    );

    // Доступные миры для автодополнения
    private static final List<String> WORLD_SUGGESTIONS = Arrays.asList(
            "world", "world_nether", "world_the_end"
    );

    public BorderCommand(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        this.borderManager = plugin.getBorderManager();
        this.currencyManager = plugin.getCurrencyManager();
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player, 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            switch (subCommand) {
                case "expand":
                    handleExpand(player, args);
                    break;

                case "shrink":
                    handleShrink(player, args);
                    break;

                case "speed":
                    handleSpeed(player, args);
                    break;

                case "damage":
                    handleDamage(player, args);
                    break;

                case "status":
                    handleStatus(player, args);
                    break;

                case "balance":
                    handleBalance(player);
                    break;

                case "sell":
                    handleSell(player, args);
                    break;

                case "prices":
                    handlePrices(player, args);
                    break;

                case "items":
                    handleItems(player);
                    break;

                case "upgrades":
                    handleUpgrades(player, args);
                    break;

                case "effects":
                    handleEffects(player, args);
                    break;

                case "worlds":
                    handleWorlds(player);
                    break;

                case "switch":
                    handleSwitch(player, args);
                    break;

                case "limits":
                    handleLimits(player, args);
                    break;

                case "info":
                    handleInfo(player, args);
                    break;

                case "price":
                    handlePrice(player, args);
                    break;

                case "history":
                    handlePriceHistory(player, args); // Используем существующий метод
                    break;

                case "stats":
                    handleStats(player);
                    break;

                case "help":
                    handleHelp(player, args);
                    break;

                case "version":
                    handleVersion(player);
                    break;

                default:
                    player.sendMessage("§cНеизвестная команда. Используйте §e/border help");
                    break;
            }
        } catch (Exception e) {
            player.sendMessage("§cПроизошла ошибка при выполнении команды!");
            plugin.logError("Ошибка в команде /border " + subCommand + ": " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return true;
    }

    // ========== ОБРАБОТЧИКИ КОМАНД ==========

    private void handleExpand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border expand <мир> <блоков>");
            player.sendMessage("§cИли: §e/brd expand <блоков> §7(если выбран мир)");
            return;
        }

        String worldName;
        double blocks;

        if (args.length >= 3) {
            // Формат: /border expand <мир> <блоков>
            worldName = args[1];
            try {
                blocks = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВведите число для блоков!");
                return;
            }
        } else {
            // Формат: /brd expand <блоков> (использует выбранный мир)
            worldName = borderManager.getPlayerSelectedWorld(player);
            try {
                blocks = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВведите число для блоков!");
                return;
            }
        }

        // Проверка доступности мира
        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isEnabled()) {
            player.sendMessage("§cСистема границы отключена для мира §e" + worldName);
            return;
        }

        // Выполнение расширения
        BorderManager.ExpandResult result = borderManager.expandWorld(worldName, blocks, player);

        if (result.isSuccess()) {
            player.sendMessage("§a✅ " + result.getMessage());
            player.sendMessage("§7Стоимость: §e" + currencyManager.formatCurrency(result.getCost()));

            // Показать следующую цену
            double nextPrice = multiplierManager.getPrice(
                    player, worldName, "expand",
                    worldData.getExpandCostFor(1.0)
            );
            player.sendMessage("§7Следующая цена за блок: §e" + currencyManager.formatCurrency(nextPrice));
        } else {
            player.sendMessage("§c❌ " + result.getMessage());
        }
    }

    private void handleShrink(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border shrink <мир> <блоков>");
            player.sendMessage("§cИли: §e/brd shrink <блоков> §7(если выбран мир)");
            return;
        }

        String worldName;
        double blocks;

        if (args.length >= 3) {
            worldName = args[1];
            try {
                blocks = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВведите число для блоков!");
                return;
            }
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
            try {
                blocks = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВведите число для блоков!");
                return;
            }
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isEnabled()) {
            player.sendMessage("§cСистема границы отключена для мира §e" + worldName);
            return;
        }

        BorderManager.ShrinkResult result = borderManager.shrinkWorld(worldName, blocks, player);

        if (result.isSuccess()) {
            player.sendMessage("§a✅ " + result.getMessage());
            player.sendMessage("§7Стоимость: §e" + currencyManager.formatCurrency(result.getCost()));

            double nextPrice = multiplierManager.getPrice(
                    player, worldName, "shrink",
                    worldData.getShrinkCostFor(1.0)
            );
            player.sendMessage("§7Следующая цена за блок: §e" + currencyManager.formatCurrency(nextPrice));
        } else {
            player.sendMessage("§c❌ " + result.getMessage());
        }
    }

    private void handleSpeed(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border speed <up/down> [мир]");
            player.sendMessage("§cИли: §e/brd speed <up/down>");
            return;
        }

        String direction = args[1].toLowerCase();
        if (!direction.equals("up") && !direction.equals("down")) {
            player.sendMessage("§cИспользуйте: §eup §cили §edown");
            return;
        }

        boolean up = direction.equals("up");
        String worldName;

        if (args.length >= 3) {
            worldName = args[2];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isEnabled() || !worldData.isUpgradable()) {
            player.sendMessage("§cУлучшения отключены для мира §e" + worldName);
            return;
        }

        BorderManager.UpgradeResult result = borderManager.upgradeSpeed(worldName, up, player);

        if (result.isSuccess()) {
            player.sendMessage("§a✅ " + result.getMessage());
            player.sendMessage("§7Стоимость: §e" + currencyManager.formatCurrency(result.getCost()));

            String priceType = up ? "speed-up" : "speed-down";
            double nextPrice = multiplierManager.getPrice(
                    player, worldName, priceType,
                    worldData.getSpeedUpgradeCost(up)
            );
            player.sendMessage("§7Следующая цена: §e" + currencyManager.formatCurrency(nextPrice));
        } else {
            player.sendMessage("§c❌ " + result.getMessage());
        }
    }

    private void handleDamage(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border damage <up/down> [мир]");
            player.sendMessage("§cИли: §e/brd damage <up/down>");
            return;
        }

        String direction = args[1].toLowerCase();
        if (!direction.equals("up") && !direction.equals("down")) {
            player.sendMessage("§cИспользуйте: §eup §cили §edown");
            return;
        }

        boolean down = direction.equals("down"); // down = уменьшить урон, up = увеличить
        String worldName;

        if (args.length >= 3) {
            worldName = args[2];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isEnabled() || !worldData.isUpgradable()) {
            player.sendMessage("§cУлучшения отключены для мира §e" + worldName);
            return;
        }

        BorderManager.UpgradeResult result = borderManager.upgradeDamage(worldName, down, player);

        if (result.isSuccess()) {
            player.sendMessage("§a✅ " + result.getMessage());
            player.sendMessage("§7Стоимость: §e" + currencyManager.formatCurrency(result.getCost()));

            String priceType = down ? "damage-down" : "damage-up";
            double nextPrice = multiplierManager.getPrice(
                    player, worldName, priceType,
                    worldData.getDamageUpgradeCost(down)
            );
            player.sendMessage("§7Следующая цена: §e" + currencyManager.formatCurrency(nextPrice));
        } else {
            player.sendMessage("§c❌ " + result.getMessage());
        }
    }

    private void handleStatus(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendStatusInfo(player, worldData);
    }

    private void sendStatusInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eСтатус границы: §a" + data.getWorldName());
        player.sendMessage("§6──────────────────────────────────────");

        // Основная информация
        player.sendMessage("§7Размер: §e" + String.format("%.1f", data.getCurrentSize()) +
                "§7/§a" + String.format("%.1f", data.getPlayerMaxSize()) +
                " §7блоков");
        player.sendMessage("§7Скорость: §e" + String.format("%.1f", data.getCurrentSpeed()) +
                "§7/§a" + String.format("%.1f", data.getPlayerMaxSpeed()) +
                " §7блоков/сек");
        player.sendMessage("§7Урон: §e" + String.format("%.1f", data.getCurrentDamage()) +
                "§7/§a" + String.format("%.1f", data.getPlayerMaxDamage()) +
                " §7урона/сек");

        // Лимиты
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Абсолютные лимиты:");
        player.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getAbsoluteMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getAbsoluteMaxSize()));
        player.sendMessage("  §7Скорость: §a" + String.format("%.2f", data.getAbsoluteMinSpeed()) +
                "§7 - §a" + String.format("%.2f", data.getAbsoluteMaxSpeed()));
        player.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getAbsoluteMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getAbsoluteMaxDamage()));

        // Цены
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Текущие цены:");
        player.sendMessage("  §7Расширение: §a" + currencyManager.formatCurrency(data.getExpandCost()) + "§7/блок");
        player.sendMessage("  §7Сужение: §a" + currencyManager.formatCurrency(data.getShrinkCost()) + "§7/блок");
        player.sendMessage("  §7Ускорение: §a" + currencyManager.formatCurrency(data.getSpeedUpCost()));
        player.sendMessage("  §7Замедление: §a" + currencyManager.formatCurrency(data.getSpeedDownCost()));
        player.sendMessage("  §7-Урон: §a" + currencyManager.formatCurrency(data.getDamageDownCost()));
        player.sendMessage("  §7+Урон: §a" + currencyManager.formatCurrency(data.getDamageUpCost()));

        // Статистика
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Статистика мира:");
        player.sendMessage("  §7Расширений: §a" + data.getTotalExpansions());
        player.sendMessage("  §7Сужений: §a" + data.getTotalShrinks());
        player.sendMessage("  §7Потрачено: §a" + currencyManager.formatCurrency(data.getTotalCurrencySpent()));
        player.sendMessage("  §7Заработано: §a" + currencyManager.formatCurrency(data.getTotalCurrencyEarned()));

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleBalance(Player player) {
        double balance = currencyManager.getBalance(player);
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eВаш баланс: §a" + currencyManager.formatCurrency(balance));

        // Показываем топ-5 предметов для продажи
        Map<String, Double> topItems = currencyManager.getTopSellableItems(5);
        if (!topItems.isEmpty()) {
            player.sendMessage("§6──────────────────────────────────────");
            player.sendMessage("§eТоп предметов для продажи:");
            int i = 1;
            for (Map.Entry<String, Double> entry : topItems.entrySet()) {
                player.sendMessage("§7" + i + ". §f" + entry.getKey() + " §7- §a" +
                        currencyManager.formatCurrency(entry.getValue()));
                i++;
            }
        }

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border sell <предмет> [количество]");
            player.sendMessage("§cИли: §e/border sell §7(предмет в руке)");
            return;
        }

        // Реализация продажи будет в CurrencyManager
        player.sendMessage("§aФункция продажи в разработке...");
    }

    private void handlePrices(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendPricesInfo(player, worldData);
    }

    private void sendPricesInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eЦены в мире: §a" + data.getWorldName());
        player.sendMessage("§6──────────────────────────────────────");

        // Базовая информация
        player.sendMessage("§7Базовые цены:");
        player.sendMessage("  §7Расширение: §a" + currencyManager.formatCurrency(data.getExpandCost()) + "§7/блок");
        player.sendMessage("  §7Сужение: §a" + currencyManager.formatCurrency(data.getShrinkCost()) + "§7/блок");
        player.sendMessage("  §7Ускорение: §a" + currencyManager.formatCurrency(data.getSpeedUpCost()));
        player.sendMessage("  §7Замедление: §a" + currencyManager.formatCurrency(data.getSpeedDownCost()));
        player.sendMessage("  §7-Урон: §a" + currencyManager.formatCurrency(data.getDamageDownCost()));
        player.sendMessage("  §7+Урон: §a" + currencyManager.formatCurrency(data.getDamageUpCost()));

        // Множители
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Текущие множители:");
        player.sendMessage("  §7Размер: §a×" + String.format("%.2f", data.getPriceMultiplierSize()));
        player.sendMessage("  §7Скорость: §a×" + String.format("%.2f", data.getPriceMultiplierSpeed()));
        player.sendMessage("  §7Урон: §a×" + String.format("%.2f", data.getPriceMultiplierDamage()));

        // Предметы для продажи в этом мире
        Map<String, Double> items = data.getWorldItemPrices();
        if (!items.isEmpty()) {
            player.sendMessage("§6──────────────────────────────────────");
            player.sendMessage("§7Предметы для продажи:");
            for (Map.Entry<String, Double> entry : items.entrySet()) {
                player.sendMessage("  §7- §f" + entry.getKey() + " §7- §a" +
                        currencyManager.formatCurrency(entry.getValue()));
            }
        }

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleItems(Player player) {
        Map<String, Double> allItems = currencyManager.getAllSellableItems();

        if (allItems.isEmpty()) {
            player.sendMessage("§cНет предметов для продажи!");
            return;
        }

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eВсе предметы для продажи");
        player.sendMessage("§6──────────────────────────────────────");

        int i = 1;
        for (Map.Entry<String, Double> entry : allItems.entrySet()) {
            player.sendMessage("§7" + i + ". §f" + entry.getKey() + " §7- §a" +
                    currencyManager.formatCurrency(entry.getValue()));
            i++;
        }

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§7Используйте: §e/border sell <предмет> [количество]");
    }

    private void handleUpgrades(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isUpgradable()) {
            player.sendMessage("§cУлучшения отключены для этого мира!");
            return;
        }

        sendUpgradesInfo(player, worldData);
    }

    private void sendUpgradesInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eУлучшения в мире: §a" + data.getWorldName());
        player.sendMessage("§6──────────────────────────────────────");

        // Доступные улучшения
        player.sendMessage("§7Доступные улучшения:");
        player.sendMessage("  §a/border speed up §7- Увеличить скорость");
        player.sendMessage("  §a/border speed down §7- Уменьшить скорость");
        player.sendMessage("  §a/border damage down §7- Уменьшить урон");
        player.sendMessage("  §a/border damage up §7- Увеличить урон");

        // Текущие значения
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Текущие значения:");
        player.sendMessage("  §7Скорость: §e" + String.format("%.1f", data.getCurrentSpeed()) +
                " §7(Шаг: §a" + String.format("%.2f", data.getUpgradeStepSpeed()) + "§7)");
        player.sendMessage("  §7Урон: §e" + String.format("%.1f", data.getCurrentDamage()) +
                " §7(Шаг: §a" + String.format("%.2f", data.getUpgradeStepDamage()) + "§7)");

        // Лимиты улучшений
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Лимиты улучшений:");
        player.sendMessage("  §7Скорость: §a" + String.format("%.1f", data.getPlayerMinSpeed()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxSpeed()));
        player.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getPlayerMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxDamage()));

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleEffects(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = player.getWorld().getName();
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendEffectsInfo(player, worldData);
    }

    private void sendEffectsInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eЭффекты границы: §a" + data.getWorldName());
        player.sendMessage("§6──────────────────────────────────────");

        // Информация об уроне
        if (data.getCurrentDamage() > 0) {
            player.sendMessage("§c⚠ Урон за границей:");
            player.sendMessage("  §7Количество: §c" + String.format("%.1f", data.getCurrentDamage()) + " урона/сек");
            player.sendMessage("  §7Буфер: §e" + String.format("%.1f", data.getDamageBuffer()) + " блоков");
            player.sendMessage("  §7Предупреждение: §e" + String.format("%.0f", data.getWarningDistance()) + " блоков");
        } else {
            player.sendMessage("§a✓ Урон отключен");
        }

        // Информация о скорости
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§e⚡ Скорость изменения:");
        player.sendMessage("  §7Текущая: §e" + String.format("%.1f", data.getCurrentSpeed()) + " блоков/сек");

        // Расчет времени для разных расстояний
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Время для изменения на:");
        player.sendMessage("  §7100 блоков: §e" + formatTime(100 / data.getCurrentSpeed()));
        player.sendMessage("  §71000 блоков: §e" + formatTime(1000 / data.getCurrentSpeed()));
        player.sendMessage("  §710000 блоков: §e" + formatTime(10000 / data.getCurrentSpeed()));

        player.sendMessage("§6══════════════════════════════════════");
    }

    private String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format("%.0f сек", seconds);
        } else if (seconds < 3600) {
            return String.format("%.1f мин", seconds / 60);
        } else {
            return String.format("%.1f час", seconds / 3600);
        }
    }

    private void handleWorlds(Player player) {
        Map<String, WorldBorderData> allWorlds = borderManager.getAllWorldData();

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eДоступные миры");
        player.sendMessage("§6──────────────────────────────────────");

        for (Map.Entry<String, WorldBorderData> entry : allWorlds.entrySet()) {
            WorldBorderData data = entry.getValue();
            String status = data.isEnabled() ? "§a✓" : "§c✗";
            String upgradable = data.isUpgradable() ? "§aУлучш." : "§cНет";

            player.sendMessage(String.format("%s §7%s §8| §7Размер: §e%.0f §8| §7Улучш.: %s",
                    status, entry.getKey(), data.getCurrentSize(), upgradable));
        }

        String selectedWorld = borderManager.getPlayerSelectedWorld(player);
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Выбранный мир: §e" + selectedWorld);
        player.sendMessage("§7Используйте: §e/border switch <мир>");
        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleSwitch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользуйте: §e/border switch <мир>");
            player.sendMessage("§cДоступные миры: §e" + String.join(", ", borderManager.getAvailableWorlds()));
            return;
        }

        String worldName = args[1];
        WorldBorderData worldData = borderManager.getWorldData(worldName);

        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        if (!worldData.isEnabled()) {
            player.sendMessage("§cВнимание! Система границы отключена для этого мира!");
        }

        borderManager.setPlayerSelectedWorld(player, worldName);
        player.sendMessage("§aВыбран мир: §e" + worldName);
        player.sendMessage("§7Теперь вы можете использовать быстрые команды:");
        player.sendMessage("  §e/brd expand <блоков> §7- Расширить границу");
        player.sendMessage("  §e/brd shrink <блоков> §7- Сузить границу");
        player.sendMessage("  §e/brd speed up/down §7- Изменить скорость");
        player.sendMessage("  §e/brd damage up/down §7- Изменить урон");
        player.sendMessage("  §e/brd status §7- Статус мира");
    }

    private void handleLimits(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendLimitsInfo(player, worldData);
    }

    private void sendLimitsInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eЛимиты мира: §a" + data.getWorldName());
        player.sendMessage("§6──────────────────────────────────────");

        // Абсолютные лимиты
        player.sendMessage("§cАбсолютные лимиты (невозможно превысить):");
        player.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getAbsoluteMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getAbsoluteMaxSize()));
        player.sendMessage("  §7Скорость: §a" + String.format("%.2f", data.getAbsoluteMinSpeed()) +
                "§7 - §a" + String.format("%.2f", data.getAbsoluteMaxSpeed()));
        player.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getAbsoluteMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getAbsoluteMaxDamage()));

        // Лимиты для игроков
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§eЛимиты для игроков:");
        player.sendMessage("  §7Размер: §a" + String.format("%.0f", data.getPlayerMinSize()) +
                "§7 - §a" + String.format("%.0f", data.getPlayerMaxSize()));
        player.sendMessage("  §7Скорость: §a" + String.format("%.1f", data.getPlayerMinSpeed()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxSpeed()));
        player.sendMessage("  §7Урон: §a" + String.format("%.1f", data.getPlayerMinDamage()) +
                "§7 - §a" + String.format("%.1f", data.getPlayerMaxDamage()));

        // Шаги улучшений
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Шаги улучшений:");
        player.sendMessage("  §7Размер: §a" + String.format("%.1f", data.getUpgradeStepSize()) + " блоков");
        player.sendMessage("  §7Скорость: §a" + String.format("%.2f", data.getUpgradeStepSpeed()) + " блоков/сек");
        player.sendMessage("  §7Урон: §a" + String.format("%.2f", data.getUpgradeStepDamage()) + " урона/сек");

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleInfo(Player player, String[] args) {
        String worldName;

        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        sendDetailedInfo(player, worldData);
    }

    private void sendDetailedInfo(Player player, WorldBorderData data) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eПодробная информация: §a" + data.getWorldName());
        player.sendMessage("§6══════════════════════════════════════");

        // Все параметры
        String status = data.isEnabled() ? "§aВключена" : "§cОтключена";
        String upgradable = data.isUpgradable() ? "§aДа" : "§cНет";
        String shop = data.isShopEnabled() ? "§aОткрыт" : "§cЗакрыт";

        player.sendMessage("§7Статус: " + status);
        player.sendMessage("§7Улучшения: " + upgradable);
        player.sendMessage("§7Магазин: " + shop);

        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Текущие параметры:");
        player.sendMessage("  §7Размер: §e" + String.format("%.0f", data.getCurrentSize()) + " блоков");
        player.sendMessage("  §7Скорость: §e" + String.format("%.2f", data.getCurrentSpeed()) + " блоков/сек");
        player.sendMessage("  §7Урон: §e" + String.format("%.1f", data.getCurrentDamage()) + " урона/сек");
        player.sendMessage("  §7Буфер урона: §e" + String.format("%.1f", data.getDamageBuffer()) + " блоков");
        player.sendMessage("  §7Дистанция предупреждения: §e" + String.format("%.0f", data.getWarningDistance()) + " блоков");

        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Статистика мира:");
        player.sendMessage("  §7Всего расширений: §a" + data.getTotalExpansions());
        player.sendMessage("  §7Всего сужений: §a" + data.getTotalShrinks());
        player.sendMessage("  §7Ускорений: §a" + data.getTotalSpeedUpgrades());
        player.sendMessage("  §7Замедлений: §a" + data.getTotalSpeedDowngrades());
        player.sendMessage("  §7Увеличений урона: §a" + data.getTotalDamageUpgrades());
        player.sendMessage("  §7Уменьшений урона: §a" + data.getTotalDamageDowngrades());
        player.sendMessage("  §7Всего потрачено: §a" + currencyManager.formatCurrency(data.getTotalCurrencySpent()));
        player.sendMessage("  §7Всего заработано: §a" + currencyManager.formatCurrency(data.getTotalCurrencyEarned()));

        // Время создания и модификации
        java.util.Date created = new java.util.Date(data.getCreationTime());
        java.util.Date modified = new java.util.Date(data.getLastModified());

        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Создан: §e" + created);
        player.sendMessage("§7Изменен: §e" + modified);

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handlePrice(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользуйте: §e/border price <next/history> <тип> [мир]");
            player.sendMessage("§cТипы: expand, shrink, speed-up, speed-down, damage-up, damage-down");
            return;
        }

        String action = args[1].toLowerCase();
        String priceType = args[2].toLowerCase();
        String worldName;

        if (args.length >= 4) {
            worldName = args[3];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        WorldBorderData worldData = borderManager.getWorldData(worldName);
        if (worldData == null) {
            player.sendMessage("§cМир §e" + worldName + "§c не найден!");
            return;
        }

        switch (action) {
            case "next":
                handlePriceNext(player, worldName, priceType, worldData);
                break;

            case "history":
                handlePriceHistory(player, worldName, priceType);
                break;

            default:
                player.sendMessage("§cИспользуйте: §enext §cили §ehistory");
                break;
        }
    }

    private void handlePriceNext(Player player, String worldName, String priceType, WorldBorderData data) {
        // Получаем базовую цену в зависимости от типа
        double basePrice = 0;
        switch (priceType) {
            case "expand":
                basePrice = data.getExpandCost();
                break;
            case "shrink":
                basePrice = data.getShrinkCost();
                break;
            case "speed-up":
                basePrice = data.getSpeedUpCost();
                break;
            case "speed-down":
                basePrice = data.getSpeedDownCost();
                break;
            case "damage-down":
                basePrice = data.getDamageDownCost();
                break;
            case "damage-up":
                basePrice = data.getDamageUpCost();
                break;
            default:
                player.sendMessage("§cНеизвестный тип цены: §e" + priceType);
                return;
        }

        // Получаем следующую цену с учетом множителя
        double nextPrice = multiplierManager.getPrice(player, worldName, priceType, basePrice);

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eСледующая цена: §a" + priceType);
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Базовая цена: §a" + currencyManager.formatCurrency(basePrice));
        player.sendMessage("§7Следующая цена: §e" + currencyManager.formatCurrency(nextPrice));

        // Получаем информацию о множителе
        MultiplierManager.MultiplierInfo info = multiplierManager.getMultiplierInfo(priceType);
        if (info != null) {
            player.sendMessage("§7Тип множителя: §a" + info.getType());
            player.sendMessage("§7Текущий множитель: §a×" + String.format("%.3f", nextPrice / basePrice));
        }

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handlePriceHistory(Player player, String worldName, String priceType) {
        MultiplierManager.PlayerMultiplierStats stats = multiplierManager.getPlayerStats(player, worldName);
        if (stats == null) {
            player.sendMessage("§cНет истории покупок для этого типа!");
            return;
        }

        MultiplierManager.MultiplierInfo info = multiplierManager.getMultiplierInfo(priceType);
        if (info == null) {
            player.sendMessage("§cМножитель не найден для типа: §e" + priceType);
            return;
        }

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eИстория цен: §a" + priceType);
        player.sendMessage("§eМир: §a" + worldName);
        player.sendMessage("§6──────────────────────────────────────");

        int level = stats.getLevel(priceType);
        double lastPrice = stats.getLastPrice(priceType);

        player.sendMessage("§7Текущий уровень: §a" + level);
        player.sendMessage("§7Последняя цена: §a" + currencyManager.formatCurrency(lastPrice));
        player.sendMessage("§7Всего потрачено: §a" + currencyManager.formatCurrency(stats.getTotalSpent()));
        player.sendMessage("§7Всего покупок: §a" + stats.getTotalPurchases());

        // Показываем тип множителя
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Настройки множителя:");
        player.sendMessage("  §7Тип: §a" + info.getType());
        player.sendMessage("  §7База: §a" + info.getBaseValue());

        if (info.getType() == MultiplierManager.MultiplierType.LINEAR) {
            player.sendMessage("  §7Шаг: §a" + info.getStep());
        }

        player.sendMessage("  §7Лимиты: §a" + info.getMinMultiplier() + " - " + info.getMaxMultiplier());
        player.sendMessage("  §7Сброс: §a" + info.getResetSchedule());

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleStats(Player player) {
        double balance = currencyManager.getBalance(player);
        String selectedWorld = borderManager.getPlayerSelectedWorld(player);

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eВаша статистика");
        player.sendMessage("§6══════════════════════════════════════");

        player.sendMessage("§7Баланс: §a" + currencyManager.formatCurrency(balance));
        player.sendMessage("§7Выбранный мир: §a" + selectedWorld);

        // Получаем статистику по множителям
        MultiplierManager.PlayerMultiplierStats stats = multiplierManager.getPlayerStats(player, selectedWorld);
        if (stats != null && stats.getTotalPurchases() > 0) {
            player.sendMessage("§6──────────────────────────────────────");
            player.sendMessage("§7Статистика улучшений:");
            player.sendMessage("  §7Всего покупок: §a" + stats.getTotalPurchases());
            player.sendMessage("  §7Всего потрачено: §a" + currencyManager.formatCurrency(stats.getTotalSpent()));

            // Показываем статистику по типам
            for (java.util.Map.Entry<String, org.dan.dynamicborder.data.PlayerMultiplierData> entry : stats.getMultipliers().entrySet()) {
                PlayerMultiplierData data = entry.getValue();
                if (data.getLevel() > 0) {
                    player.sendMessage("  §7- " + data.getPriceType() + ": §aур. " + data.getLevel() +
                            " (§e" + currencyManager.formatCurrency(data.getTotalSpent()) + "§7)");
                }
            }
        }

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleHelp(Player player, String[] args) {
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // Игнорируем, оставляем страницу 1
            }
        }

        sendHelp(player, page);
    }

    private void sendHelp(Player player, int page) {
        List<String> helpPages = new ArrayList<>();

        // Страница 1: Основные команды
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Помощь §7(Страница 1/4)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Основные команды:\n" +
                "§e/border expand <мир> <блоков> §7- Расширить границу\n" +
                "§e/border shrink <мир> <блоков> §7- Сузить границу\n" +
                "§e/border speed <up/down> [мир] §7- Изменить скорость\n" +
                "§e/border damage <up/down> [мир] §7- Изменить урон\n" +
                "§e/border status [мир] §7- Статус границы\n" +
                "§e/border balance §7- Ваш баланс\n" +
                "§e/border sell <предмет> §7- Продать предмет\n" +
                "§e/border help [страница] §7- Эта справка\n" +
                "§6══════════════════════════════════════");

        // Страница 2: Управление мирами
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Помощь §7(Страница 2/4)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Управление мирами:\n" +
                "§e/border worlds §7- Список миров\n" +
                "§e/border switch <мир> §7- Выбрать мир\n" +
                "§e/border prices [мир] §7- Цены в мире\n" +
                "§e/border items §7- Предметы для продажи\n" +
                "§e/border upgrades [мир] §7- Улучшения\n" +
                "§e/border effects [мир] §7- Эффекты границы\n" +
                "§e/border limits [мир] §7- Лимиты мира\n" +
                "§e/border info [мир] §7- Подробная информация\n" +
                "§6══════════════════════════════════════");

        // Страница 3: Быстрые команды
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Помощь §7(Страница 3/4)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Быстрые команды §7(после /border switch):\n" +
                "§e/brd expand <блоков> §7- Расширить выбранный мир\n" +
                "§e/brd shrink <блоков> §7- Сузить выбранный мир\n" +
                "§e/brd speed up/down §7- Изменить скорость\n" +
                "§e/brd damage up/down §7- Изменить урон\n" +
                "§e/brd status §7- Статус выбранного мира\n" +
                "§e/brd prices §7- Цены в выбранном мире\n" +
                "§e/brd limits §7- Лимиты выбранного мира\n" +
                "§e/brd info §7- Информация о выбранном мире\n" +
                "§6══════════════════════════════════════");

        // Страница 4: Цены и статистика
        helpPages.add("§6══════════════════════════════════════\n" +
                "§eDynamicWorldBorder - Помощь §7(Страница 4/4)\n" +
                "§6══════════════════════════════════════\n" +
                "§7Цены и статистика:\n" +
                "§e/border price next <тип> §7- Следующая цена\n" +
                "§e/border price history <тип> §7- История цен\n" +
                "§e/border stats §7- Ваша статистика\n" +
                "§7\n" +
                "§7Типы цен:\n" +
                "§7- §eexpand §7- Расширение\n" +
                "§7- §eshrink §7- Сужение\n" +
                "§7- §espeed-up §7- Ускорение\n" +
                "§7- §espeed-down §7- Замедление\n" +
                "§7- §edamage-up §7- Увеличение урона\n" +
                "§7- §edamage-down §7- Уменьшение урона\n" +
                "§6══════════════════════════════════════");

        // Проверяем номер страницы
        if (page < 1 || page > helpPages.size()) {
            page = 1;
        }

        // Отправляем нужную страницу
        player.sendMessage(helpPages.get(page - 1));

        // Показываем навигацию
        if (helpPages.size() > 1) {
            player.sendMessage("§7Используйте: §e/border help " + (page % helpPages.size() + 1) +
                    " §7для следующей страницы");
        }
    }
    private void handlePriceHistory(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользуйте: §e/border price history <тип>");
            player.sendMessage("§cТипы: expand, shrink, speed-up, speed-down, damage-up, damage-down");
            return;
        }

        String priceType = args[2].toLowerCase();
        String worldName;

        if (args.length >= 4) {
            worldName = args[3];
        } else {
            worldName = borderManager.getPlayerSelectedWorld(player);
        }

        // Получаем информацию о множителе
        MultiplierManager.MultiplierInfo info = multiplierManager.getMultiplierInfo(priceType);
        if (info == null) {
            player.sendMessage("§cМножитель не найден для типа: §e" + priceType);
            return;
        }

        // Получаем статистику игрока
        MultiplierManager.PlayerMultiplierStats stats = multiplierManager.getPlayerStats(player, worldName);

        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eИстория цен: §a" + priceType);
        player.sendMessage("§eМир: §a" + worldName);
        player.sendMessage("§6──────────────────────────────────────");

        if (stats != null) {
            int level = stats.getLevel(priceType);
            double lastPrice = stats.getLastPrice(priceType);

            player.sendMessage("§7Текущий уровень: §a" + level);
            player.sendMessage("§7Последняя цена: §a" + currencyManager.formatCurrency(lastPrice));
            player.sendMessage("§7Всего потрачено: §a" + currencyManager.formatCurrency(stats.getTotalSpent()));
            player.sendMessage("§7Всего покупок: §a" + stats.getTotalPurchases());
        } else {
            player.sendMessage("§7У вас нет истории покупок для этого типа");
        }

        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Настройки множителя:");
        player.sendMessage("  §7Тип: §a" + info.getType());
        player.sendMessage("  §7База: §a" + info.getBaseValue());

        if (info.getType() == MultiplierManager.MultiplierType.LINEAR) {
            player.sendMessage("  §7Шаг: §a" + info.getStep());
        } else if (info.getType() == MultiplierManager.MultiplierType.CUSTOM) {
            player.sendMessage("  §7Формула: §a" + info.getCustomFormula());
        }

        player.sendMessage("  §7Лимиты: §a" + info.getMinMultiplier() + " - " + info.getMaxMultiplier());
        player.sendMessage("  §7Сброс: §a" + info.getResetSchedule());

        player.sendMessage("§6══════════════════════════════════════");
    }

    private void handleVersion(Player player) {
        player.sendMessage("§6══════════════════════════════════════");
        player.sendMessage("§eDynamicWorldBorder v3.0");
        player.sendMessage("§7Разработчик: §aDan");
        player.sendMessage("§7Для Minecraft: §a1.21.9");
        player.sendMessage("§6──────────────────────────────────────");
        player.sendMessage("§7Функции:");
        player.sendMessage("§7• Динамические границы для миров");
        player.sendMessage("§7• Система улучшений скорости и урона");
        player.sendMessage("§7• Прогрессивные цены с множителями");
        player.sendMessage("§7• Магазин предметов");
        player.sendMessage("§7• Полный контроль для админов");
        player.sendMessage("§6══════════════════════════════════════");
    }

    // ========== АВТОДОПОЛНЕНИЕ ==========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Дополнение для первой команды
            return StringUtil.copyPartialMatches(args[0], PLAYER_SUBCOMMANDS, new ArrayList<>());
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "expand":
            case "shrink":
                if (args.length == 2) {
                    // Дополнение для имени мира
                    return StringUtil.copyPartialMatches(args[1], borderManager.getAvailableWorlds(), new ArrayList<>());
                } else if (args.length == 3) {
                    // Предлагаем числа
                    completions.add("10");
                    completions.add("50");
                    completions.add("100");
                    completions.add("500");
                    completions.add("1000");
                    return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                }
                break;

            case "speed":
            case "damage":
                if (args.length == 2) {
                    // up или down
                    completions.add("up");
                    completions.add("down");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                } else if (args.length == 3) {
                    // Дополнение для мира
                    return StringUtil.copyPartialMatches(args[2], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;

            case "status":
            case "prices":
            case "upgrades":
            case "effects":
            case "limits":
            case "info":
                if (args.length == 2) {
                    // Дополнение для мира
                    return StringUtil.copyPartialMatches(args[1], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;

            case "switch":
                if (args.length == 2) {
                    // Дополнение для мира
                    return StringUtil.copyPartialMatches(args[1], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;

            case "price":
                if (args.length == 2) {
                    // next или history
                    completions.add("next");
                    completions.add("history");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                } else if (args.length == 3) {
                    // Типы цен
                    completions.add("expand");
                    completions.add("shrink");
                    completions.add("speed-up");
                    completions.add("speed-down");
                    completions.add("damage-up");
                    completions.add("damage-down");
                    return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                } else if (args.length == 4) {
                    // Дополнение для мира
                    return StringUtil.copyPartialMatches(args[3], borderManager.getAvailableWorlds(), new ArrayList<>());
                }
                break;

            case "help":
                if (args.length == 2) {
                    // Номера страниц
                    completions.add("1");
                    completions.add("2");
                    completions.add("3");
                    completions.add("4");
                    return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                }
                break;

            case "sell":
                if (args.length == 2) {
                    // Предметы для продажи
                    return StringUtil.copyPartialMatches(args[1], currencyManager.getSellableItemNames(), new ArrayList<>());
                } else if (args.length == 3) {
                    // Количество
                    completions.add("1");
                    completions.add("10");
                    completions.add("64");
                    completions.add("all");
                    return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                }
                break;
        }

        return Collections.emptyList();
    }
}