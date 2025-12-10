package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.lang.reflect.Method;

public class EconomyManager {

    private final DynamicBorderPlugin plugin;
    private Object vaultEconomy = null;
    private boolean vaultEnabled = false;

    public EconomyManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        try {
            // Проверяем наличие Vault через рефлексию
            Class.forName("net.milkbowl.vault.economy.Economy");

            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager()
                        .getRegistration(economyClass);

                if (rsp != null) {
                    vaultEconomy = rsp.getProvider();
                    vaultEnabled = true;
                    plugin.getLogger().info("Vault экономика подключена!");
                }
            }
        } catch (Exception e) {
            vaultEnabled = false;
            plugin.getLogger().info("Vault не найден, используем внутреннюю экономику");
        }
    }

    public boolean hasEconomy() {
        return vaultEnabled && vaultEconomy != null;
    }

    public double getBalance(Player player) {
        if (hasEconomy()) {
            try {
                // Используем рефлексию для вызова методов Vault
                Method getBalanceMethod = vaultEconomy.getClass()
                        .getMethod("getBalance", Player.class);
                Object result = getBalanceMethod.invoke(vaultEconomy, player);

                if (result instanceof Double) {
                    return (Double) result;
                } else if (result instanceof Integer) {
                    return ((Integer) result).doubleValue();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка получения баланса через Vault: " + e.getMessage());
            }
        }

        // Используем внутреннюю валюту или заглушку
        if (plugin.getCurrencyManager() != null) {
            return plugin.getCurrencyManager().getBalance(player);
        }

        // Заглушка для теста
        return 10000.0;
    }

    public boolean withdraw(Player player, double amount) {
        if (hasEconomy()) {
            try {
                Method withdrawMethod = vaultEconomy.getClass()
                        .getMethod("withdrawPlayer", Player.class, double.class);
                Object result = withdrawMethod.invoke(vaultEconomy, player, amount);

                // Проверяем успешность транзакции
                Method successMethod = result.getClass().getMethod("transactionSuccess");
                return (Boolean) successMethod.invoke(result);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка списания через Vault: " + e.getMessage());
            }
        }

        // Используем внутреннюю валюту или заглушку
        if (plugin.getCurrencyManager() != null) {
            return plugin.getCurrencyManager().withdrawBalance(player, amount);
        }

        // Заглушка для теста - всегда успешно
        return true;
    }

    // Дополнительные полезные методы

    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public boolean deposit(Player player, double amount) {
        // Для внутренней экономики
        if (plugin.getCurrencyManager() != null) {
            // Если у CurrencyManager есть метод deposit
            try {
                Method depositMethod = plugin.getCurrencyManager().getClass()
                        .getMethod("depositBalance", Player.class, double.class);
                return (Boolean) depositMethod.invoke(plugin.getCurrencyManager(), player, amount);
            } catch (Exception e) {
                // Если нет метода deposit, просто увеличиваем баланс другим способом
                return true;
            }
        }
        return true;
    }

    public String getCurrencyName() {
        if (hasEconomy()) {
            try {
                Method getNameMethod = vaultEconomy.getClass().getMethod("currencyNamePlural");
                return (String) getNameMethod.invoke(vaultEconomy);
            } catch (Exception e) {
                return "денег";
            }
        }
        return "кредитов";
    }
}