package org.dan.dynamicborder.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("PlayerMultiplierData")
public class PlayerMultiplierData implements ConfigurationSerializable {

    private String priceType;          // Тип цены (expand, speed-up и т.д.)
    private String worldName;          // Имя мира
    private int level;                 // Текущий уровень
    private double lastPrice;          // Последняя уплаченная цена
    private double totalSpent;         // Всего потрачено на этот тип
    private long lastPurchaseTime;     // Время последней покупки
    private long lastResetTime;        // Время последнего сброса

    // Дополнительная статистика
    private int purchasesToday;        // Покупок сегодня
    private int purchasesThisWeek;     // Покупок на этой неделе
    private int purchasesThisMonth;    // Покупок в этом месяце
    private double spentToday;         // Потрачено сегодня
    private double spentThisWeek;      // Потрачено на этой неделе
    private double spentThisMonth;     // Потрачено в этом месяце

    public PlayerMultiplierData() {
        this.level = 0;
        this.lastPrice = 0.0;
        this.totalSpent = 0.0;
        this.lastPurchaseTime = 0L;
        this.lastResetTime = System.currentTimeMillis();

        this.purchasesToday = 0;
        this.purchasesThisWeek = 0;
        this.purchasesThisMonth = 0;
        this.spentToday = 0.0;
        this.spentThisWeek = 0.0;
        this.spentThisMonth = 0.0;
    }

    // ========== ГЕТТЕРЫ ==========
    public String getPriceType() { return priceType; }
    public String getWorldName() { return worldName; }
    public int getLevel() { return level; }
    public double getLastPrice() { return lastPrice; }
    public double getTotalSpent() { return totalSpent; }
    public long getLastPurchaseTime() { return lastPurchaseTime; }
    public long getLastResetTime() { return lastResetTime; }

    public int getPurchasesToday() { return purchasesToday; }
    public int getPurchasesThisWeek() { return purchasesThisWeek; }
    public int getPurchasesThisMonth() { return purchasesThisMonth; }
    public double getSpentToday() { return spentToday; }
    public double getSpentThisWeek() { return spentThisWeek; }
    public double getSpentThisMonth() { return spentThisMonth; }

    // ========== СЕТТЕРЫ ==========
    public void setPriceType(String priceType) { this.priceType = priceType; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }
    public void setLastPrice(double lastPrice) {
        this.lastPrice = Math.max(0.0, lastPrice);
    }
    public void setTotalSpent(double totalSpent) {
        this.totalSpent = Math.max(0.0, totalSpent);
    }
    public void setLastPurchaseTime(long lastPurchaseTime) {
        this.lastPurchaseTime = Math.max(0L, lastPurchaseTime);
    }
    public void setLastResetTime(long lastResetTime) {
        this.lastResetTime = Math.max(0L, lastResetTime);
    }

    public void setPurchasesToday(int purchasesToday) {
        this.purchasesToday = Math.max(0, purchasesToday);
    }
    public void setPurchasesThisWeek(int purchasesThisWeek) {
        this.purchasesThisWeek = Math.max(0, purchasesThisWeek);
    }
    public void setPurchasesThisMonth(int purchasesThisMonth) {
        this.purchasesThisMonth = Math.max(0, purchasesThisMonth);
    }
    public void setSpentToday(double spentToday) {
        this.spentToday = Math.max(0.0, spentToday);
    }
    public void setSpentThisWeek(double spentThisWeek) {
        this.spentThisWeek = Math.max(0.0, spentThisWeek);
    }
    public void setSpentThisMonth(double spentThisMonth) {
        this.spentThisMonth = Math.max(0.0, spentThisMonth);
    }

    // ========== МЕТОДЫ ОБНОВЛЕНИЯ ==========

    /**
     * Обновить данные после покупки
     */
    public void updateAfterPurchase(double price) {
        this.level++;
        this.lastPrice = price;
        this.totalSpent += price;
        this.lastPurchaseTime = System.currentTimeMillis();

        // Обновление статистики по периодам
        updatePeriodStats(price);
    }

    /**
     * Обновить статистику по периодам
     */
    private void updatePeriodStats(double price) {
        // Здесь можно добавить логику для обновления статистики
        // по дням/неделям/месяцам на основе lastPurchaseTime
        this.purchasesToday++;
        this.purchasesThisWeek++;
        this.purchasesThisMonth++;

        this.spentToday += price;
        this.spentThisWeek += price;
        this.spentThisMonth += price;
    }

    /**
     * Сбросить дневную статистику
     */
    public void resetDailyStats() {
        this.purchasesToday = 0;
        this.spentToday = 0.0;
    }

    /**
     * Сбросить недельную статистику
     */
    public void resetWeeklyStats() {
        this.purchasesThisWeek = 0;
        this.spentThisWeek = 0.0;
    }

    /**
     * Сбросить месячную статистику
     */
    public void resetMonthlyStats() {
        this.purchasesThisMonth = 0;
        this.spentThisMonth = 0.0;
    }

    /**
     * Полный сброс
     */
    public void resetAll() {
        this.level = 0;
        this.lastPrice = 0.0;
        this.totalSpent = 0.0;
        this.lastResetTime = System.currentTimeMillis();

        resetDailyStats();
        resetWeeklyStats();
        resetMonthlyStats();
    }

    /**
     * Получить среднюю цену за покупку
     */
    public double getAveragePrice() {
        return level > 0 ? totalSpent / level : 0.0;
    }

    /**
     * Получить множитель на основе уровня
     */
    public double getCurrentMultiplier() {
        // Базовая формула: 1 + (level * 0.01)
        return 1.0 + (level * 0.01);
    }

    // ========== СЕРИАЛИЗАЦИЯ ==========

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("priceType", priceType);
        data.put("worldName", worldName);
        data.put("level", level);
        data.put("lastPrice", lastPrice);
        data.put("totalSpent", totalSpent);
        data.put("lastPurchaseTime", lastPurchaseTime);
        data.put("lastResetTime", lastResetTime);

        data.put("purchasesToday", purchasesToday);
        data.put("purchasesThisWeek", purchasesThisWeek);
        data.put("purchasesThisMonth", purchasesThisMonth);
        data.put("spentToday", spentToday);
        data.put("spentThisWeek", spentThisWeek);
        data.put("spentThisMonth", spentThisMonth);

        return data;
    }

    public static PlayerMultiplierData deserialize(Map<String, Object> data) {
        PlayerMultiplierData pmd = new PlayerMultiplierData();

        pmd.setPriceType((String) data.get("priceType"));
        pmd.setWorldName((String) data.get("worldName"));
        pmd.setLevel((int) data.getOrDefault("level", 0));
        pmd.setLastPrice((double) data.getOrDefault("lastPrice", 0.0));
        pmd.setTotalSpent((double) data.getOrDefault("totalSpent", 0.0));
        pmd.setLastPurchaseTime((long) data.getOrDefault("lastPurchaseTime", 0L));
        pmd.setLastResetTime((long) data.getOrDefault("lastResetTime", System.currentTimeMillis()));

        pmd.setPurchasesToday((int) data.getOrDefault("purchasesToday", 0));
        pmd.setPurchasesThisWeek((int) data.getOrDefault("purchasesThisWeek", 0));
        pmd.setPurchasesThisMonth((int) data.getOrDefault("purchasesThisMonth", 0));
        pmd.setSpentToday((double) data.getOrDefault("spentToday", 0.0));
        pmd.setSpentThisWeek((double) data.getOrDefault("spentThisWeek", 0.0));
        pmd.setSpentThisMonth((double) data.getOrDefault("spentThisMonth", 0.0));

        return pmd;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Получить ключ для хранения в карте
     */
    public String getKey() {
        return worldName + ":" + priceType;
    }

    /**
     * Проверить, устарели ли данные (больше 30 дней)
     */
    public boolean isStale() {
        long thirtyDays = 30L * 24L * 60L * 60L * 1000L;
        return System.currentTimeMillis() - lastPurchaseTime > thirtyDays;
    }

    /**
     * Получить время с последней покупки
     */
    public String getTimeSinceLastPurchase() {
        if (lastPurchaseTime == 0) return "никогда";

        long diff = System.currentTimeMillis() - lastPurchaseTime;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " дней";
        if (hours > 0) return hours + " часов";
        if (minutes > 0) return minutes + " минут";
        return seconds + " секунд";
    }

    @Override
    public String toString() {
        return String.format(
                "PlayerMultiplierData{type=%s, world=%s, level=%d, total=%.2f, last=%.2f}",
                priceType, worldName, level, totalSpent, lastPrice
        );
    }
}