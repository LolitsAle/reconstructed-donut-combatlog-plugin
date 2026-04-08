package me.serbob.donutcombatlog.utils.protect;

import org.bukkit.entity.Player;

import java.util.List;

public class Allow {
    public static boolean isLocationProtected(Player player, List<String> blacklistedWorlds) {
        if (blacklistedWorlds.contains(player.getWorld().getName())) {
            return true;
        }
        return false;
    }
}
