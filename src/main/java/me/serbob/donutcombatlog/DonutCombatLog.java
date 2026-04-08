package me.serbob.donutcombatlog;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.serbob.donutcombatlog.internal.DonutDatabaseAdapter;
import me.serbob.donutcombatlog.internal.SchedulerBridge;
import me.serbob.donutcombatlog.utils.ChatUtil;
import me.serbob.donutcombatlog.utils.Format;
import me.serbob.donutcombatlog.utils.protect.Allow;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DonutCombatLog extends JavaPlugin implements Listener {
    private final Map<UUID, Long> combatExpiryMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> shardExpiryMillis = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> combatTasks = new ConcurrentHashMap<>();

    private int shardDelay;
    private int defaultTime;
    private List<String> disallowedCommands;
    private boolean elytraAllowed;
    private boolean enderpearlAllowed;
    private boolean guiAllowed;
    private boolean flyingAllowed;

    private SchedulerBridge scheduler;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("DonutDatabase") == null || !DonutDatabaseAdapter.isAvailable()) {
            getLogger().severe("DonutDatabase not found! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.scheduler = new SchedulerBridge(this);

        saveDefaultConfig();

        defaultTime = getConfig().getInt("default_time");
        shardDelay = getConfig().getInt("shard_delay");
        disallowedCommands = getConfig().getStringList("commands_not_allowed");
        elytraAllowed = getConfig().getBoolean("elytra");
        enderpearlAllowed = getConfig().getBoolean("enderpearl");
        guiAllowed = getConfig().getBoolean("gui");
        flyingAllowed = getConfig().getBoolean("flying");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (ScheduledTask task : combatTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        combatTasks.clear();
        combatExpiryMillis.clear();
        shardExpiryMillis.clear();
        lastAttacker.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if ("ENDER_PEARL".equals(event.getDamager().getType().toString())) {
            return;
        }

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            handlePlayerVsPlayerHit(attacker, victim);
            return;
        }

        if (event.getDamager() instanceof Projectile projectile && event.getEntity() instanceof Player victim) {
            if (projectile.getShooter() instanceof Player attacker) {
                handlePlayerVsPlayerHit(attacker, victim);
            }
        }
    }

    private void handlePlayerVsPlayerHit(Player attacker, Player victim) {
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (Allow.isLocationProtected(attacker, getConfig().getStringList("combat_not_allowed_worlds"))) {
            return;
        }

        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());

        scheduler.runEntityLater(victim, () -> handleDamageResolution(victim, attacker.getUniqueId(), attacker.getName()), 1L);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        if ("ENDER_PEARL".equals(event.getEntity().getType().toString()) && !enderpearlAllowed && isInCombat(player)) {
            event.setCancelled(true);
            deny(player, getConfig().getString("messages.enderpearl_denied", "&cYou cannot use enderpearls in combat."));
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!flyingAllowed && isInCombat(player)) {
            event.setCancelled(true);
            if (player.isFlying()) {
                player.setFlying(false);
            }
            deny(player, getConfig().getString("messages.flying_denied", "&cYou cannot fly in combat."));
        }

        if (!elytraAllowed && isInCombat(player) && player.isGliding()) {
            event.setCancelled(true);
            player.setGliding(false);
            deny(player, getConfig().getString("messages.elytra_denied", "&cYou cannot use elytra in combat."));
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!guiAllowed && isInCombat(player) && event.getInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            deny(player, getConfig().getString("messages.gui_denied", "&cYou cannot open GUIs in combat."));
        }
    }

    @EventHandler
    public void die(PlayerDeathEvent event) {
        clearCombatState(event.getEntity().getUniqueId(), true);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].toLowerCase().substring(1);

        if (isInCombat(player) && disallowedCommands.contains(command)) {
            event.setCancelled(true);
            deny(player, getConfig().getString("messages.command_denied", "&cYou cannot do this in combat."));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isInCombat(player)) {
            handleCombatLog(player);
        }
    }

    private void handleDamageResolution(Player victim, UUID attackerUuid, String attackerName) {
        if (victim.getHealth() <= 0.0D) {
            clearCombatState(victim.getUniqueId(), true);
            clearCombatState(attackerUuid, false);

            String killMessage = getConfig().getString("kill_message")
                    .replace("{victim}", victim.getName())
                    .replace("{attacker}", attackerName);
            scheduler.runGlobal(() -> Bukkit.broadcastMessage(ChatUtil.c(killMessage)));

            Player attacker = Bukkit.getPlayer(attackerUuid);
            if (attacker != null && attacker.isOnline()) {
                scheduleHandleKill(attacker, victim.getUniqueId(), victim.getName());
            }
            return;
        }

        setCombatLogTime(victim, defaultTime);

        Player attacker = Bukkit.getPlayer(attackerUuid);
        if (attacker != null && attacker.isOnline()) {
            scheduler.runEntityNextTick(attacker, () -> setCombatLogTime(attacker, defaultTime));
        }
    }

    private void scheduleHandleKill(Player killer, UUID victimUuid, String victimName) {
        scheduler.runEntityNextTick(killer, () -> handleKill(killer, victimUuid, victimName));
    }

    private void handleKill(Player killer, UUID victimUuid, String victimName) {
        double bounty = DonutDatabaseAdapter.getBounty(victimName);
        boolean hasBounty = bounty > 0.0D;
        boolean ableToGetShards = !isOnShardCooldown(killer.getUniqueId(), victimUuid);

        if (ableToGetShards && hasBounty) {
            executeCommands("shards_only.commands", killer.getName(), victimName);

            String command = getConfig().getString("execute.bounty_only.command")
                    .replace("{attacker}", killer.getName())
                    .replace("{bounty}", String.valueOf(bounty));
            scheduler.runGlobalLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), 1L);

            String message = getConfig().getString("execute.shards_and_bounty.message")
                    .replace("{victim}", victimName)
                    .replace("{bounty}", Format.formatPrice(bounty));
            killer.sendMessage(ChatUtil.c(message));
        } else if (ableToGetShards) {
            executeCommands("shards_only.commands", killer.getName(), victimName);

            String message = getConfig().getString("execute.shards_only.message")
                    .replace("{victim}", victimName);
            killer.sendMessage(ChatUtil.c(message));
        } else if (hasBounty) {
            String message = getConfig().getString("execute.bounty_only.message")
                    .replace("{victim}", victimName)
                    .replace("{bounty}", Format.formatPrice(bounty));
            killer.sendMessage(ChatUtil.c(message));

            String command = getConfig().getString("execute.bounty_only.command")
                    .replace("{attacker}", killer.getName())
                    .replace("{bounty}", String.valueOf(bounty));
            scheduler.runGlobalLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), 1L);
        }

        if (ableToGetShards) {
            setShardCooldown(killer.getUniqueId(), victimUuid, shardDelay);
        }

        if (hasBounty) {
            DonutDatabaseAdapter.removeBounty(victimName);
        }
    }

    private void executeCommands(String path, String attackerName, String victimName) {
        List<String> attackerCommands = getConfig().getStringList("execute." + path + ".attacker");
        List<String> victimCommands = getConfig().getStringList("execute." + path + ".victim");

        scheduler.runGlobalLater(() -> {
            for (String command : attackerCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{attacker}", attackerName));
            }

            for (String command : victimCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{victim}", victimName));
            }
        }, 1L);
    }

    private void handleCombatLog(Player player) {
        UUID victimUuid = player.getUniqueId();
        UUID killerUuid = lastAttacker.get(victimUuid);
        Player killer = killerUuid != null ? Bukkit.getPlayer(killerUuid) : null;
        String victimName = player.getName();
        String killerName = killer != null ? killer.getName() : "unknown";

        if (killer != null && killer.isOnline()) {
            String combatLogMessage = getConfig().getString("kill_message")
                    .replace("{victim}", victimName)
                    .replace("{attacker}", killerName);

            scheduler.runEntityNextTick(killer, () -> {
                killer.sendMessage(ChatUtil.c(combatLogMessage));
                handleKill(killer, victimUuid, victimName);
            });
        }

        player.getInventory().forEach(item -> dropInventoryItem(player, item));
        player.getInventory().clear();
        player.setHealth(0.0D);

        clearCombatState(victimUuid, true);
    }

    private static void dropInventoryItem(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private boolean isInCombat(Player player) {
        return combatExpiryMillis.containsKey(player.getUniqueId());
    }

    private void setCombatLogTime(Player player, int timeSeconds) {
        UUID playerId = player.getUniqueId();
        combatExpiryMillis.put(playerId, System.currentTimeMillis() + (timeSeconds * 1000L));
        ensureCombatTicker(player);
        enforceFlightRestriction(player);
    }

    private void ensureCombatTicker(Player player) {
        UUID playerId = player.getUniqueId();
        if (combatTasks.containsKey(playerId)) {
            return;
        }

        ScheduledTask task = scheduler.runEntityTimer(player, scheduledTask -> {
            Long expiryMillis = combatExpiryMillis.get(playerId);
            if (expiryMillis == null) {
                cleanupCombatTask(playerId, scheduledTask);
                return;
            }

            long remaining = Math.max(0L, (expiryMillis - System.currentTimeMillis() + 999L) / 1000L);
            if (remaining <= 0L) {
                clearCombatState(playerId, true);
                cleanupCombatTask(playerId, scheduledTask);
                return;
            }

            String actionBarMessage = getConfig().getString("messages.combat_timer", "&7Combat: #37BFF9{time}")
                    .replace("{time}", String.valueOf(remaining));
            sendActionBar(player, actionBarMessage);
            enforceFlightRestriction(player);
        }, () -> clearCombatState(playerId, true), 20L, 20L);

        if (task != null) {
            ScheduledTask existing = combatTasks.putIfAbsent(playerId, task);
            if (existing != null) {
                task.cancel();
            }
        }
    }

    private void enforceFlightRestriction(Player player) {
        if (!flyingAllowed && isInCombat(player) && player.isFlying()) {
            player.setFlying(false);
            deny(player, getConfig().getString("messages.flying_denied", "&cYou cannot fly in combat."));
        }
    }

    private void cleanupCombatTask(UUID playerId, ScheduledTask task) {
        combatTasks.remove(playerId, task);
        task.cancel();
    }

    private void clearCombatState(UUID playerId, boolean clearLastAttacker) {
        combatExpiryMillis.remove(playerId);
        if (clearLastAttacker) {
            lastAttacker.remove(playerId);
        }

        ScheduledTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean isOnShardCooldown(UUID attackerUUID, UUID victimUUID) {
        Map<UUID, Long> victimMap = shardExpiryMillis.get(attackerUUID);
        if (victimMap == null) {
            return false;
        }

        Long expiryMillis = victimMap.get(victimUUID);
        if (expiryMillis == null) {
            return false;
        }

        if (expiryMillis <= System.currentTimeMillis()) {
            victimMap.remove(victimUUID);
            if (victimMap.isEmpty()) {
                shardExpiryMillis.remove(attackerUUID, victimMap);
            }
            return false;
        }

        return true;
    }

    private void setShardCooldown(UUID attackerUUID, UUID victimUUID, int cooldownSeconds) {
        shardExpiryMillis
                .computeIfAbsent(attackerUUID, ignored -> new ConcurrentHashMap<>())
                .put(victimUUID, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatUtil.c(message)));
    }

    private void deny(Player player, String message) {
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatUtil.c(message)));
        player.sendMessage(ChatUtil.c(message));
    }
}
