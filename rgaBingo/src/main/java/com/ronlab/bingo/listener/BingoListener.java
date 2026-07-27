package com.ronlab.bingo.listener;

import com.ronlab.bingo.BingoPlugin;
import com.ronlab.bingo.gui.BingoCardGUI;
import com.ronlab.bingo.model.BingoCard;
import com.ronlab.bingo.model.BingoSession;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BingoListener implements Listener {

    private final BingoPlugin plugin;

    public BingoListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Performance guard: Early short-circuit if player is not in an active session
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        Material pickedUp = event.getItem().getItemStack().getType();
        session.handleItemAcquisition(player, pickedUp);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Performance guard: Early short-circuit if player is not in an active session
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        session.handleItemAcquisition(player, result.getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if viewing the Bingo Card GUI
        if (event.getView().getTitle().contains("BINGO CARD")) {
            event.setCancelled(true);
            return;
        }

        // Performance guard: Early short-circuit if player is not in an active session
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() != Material.AIR) {
            session.handleItemAcquisition(player, currentItem.getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        // Freeze XYZ positioning during COUNTDOWN state
        if (session.isFrozen(player)) {
            if (event.hasChangedBlock()) {
                event.setTo(session.getFrozenLocation(player));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && isBingoCardItem(item)) {
            BingoCard card = session.getCard(player);
            if (card != null) {
                BingoCardGUI.openGUI(player, card);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getScoreboardManager().removeBoard(player);
    }

    private boolean isBingoCardItem(ItemStack item) {
        if (item.getType() == Material.PAPER || item.getType() == Material.FILLED_MAP || item.getType() == Material.MAP) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String plainName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                return plainName.toLowerCase().contains("bingo card");
            }
        }
        return false;
    }
}
