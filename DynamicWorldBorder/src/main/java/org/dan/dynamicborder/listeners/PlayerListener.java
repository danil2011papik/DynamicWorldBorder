package org.dan.dynamicborder.listeners;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.dan.dynamicborder.managers.BorderManager;
import org.dan.dynamicborder.data.WorldBorderData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final DynamicBorderPlugin plugin;
    private final BorderManager borderManager;
    private final Map<UUID, BukkitTask> damageTasks = new HashMap<>();
    private final Map<String, Long> lastDamageTime = new HashMap<>();

    // Конфигурация
    private boolean enableBorderDamage = true;
    private boolean enableBorderWarning = true;
    private int warningDistance = 10;
    private long damageCooldown = 1000; // 1 секунда

    public PlayerListener(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        this.borderManager = plugin.getBorderManager();
        loadConfig();
    }

    private void loadConfig() {
        // Используем явный тип вместо var
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        enableBorderDamage = config.getBoolean("border.damage.enabled", true);
        enableBorderWarning = config.getBoolean("border.warning.enabled", true);
        warningDistance = config.getInt("border.warning.distance", 10);
        damageCooldown = config.getLong("border.damage.cooldown", 1000);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверка границы при входе
        checkPlayerBorder(player);

        // Запуск периодической проверки
        startBorderCheckTask(player);

        // Отправка информации о границе
        sendBorderInfo(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Остановка задач
        stopBorderCheckTask(player);

        // Очистка данных
        lastDamageTime.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enableBorderDamage && !enableBorderWarning) {
            return;
        }

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Проверяем, изменилась ли позиция по X или Z
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Проверка границы
        checkPlayerBorder(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Проверка границы после возрождения
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayerBorder(player), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Отменяем урон от границы, если он уже обработан нашим плагином
        if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            // Это наш урон, пропускаем
            return;
        }

        // Если игрок получает урон от границы, но мы его уже обработали
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
                event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            // Проверяем, не за границей ли игрок
            if (isOutsideBorder(player)) {
                // Игнорируем стандартный урон, т.к. мы сами обрабатываем границу
                event.setCancelled(true);
            }
        }
    }

    // ========== ОБРАБОТКА СОБЫТИЙ МИРОВ ==========

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        plugin.logInfo("Мир загружен: " + worldName);

        // Инициализация границы для нового мира
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            WorldBorderData data = borderManager.getWorldData(worldName);
            if (data != null && data.isEnabled()) {
                applyWorldBorder(world, data);
                plugin.logInfo("Граница применена для мира: " + worldName);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        plugin.logInfo("Мир выгружен: " + worldName);

        // Остановка задач для игроков этого мира
        for (Player player : world.getPlayers()) {
            stopBorderCheckTask(player);
        }
    }

    // ========== МЕТОДЫ ПРОВЕРКИ ГРАНИЦЫ ==========

    private void checkPlayerBorder(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        World world = player.getWorld();
        String worldName = world.getName();

        // Получаем данные границы
        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null || !data.isEnabled()) {
            return;
        }

        WorldBorder border = world.getWorldBorder();
        Location playerLoc = player.getLocation();
        Location borderCenter = border.getCenter();

        // Рассчитываем расстояние до границы
        double distanceToBorder = calculateDistanceToBorder(playerLoc, border);

        // Проверка предупреждения
        if (enableBorderWarning && distanceToBorder <= warningDistance) {
            sendBorderWarning(player, distanceToBorder, data);
        }

        // Проверка урона
        if (enableBorderDamage && isOutsideBorder(player)) {
            applyBorderDamage(player, data);
        }
    }

    private double calculateDistanceToBorder(Location location, WorldBorder border) {
        Location center = border.getCenter();
        double size = border.getSize() / 2.0;

        double dx = Math.abs(location.getX() - center.getX());
        double dz = Math.abs(location.getZ() - center.getZ());

        // Расстояние до ближайшей стороны границы
        double distanceX = Math.max(0, dx - size);
        double distanceZ = Math.max(0, dz - size);

        // Если внутри границы по обеим осям
        if (distanceX == 0 && distanceZ == 0) {
            // Находим минимальное расстояние до границы
            return Math.min(size - dx, size - dz);
        }

        // Если за границей по одной или обеим осям
        return Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
    }

    private boolean isOutsideBorder(Player player) {
        if (player == null) {
            return false;
        }

        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();
        Location playerLoc = player.getLocation();
        Location borderCenter = border.getCenter();

        double borderSize = border.getSize() / 2.0;
        double dx = Math.abs(playerLoc.getX() - borderCenter.getX());
        double dz = Math.abs(playerLoc.getZ() - borderCenter.getZ());

        return dx > borderSize || dz > borderSize;
    }

    private void applyBorderDamage(Player player, WorldBorderData data) {
        if (player == null || data == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        double damage = data.getCurrentDamage();

        // Проверка кулдауна
        long now = System.currentTimeMillis();
        Long lastDamage = lastDamageTime.get(uuid.toString());

        if (lastDamage != null && (now - lastDamage) < damageCooldown) {
            return;
        }

        // Применяем урон
        if (damage > 0) {
            // Проверяем креатив и режим призрака
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                    player.getGameMode() == org.bukkit.GameMode.SPECTATOR ||
                    player.isDead()) {
                return;
            }

            // Наносим урон
            player.damage(damage);
            lastDamageTime.put(uuid.toString(), now);

            // Визуальные эффекты
            showBorderDamageEffects(player);

            // Сообщение об уроне (если включено)
            if (data.getCurrentDamage() > 0) {
                player.sendMessage("§cВы находитесь за границей! Урон: §4" +
                        String.format("%.1f", damage) + "§c/сек");
            }
        }
    }

    private void showBorderDamageEffects(Player player) {
        // Визуальные эффекты при уроне от границы
        if (plugin.isDebugMode()) {
            player.spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);
        }
    }

    // ========== ИНФОРМАЦИЯ И ПРЕДУПРЕЖДЕНИЯ ==========

    private void sendBorderInfo(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        World world = player.getWorld();
        String worldName = world.getName();

        WorldBorderData data = borderManager.getWorldData(worldName);
        if (data == null || !data.isEnabled()) {
            return;
        }

        // Отправляем информацию о границе (если включено в конфиге)
        boolean showInfoOnJoin = plugin.getConfig().getBoolean("border.info.show-on-join", true);

        if (showInfoOnJoin) {
            List<String> info = createBorderInfo(player, data);
            for (String line : info) {
                player.sendMessage(line);
            }
        }
    }

    private List<String> createBorderInfo(Player player, WorldBorderData data) {
        List<String> info = new ArrayList<>();

        info.add("§6══════════════════════════════════════");
        info.add("§eИнформация о границе мира");
        info.add("§6══════════════════════════════════════");
        info.add("§7Размер: §a" + String.format("%.0f", data.getCurrentSize()) + " §7блоков");
        info.add("§7Урон за границей: §a" + String.format("%.1f", data.getCurrentDamage()) + " §7урона/сек");
        info.add("§7Предупреждение: §a" + String.format("%.0f", data.getWarningDistance()) + " §7блоков");

        if (data.getCurrentDamage() > 0) {
            info.add("§c⚠ Будьте осторожны за границей!");
        }

        info.add("§6══════════════════════════════════════");

        return info;
    }

    private void sendBorderWarning(Player player, double distance, WorldBorderData data) {
        if (player == null || !enableBorderWarning) {
            return;
        }

        // Проверяем кулдаун для предупреждений
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastWarning = lastDamageTime.get(uuid + "_warning");

        if (lastWarning != null && (now - lastWarning) < 3000) { // 3 секунды кулдаун
            return;
        }

        // Отправляем предупреждение
        String message;
        if (distance <= 0) {
            message = "§cВы на границе!";
        } else if (distance <= 5) {
            message = "§e⚠ Вы приближаетесь к границе!";
        } else {
            message = "§6⚠ Вы близко к границе (" + String.format("%.0f", distance) + " блоков)";
        }

        player.sendMessage(message);
        lastDamageTime.put(uuid + "_warning", now);

        // Визуальный эффект
        if (distance <= 5) {
            player.spawnParticle(org.bukkit.Particle.HEART,
                    player.getLocation().add(0, 2, 0), 3, 0.5, 0.5, 0.5);
        }
    }

    // ========== УПРАВЛЕНИЕ ЗАДАЧАМИ ==========

    private void startBorderCheckTask(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Останавливаем существующую задачу
        stopBorderCheckTask(player);

        // Создаем новую задачу для периодической проверки
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                checkPlayerBorder(player);
            }
        }, 20L, 20L); // Каждую секунду

        damageTasks.put(uuid, task);
    }

    private void stopBorderCheckTask(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        BukkitTask task = damageTasks.remove(uuid);

        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void applyWorldBorder(World world, WorldBorderData data) {
        if (world == null || data == null) {
            return;
        }

        WorldBorder border = world.getWorldBorder();

        // Устанавливаем центр, если он не установлен
        if (border.getCenter().getX() == 0 && border.getCenter().getZ() == 0) {
            // Устанавливаем центр в спавн мира или на позицию первого игрока
            Location spawnLocation = world.getSpawnLocation();
            border.setCenter(spawnLocation.getX(), spawnLocation.getZ());
        }

        // Устанавливаем параметры
        border.setSize(data.getCurrentSize());
        border.setDamageAmount(data.getCurrentDamage());
        border.setDamageBuffer(data.getDamageBuffer());
        border.setWarningDistance((int) data.getWarningDistance());
    }

    /**
     * Обновить настройки из конфига
     */
    public void reloadConfig() {
        loadConfig();

        // Останавливаем все задачи
        for (BukkitTask task : damageTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        damageTasks.clear();

        // Перезапускаем задачи для онлайн игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            startBorderCheckTask(player);
        }
    }

    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        // Останавливаем все задачи
        for (BukkitTask task : damageTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        damageTasks.clear();

        // Очищаем карты
        lastDamageTime.clear();
    }
}