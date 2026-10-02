package org.khmc.eventslib.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.khmc.shards.service.ShardsService;

import java.text.NumberFormat;
import java.util.Locale;

public class ShardsHook {

    private static ShardsService shardsService = null;
    private static boolean checked = false;

    public static void init() {
        checked = false;
        shardsService = null;
        getService();
    }

    public static ShardsService getService() {
        if (shardsService != null) {
            return shardsService;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Shards")) {
            RegisteredServiceProvider<ShardsService> rsp = Bukkit.getServicesManager().getRegistration(ShardsService.class);
            if (rsp != null) {
                shardsService = rsp.getProvider();
            } else {
                try {
                    shardsService = org.khmc.shards.ShardsPlugin.getInstance().getShardsService();
                } catch (Throwable ignored) {}
            }
        }
        checked = true;
        return shardsService;
    }

    public static boolean isAvailable() {
        return getService() != null;
    }

    public static double getBalance(Player player) {
        if (player == null) return 0.0;
        ShardsService service = getService();
        if (service != null) {
            try {
                return service.getBalance(player, "shards");
            } catch (Throwable ignored) {}
        }
        return 0.0;
    }

    public static boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public static boolean withdraw(Player player, double amount) {
        if (player == null || amount <= 0) return true;
        ShardsService service = getService();
        if (service != null) {
            try {
                return service.withdraw(player, "shards", amount);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static String format(double amount) {
        ShardsService service = getService();
        if (service != null) {
            try {
                return service.format("shards", amount);
            } catch (Throwable ignored) {}
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        return "&d★ " + nf.format(Math.round(amount));
    }

    public static String getSymbol() {
        ShardsService service = getService();
        if (service != null) {
            try {
                return service.getSymbol("shards");
            } catch (Throwable ignored) {}
        }
        return "★";
    }
}
