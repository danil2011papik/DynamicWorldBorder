package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CurrencyManager {

    private final DynamicBorderPlugin plugin;
    private final Map<UUID, Double> playerBalances = new ConcurrentHashMap<>();
    private final Map<Material, Double> sellableItems = new ConcurrentHashMap<>();
    private final Set<UUID> blockedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> playerCacheTime = new ConcurrentHashMap<>();

    private boolean shopEnabled = true;
    private String currencyName = "Граничных блоков";
    private String currencySymbol = "⧈";
    private File balancesFile;
    private File itemsFile;

    private static final long CACHE_TIMEOUT = 300000; // 5 минут

    public CurrencyManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        // Создание файлов
        balancesFile = new File(plugin.getDataFolder(), "data/balances.yml");
        itemsFile = new File(plugin.getDataFolder(), "data/items.yml");

        // Загрузка данных
        loadBalances();
        loadItems();

        // Загрузка настроек из конфига
        loadConfigSettings();

        plugin.logInfo("CurrencyManager инициализирован");
        plugin.logInfo("Загружено балансов: " + playerBalances.size());
        plugin.logInfo("Загружено предметов для продажи: " + sellableItems.size());
    }

    private void loadConfigSettings() {
        var config = plugin.getConfigManager().getMainConfig();
        if (config != null) {
            currencyName = config.getString("settings.currency-name", "Граничных блоков");
            currencySymbol = config.getString("settings.currency-symbol", "⧈");
            shopEnabled = config.getBoolean("settings.shop-enabled", true);
        }
    }

    // ========== БАЛАНСЫ ==========

    /**
     * Получить баланс игрока
     */
    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    /**
     * Получить баланс по UUID
     */
    public double getBalance(UUID uuid) {
        // Проверка кэша
        Long cacheTime = playerCacheTime.get(uuid);
        if (cacheTime != null && System.currentTimeMillis() - cacheTime > CACHE_TIMEOUT) {
            // Кэш устарел, загружаем заново
            loadPlayerBalance(uuid);
        }

        return playerBalances.getOrDefault(uuid, 0.0);
    }

    /**
     * Установить баланс игрока
     */
    public boolean setBalance(Player player, double amount) {
        return setBalance(player.getUniqueId(), amount);
    }

    /**
     * Установить баланс по UUID
     */
    public boolean setBalance(UUID uuid, double amount) {
        if (amount < 0) {
            amount = 0;
        }

        playerBalances.put(uuid, amount);
        playerCacheTime.put(uuid, System.currentTimeMillis());

        // Асинхронное сохранение
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerBalance(uuid));

        return true;
    }

    /**
     * Добавить валюту
     */
    public boolean addBalance(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        double current = getBalance(uuid);
        double newBalance = current + amount;

        return setBalance(uuid, newBalance);
    }

    /**
     * Снять валюту
     */
    public boolean withdrawBalance(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        double current = getBalance(uuid);

        if (current < amount) {
            return false;
        }

        double newBalance = current - amount;
        return setBalance(uuid, newBalance);
    }

    /**
     * Проверить, достаточно ли валюты
     */
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Перевести валюту между игроками
     */
    public TransferResult transfer(Player from, Player to, double amount) {
        if (from == null || to == null || amount <= 0) {
            return new TransferResult(false, "Неверные параметры перевода");
        }

        if (from.equals(to)) {
            return new TransferResult(false, "Нельзя перевести валюту самому себе");
        }

        if (!hasEnough(from, amount)) {
            return new TransferResult(false, "Недостаточно валюты для перевода");
        }

        // Снимаем у отправителя
        if (!withdrawBalance(from, amount)) {
            return new TransferResult(false, "Ошибка списания валюты");
        }

        // Добавляем получателю
        if (!addBalance(to, amount)) {
            // Если не удалось добавить получателю, возвращаем отправителю
            addBalance(from, amount);
            return new TransferResult(false, "Ошибка зачисления валюты");
        }

        // Уведомляем игроков
        from.sendMessage(MessageUtils.format("&aВы перевели &e" + formatCurrency(amount) +
                " &aигроку &e" + to.getName()));
        to.sendMessage(MessageUtils.format("&aВы получили &e" + formatCurrency(amount) +
                " &aот игрока &e" + from.getName()));

        return new TransferResult(true, "Перевод успешно выполнен", amount, from.getName(), to.getName());
    }

    // ========== МАГАЗИН ==========

    /**
     * Продать предмет
     */
    public SellResult sellItem(Player player, Material material, int amount) {
        return sellItem(player, material, amount, false);
    }

    /**
     * Продать предмет (с расширенными опциями)
     */
    public SellResult sellItem(Player player, Material material, int amount, boolean sellAll) {
        // Проверка доступности магазина
        if (!shopEnabled) {
            return new SellResult(false, "Магазин временно отключен");
        }

        if (blockedPlayers.contains(player.getUniqueId())) {
            return new SellResult(false, "Вы заблокированы в магазине");
        }

        // Проверка предмета
        Double price = sellableItems.get(material);
        if (price == null || price <= 0) {
            return new SellResult(false, "Этот предмет нельзя продать");
        }

        // Подсчет предметов в инвентаре
        int totalInInventory = countItemsInInventory(player, material);

        if (totalInInventory <= 0) {
            return new SellResult(false, "У вас нет этого предмета в инвентаре");
        }

        // Определяем сколько продавать
        int toSell = amount;
        if (sellAll || amount > totalInInventory) {
            toSell = totalInInventory;
        }

        if (toSell <= 0) {
            return new SellResult(false, "Нечего продавать");
        }

        // Удаляем предметы из инвентаря
        int removed = removeItemsFromInventory(player, material, toSell);

        if (removed <= 0) {
            return new SellResult(false, "Не удалось удалить предметы из инвентаря");
        }

        // Рассчитываем стоимость
        double totalPrice = price * removed;

        // Добавляем валюту
        if (!addBalance(player, totalPrice)) {
            // Если не удалось добавить валюту, возвращаем предметы
            addItemsToInventory(player, material, removed);
            return new SellResult(false, "Ошибка зачисления валюты");
        }

        // Обновляем статистику мира
        updateWorldStats(player, material, removed, totalPrice);

        return new SellResult(true, "Предметы успешно проданы",
                removed, totalPrice, material.name());
    }

    /**
     * Продать предмет в руке
     */
    public SellResult sellHandItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return new SellResult(false, "У вас нет предмета в руке");
        }

        return sellItem(player, itemInHand.getType(), itemInHand.getAmount());
    }

    /**
     * Продать все предметы указанного типа
     */
    public SellResult sellAllItems(Player player, Material material) {
        return sellItem(player, material, 0, true);
    }

    /**
     * Добавить предмет для продажи
     */
    public boolean addSellableItem(Material material, double price) {
        if (material == null || price <= 0) {
            return false;
        }

        sellableItems.put(material, price);
        saveItems();
        return true;
    }

    /**
     * Удалить предмет из продажи
     */
    public boolean removeSellableItem(Material material) {
        if (material == null) {
            return false;
        }

        sellableItems.remove(material);
        saveItems();
        return true;
    }

    /**
     * Получить цену предмета
     */
    public Double getItemPrice(Material material) {
        return sellableItems.get(material);
    }

    /**
     * Получить все предметы для продажи
     */
    public Map<String, Double> getAllSellableItems() {
        Map<String, Double> items = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<Material, Double> entry : sellableItems.entrySet()) {
            items.put(entry.getKey().name(), entry.getValue());
        }

        return items;
    }

    /**
     * Получить топ предметов для продажи
     */
    public Map<String, Double> getTopSellableItems(int count) {
        return sellableItems.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(count)
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Получить имена всех предметов для продажи
     */
    public List<String> getSellableItemNames() {
        return sellableItems.keySet().stream()
                .map(Material::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // ========== УПРАВЛЕНИЕ ДОСТУПОМ ==========

    /**
     * Заблокировать игрока в магазине
     */
    public boolean blockPlayer(Player player) {
        return blockPlayer(player.getUniqueId());
    }

    /**
     * Заблокировать игрока по UUID
     */
    public boolean blockPlayer(UUID uuid) {
        boolean added = blockedPlayers.add(uuid);
        if (added) {
            saveBlockedPlayers();
        }
        return added;
    }

    /**
     * Разблокировать игрока
     */
    public boolean unblockPlayer(Player player) {
        return unblockPlayer(player.getUniqueId());
    }

    /**
     * Разблокировать игрока по UUID
     */
    public boolean unblockPlayer(UUID uuid) {
        boolean removed = blockedPlayers.remove(uuid);
        if (removed) {
            saveBlockedPlayers();
        }
        return removed;
    }

    /**
     * Проверить, заблокирован ли игрок
     */
    public boolean isPlayerBlocked(Player player) {
        return blockedPlayers.contains(player.getUniqueId());
    }

    /**
     * Включить/выключить магазин
     */
    public void setShopEnabled(boolean enabled) {
        this.shopEnabled = enabled;
        saveConfigSettings();
    }

    /**
     * Проверить, включен ли магазин
     */
    public boolean isShopEnabled() {
        return shopEnabled;
    }

    // ========== УТИЛИТЫ ==========

    /**
     * Форматирование валюты
     */
    public String formatCurrency(double amount) {
        return String.format("%,.2f %s", amount, currencySymbol);
    }

    /**
     * Форматирование цены предмета
     */
    public String formatItemPrice(Material material) {
        Double price = getItemPrice(material);
        if (price == null) {
            return "Не продается";
        }
        return formatCurrency(price);
    }

    /**
     * Создать информационное сообщение о балансе
     */
    public List<String> createBalanceInfo(Player player) {
        List<String> info = new ArrayList<>();

        double balance = getBalance(player);
        info.add("§6══════════════════════════════════════");
        info.add("§eВаш баланс: §a" + formatCurrency(balance));
        info.add("§6══════════════════════════════════════");

        return info;
    }

    /**
     * Создать информационное сообщение о магазине
     */
    public List<String> createShopInfo() {
        List<String> info = new ArrayList<>();

        info.add("§6══════════════════════════════════════");
        info.add("§eМагазин предметов");
        info.add("§6══════════════════════════════════════");
        info.add("§7Статус: " + (shopEnabled ? "§aОткрыт" : "§cЗакрыт"));
        info.add("§7Заблокировано игроков: §a" + blockedPlayers.size());
        info.add("§7Предметов для продажи: §a" + sellableItems.size());
        info.add("§6══════════════════════════════════════");

        return info;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private int countItemsInInventory(Player player, Material material) {
        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total;
    }

    private int removeItemsFromInventory(Player player, Material material, int amount) {
        int removed = 0;

        for (int i = 0; i < player.getInventory().getSize() && removed < amount; i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                int toRemove = Math.min(itemAmount, amount - removed);

                if (toRemove == itemAmount) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - toRemove);
                }

                removed += toRemove;
            }
        }

        player.updateInventory();
        return removed;
    }

    private void addItemsToInventory(Player player, Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);

        // Если инвентарь полон, выкидываем остатки
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.updateInventory();
    }

    private void updateWorldStats(Player player, Material material, int amount, double price) {
        // Здесь можно добавить обновление статистики мира
        // Например, увеличение totalCurrencyEarned в WorldBorderData
        String worldName = player.getWorld().getName();
        // Реализация зависит от структуры ваших данных
    }

    // ========== ЗАГРУЗКА И СОХРАНЕНИЕ ==========

    private void loadBalances() {
        if (!balancesFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(balancesFile);

            if (config.contains("balances")) {
                for (String uuidStr : config.getConfigurationSection("balances").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        double balance = config.getDouble("balances." + uuidStr, 0.0);
                        playerBalances.put(uuid, balance);
                    } catch (IllegalArgumentException e) {
                        plugin.logWarning("Неверный UUID в balances.yml: " + uuidStr);
                    }
                }
            }

            // Загрузка заблокированных игроков
            if (config.contains("blocked-players")) {
                for (String uuidStr : config.getStringList("blocked-players")) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        blockedPlayers.add(uuid);
                    } catch (IllegalArgumentException e) {
                        plugin.logWarning("Неверный UUID в blocked-players: " + uuidStr);
                    }
                }
            }

            // Загрузка настроек
            if (config.contains("settings")) {
                shopEnabled = config.getBoolean("settings.shop-enabled", true);
                currencyName = config.getString("settings.currency-name", "Граничных блоков");
                currencySymbol = config.getString("settings.currency-symbol", "⧈");
            }

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки balances.yml: " + e.getMessage());
        }
    }

    private void loadPlayerBalance(UUID uuid) {
        if (!balancesFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(balancesFile);
            String uuidStr = uuid.toString();

            if (config.contains("balances." + uuidStr)) {
                double balance = config.getDouble("balances." + uuidStr, 0.0);
                playerBalances.put(uuid, balance);
            }

            playerCacheTime.put(uuid, System.currentTimeMillis());

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки баланса игрока " + uuid + ": " + e.getMessage());
        }
    }

    private void savePlayerBalance(UUID uuid) {
        try {
            YamlConfiguration config = balancesFile.exists() ?
                    YamlConfiguration.loadConfiguration(balancesFile) : new YamlConfiguration();

            String uuidStr = uuid.toString();
            double balance = playerBalances.getOrDefault(uuid, 0.0);

            config.set("balances." + uuidStr, balance);
            config.set("balances." + uuidStr + ".last-update", System.currentTimeMillis());

            config.save(balancesFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения баланса игрока " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAllBalances() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            // Сохраняем балансы
            for (Map.Entry<UUID, Double> entry : playerBalances.entrySet()) {
                config.set("balances." + entry.getKey().toString(), entry.getValue());
            }

            // Сохраняем заблокированных игроков
            List<String> blockedList = blockedPlayers.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            config.set("blocked-players", blockedList);

            // Сохраняем настройки
            config.set("settings.shop-enabled", shopEnabled);
            config.set("settings.currency-name", currencyName);
            config.set("settings.currency-symbol", currencySymbol);
            config.set("settings.last-save", System.currentTimeMillis());

            config.save(balancesFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения всех балансов: " + e.getMessage());
        }
    }

    private void loadItems() {
        if (!itemsFile.exists()) {
            // Создаем дефолтные предметы
            createDefaultItems();
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

            if (config.contains("items")) {
                for (String materialStr : config.getConfigurationSection("items").getKeys(false)) {
                    try {
                        Material material = Material.valueOf(materialStr.toUpperCase());
                        double price = config.getDouble("items." + materialStr, 0.0);

                        if (price > 0) {
                            sellableItems.put(material, price);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.logWarning("Неизвестный материал в items.yml: " + materialStr);
                    }
                }
            }

        } catch (Exception e) {
            plugin.logError("Ошибка загрузки items.yml: " + e.getMessage());
            createDefaultItems();
        }
    }

    private void createDefaultItems() {
        // Дефолтные предметы для продажи
        Map<Material, Double> defaultItems = new HashMap<>();

        defaultItems.put(Material.DIAMOND, 100.0);
        defaultItems.put(Material.EMERALD, 50.0);
        defaultItems.put(Material.GOLD_INGOT, 25.0);
        defaultItems.put(Material.IRON_INGOT, 10.0);
        defaultItems.put(Material.COAL, 5.0);
        defaultItems.put(Material.REDSTONE, 3.0);
        defaultItems.put(Material.LAPIS_LAZULI, 3.0);
        defaultItems.put(Material.NETHERITE_INGOT, 500.0);
        defaultItems.put(Material.NETHERITE_SCRAP, 100.0);
        defaultItems.put(Material.ANCIENT_DEBRIS, 200.0);

        sellableItems.putAll(defaultItems);
        saveItems();

        plugin.logInfo("Созданы предметы для продажи по умолчанию");
    }

    private void saveItems() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<Material, Double> entry : sellableItems.entrySet()) {
                config.set("items." + entry.getKey().name(), entry.getValue());
            }

            config.set("last-update", System.currentTimeMillis());
            config.save(itemsFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения items.yml: " + e.getMessage());
        }
    }

    private void saveBlockedPlayers() {
        try {
            YamlConfiguration config = balancesFile.exists() ?
                    YamlConfiguration.loadConfiguration(balancesFile) : new YamlConfiguration();

            List<String> blockedList = blockedPlayers.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            config.set("blocked-players", blockedList);
            config.save(balancesFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения заблокированных игроков: " + e.getMessage());
        }
    }

    private void saveConfigSettings() {
        try {
            YamlConfiguration config = balancesFile.exists() ?
                    YamlConfiguration.loadConfiguration(balancesFile) : new YamlConfiguration();

            config.set("settings.shop-enabled", shopEnabled);
            config.set("settings.currency-name", currencyName);
            config.set("settings.currency-symbol", currencySymbol);

            config.save(balancesFile);

        } catch (IOException e) {
            plugin.logError("Ошибка сохранения настроек: " + e.getMessage());
        }
    }

    public void saveData() {
        saveAllBalances();
        saveItems();
    }

    public void cleanupCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = playerCacheTime.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now - entry.getValue() > CACHE_TIMEOUT) {
                // Сохраняем данные перед очисткой
                savePlayerBalance(entry.getKey());
                iterator.remove();
            }
        }
    }

    // ========== КЛАССЫ РЕЗУЛЬТАТОВ ==========

    public static class SellResult {
        private final boolean success;
        private final String message;
        private final int soldCount;
        private final double earned;
        private final String itemName;

        public SellResult(boolean success, String message) {
            this(success, message, 0, 0.0, null);
        }

        public SellResult(boolean success, String message, int soldCount, double earned, String itemName) {
            this.success = success;
            this.message = message;
            this.soldCount = soldCount;
            this.earned = earned;
            this.itemName = itemName;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getSoldCount() { return soldCount; }
        public double getEarned() { return earned; }
        public String getItemName() { return itemName; }
    }

    public static class TransferResult {
        private final boolean success;
        private final String message;
        private final double amount;
        private final String fromPlayer;
        private final String toPlayer;

        public TransferResult(boolean success, String message) {
            this(success, message, 0.0, null, null);
        }

        public TransferResult(boolean success, String message, double amount, String fromPlayer, String toPlayer) {
            this.success = success;
            this.message = message;
            this.amount = amount;
            this.fromPlayer = fromPlayer;
            this.toPlayer = toPlayer;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getAmount() { return amount; }
        public String getFromPlayer() { return fromPlayer; }
        public String getToPlayer() { return toPlayer; }
    }
}