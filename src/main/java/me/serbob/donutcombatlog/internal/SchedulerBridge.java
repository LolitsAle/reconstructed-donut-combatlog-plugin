package me.serbob.donutcombatlog.internal;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Folia-safe scheduler bridge targeting Paper/Folia 1.21.11 APIs.
 */
public final class SchedulerBridge {
    private final Plugin plugin;

    public SchedulerBridge(Plugin plugin) {
        this.plugin = plugin;
    }

    public void runEntityNextTick(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    public void runEntityLater(Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
    }

    public ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, Runnable retired, long delayTicks, long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, task, retired, delayTicks, periodTicks);
    }

    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    public void runGlobalLater(Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }
}
