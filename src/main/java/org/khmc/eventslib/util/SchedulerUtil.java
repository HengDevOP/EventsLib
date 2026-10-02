package org.khmc.eventslib.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public final class SchedulerUtil {

    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public interface WrappedTask {
        void cancel();
        boolean isCancelled();
    }

    private static WrappedTask wrap(ScheduledTask task) {
        return new WrappedTask() {
            @Override public void cancel()      { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }

    private static WrappedTask wrap(BukkitTask task) {
        return new WrappedTask() {
            @Override public void cancel()      { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }

    public static void runTask(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA && entity != null) {
            entity.getScheduler().run(plugin, t -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTask(Plugin plugin, Location location, Runnable runnable) {
        if (IS_FOLIA && location != null) {
            Bukkit.getRegionScheduler().run(plugin, location, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (IS_FOLIA && entity != null) {
            entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static WrappedTask runTaskTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            long delayMs  = Math.max(1L, delayTicks  * 50L);
            long periodMs = Math.max(1L, periodTicks * 50L);
            ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
            return wrap(task);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            return wrap(task);
        }
    }
}
