package dev.tntfreeze;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TNTFreeze extends JavaPlugin implements Listener, CommandExecutor {
    private final Set<String> marked = new HashSet<>();
    private final Map<UUID, Frozen> frozen = new HashMap<>();
    private record Frozen(UUID worldId, Location location, Vector velocity, int fuse, boolean gravity) {}

    private String chunkKey(Chunk chunk) {
        return chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        marked.addAll(getConfig().getStringList("marked-chunks"));
        Bukkit.getPluginManager().registerEvents(this, this);
        var command = getCommand("tntfreeze");
        if (command != null) command.setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
    }

    private void persist() {
        FileConfiguration cfg = getConfig();
        cfg.set("marked-chunks", marked.stream().sorted().toList());
        saveConfig();
    }

    private void restore(TNTPrimed tnt, Frozen state) {
        tnt.setGravity(state.gravity());
        tnt.setFuseTicks(state.fuse());
        tnt.setVelocity(state.velocity().clone());
    }

    private void tick() {
        Set<UUID> seen = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (TNTPrimed tnt : world.getEntitiesByClass(TNTPrimed.class)) {
                UUID id = tnt.getUniqueId();
                seen.add(id);
                Frozen state = frozen.get(id);
                boolean inMarkedChunk = marked.contains(chunkKey(tnt.getLocation().getChunk()));
                if (state == null && inMarkedChunk) {
                    state = new Frozen(world.getUID(), tnt.getLocation().clone(), tnt.getVelocity().clone(),
                            tnt.getFuseTicks(), tnt.hasGravity());
                    frozen.put(id, state);
                }
                if (state != null) {
                    // Keep TNT at the saved position even when the chunk was unmarked.
                    if (!marked.contains(chunkKey(state.location().getChunk()))) {
                        frozen.remove(id);
                        restore(tnt, state);
                        continue;
                    }
                    tnt.teleport(state.location());
                    tnt.setGravity(false);
                    tnt.setVelocity(new Vector(0, 0, 0));
                    tnt.setFuseTicks(Math.max(2, state.fuse()));
                }
            }
        }
        frozen.keySet().removeIf(id -> !seen.contains(id));
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt &&
                (frozen.containsKey(tnt.getUniqueId()) || marked.contains(chunkKey(tnt.getLocation().getChunk())))) {
            event.setCancelled(true);
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("tntfreeze.admin")) {
            sender.sendMessage("You don't have permission to use TNTFreeze.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("TNTFreeze marked chunks (world UUID:x:z): " + (marked.isEmpty() ? "none" : String.join(", ", marked)));
            return true;
        }
        if (args.length != 1 || !(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            sender.sendMessage("Usage: /tntfreeze add|remove|list");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run add/remove in-game while standing in the chunk.");
            return true;
        }
        String key = chunkKey(player.getLocation().getChunk());
        if (args[0].equalsIgnoreCase("add")) {
            marked.add(key);
            sender.sendMessage("TNT freezing enabled in your current chunk.");
        } else {
            marked.remove(key);
            sender.sendMessage("TNT freezing disabled in your current chunk. Frozen TNT resumes next tick.");
        }
        persist();
        return true;
    }

    @Override public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            for (TNTPrimed tnt : world.getEntitiesByClass(TNTPrimed.class)) {
                Frozen state = frozen.get(tnt.getUniqueId());
                if (state != null) restore(tnt, state);
            }
        }
        frozen.clear();
    }
}
