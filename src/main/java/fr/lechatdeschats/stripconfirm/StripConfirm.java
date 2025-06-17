package fr.lechatdeschats.stripconfirm;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StripConfirm extends JavaPlugin implements Listener {

    private final Map<UUID, PendingStrip> pending = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("StripConfirm activé.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || !isStrippable(block)) return;
        if (!isAxe(event.getItem())) return;

        UUID uuid = player.getUniqueId();
        Location loc = block.getLocation();
        Material type = block.getType();

        if (pending.containsKey(uuid)) {
            PendingStrip previous = pending.get(uuid);
            if (isSameBlock(previous.location, loc) && block.getType() == previous.originalType) {
                block.setType(getStrippedVariant(block.getType()));
                player.sendMessage("\u2705 Bois écorcé !");
                pending.remove(uuid);
            } else {
                startPending(player, loc, type);
            }
        } else {
            startPending(player, loc, type);
        }

        event.setCancelled(true);
    }

    private void startPending(Player player, Location loc, Material type) {
        UUID uuid = player.getUniqueId();
        player.sendMessage("\uD83D\uDD01 Clique à nouveau sur le même bloc pour écorcer (5s)...");
        pending.put(uuid, new PendingStrip(loc, type));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            PendingStrip ps = pending.get(uuid);
            if (ps != null && isSameBlock(ps.location, loc)) {
                pending.remove(uuid);
                player.sendMessage("\u23F1\uFE0F Délai dépassé. Réessaie.");
            }
        }, 20 * 5);
    }

    private boolean isAxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }

    private boolean isStrippable(Block block) {
        String name = block.getType().name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD");
    }

    private Material getStrippedVariant(Material original) {
        String name = original.name();
        if (name.endsWith("_LOG") || name.endsWith("_WOOD")) {
            return Material.valueOf("STRIPPED_" + name);
        }
        return original;
    }

    private boolean isSameBlock(Location a, Location b) {
        return a.getWorld().equals(b.getWorld()) &&
                a.getBlockX() == b.getBlockX() &&
                a.getBlockY() == b.getBlockY() &&
                a.getBlockZ() == b.getBlockZ();
    }

    private static class PendingStrip {
        final Location location;
        final Material originalType;

        PendingStrip(Location loc, Material type) {
            this.location = loc;
            this.originalType = type;
        }
    }
}
