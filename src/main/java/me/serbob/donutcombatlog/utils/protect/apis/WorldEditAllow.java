package me.serbob.donutcombatlog.utils.protect.apis;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reconstructed from bytecode. The original jar bundled this helper but the main
 * plugin code does not appear to call it. Reflection keeps this repo buildable
 * without compile-time WorldEdit / WorldGuard dependencies.
 */
public class WorldEditAllow {
    public static boolean isPluginEnabled() {
        return Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static boolean isLocationProtected(Player player, Location defaultLocation) {
        try {
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> worldGuardPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Class<?> worldGuardPlatformClass = Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform");
            Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");

            Object location = bukkitAdapterClass.getMethod("adapt", Location.class).invoke(null, defaultLocation);
            Object worldGuardPlugin = worldGuardPluginClass.getMethod("inst").invoke(null);
            Object localPlayer = worldGuardPluginClass.getMethod("wrapPlayer", Player.class).invoke(worldGuardPlugin, player);
            Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            Object container = worldGuardPlatformClass.getMethod("getRegionContainer").invoke(platform);
            Object query = regionContainerClass.getMethod("createQuery").invoke(container);

            Field pvpField = flagsClass.getField("PVP");
            Object pvpFlag = pvpField.get(null);
            Object flagsArray = Array.newInstance(stateFlagClass, 1);
            Array.set(flagsArray, 0, pvpFlag);

            Method testState = regionQueryClass.getMethod("testState", location.getClass(), localPlayer.getClass(), flagsArray.getClass());
            boolean result = (boolean) testState.invoke(query, location, localPlayer, flagsArray);
            return !result;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
