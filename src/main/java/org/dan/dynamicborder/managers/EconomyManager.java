package org.dan.dynamicborder.managers;

import org.dan.dynamicborder.DynamicBorderPlugin;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final DynamicBorderPlugin plugin;

    public EconomyManager(DynamicBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasEconomy() {
        return plugin.getServer().getPluginManager().getPlugin("Vault") != null;
    }

    public double getBalance(Player player) {
        if (hasEconomy()) {
            // Интеграция с Vault
            net.milkbowl.vault.economy.Economy economy = plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();

            if (economy != null) {
                return economy.getBalance(player);
            }
        }

        // Используем нашу валюту
        return plugin.getCurrencyManager().getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (hasEconomy()) {
            net.milkbowl.vault.economy.Economy economy = plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();

            if (economy != null) {
                return economy.withdrawPlayer(player, amount).transactionSuccess();
            }
        }

        return plugin.getCurrencyManager().withdrawBalance(player, amount);
    }
}