package com.ronlab.bingo.listener;

import com.ronlab.bingo.BingoPlugin;
import com.ronlab.bingo.gui.BingoCardGUI;
import com.ronlab.bingo.model.BingoCard;
import com.ronlab.bingo.model.BingoSession;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BingoEventListener implements Listener {

    private static final String TARGET_MINIGAME_ID = "bingo";
    private final BingoPlugin plugin;

    public BingoEventListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isTargetMinigame(String incomingId) {
        if (incomingId == null) {
            return false;
        }
        String normalizedIncoming = incomingId.toLowerCase(Locale.ROOT).trim().replace("_", "").replace("-", "");
        String normalizedConfigured = TARGET_MINIGAME_ID.toLowerCase(Locale.ROOT).trim().replace("_", "").replace("-", "");
        return normalizedIncoming.equals(normalizedConfigured) || normalizedIncoming.equals("ultimatebingo");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMinigameStart(MinigameStartEvent event) {
        if (!isTargetMinigame(event.getMinigameId())) {
            plugin.getLogger().fine("[rgaBingo DEBUG] Ignoring event for minigame '" + event.getMinigameId() + "'");
            return;
        }

        plugin.getLogger().info("[rgaBingo DEBUG] Received MinigameStartEvent!");
        plugin.getLogger().info("[rgaBingo DEBUG] -> Received ID: '" + event.getMinigameId() + "'");
        plugin.getLogger().info("[rgaBingo DEBUG] -> World: " + event.getWorldName());
        plugin.getLogger().info("[rgaBingo DEBUG] -> Players: " + (event.getPlayerUuids() != null ? event.getPlayerUuids().size() : 0));

        List<UUID> playerUuids = event.getPlayerUuids();
        if (playerUuids == null || playerUuids.isEmpty()) {
            plugin.getLogger().severe("[rgaBingo DEBUG] Player list is empty! Cannot start session for world '" + event.getWorldName() + "'.");
            event.setCancelled(true);
            return;
        }

        plugin.getLogger().info("[rgaBingo] Handshake successful. Initializing Bingo session for world: " + event.getWorldName());

        BingoSession session = new BingoSession(
                event.getWorldName(),
                playerUuids,
                plugin.getRgaSessionControl(),
                plugin,
                plugin.getScoreboardManager()
        );

        plugin.registerSession(event.getWorldName(), session);
        session.initialize();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMinigameConclude(MinigameConcludeEvent event) {
        if (!isTargetMinigame(event.getMinigameId())) {
            return;
        }

        plugin.getLogger().info("[rgaBingo DEBUG] Received MinigameConcludeEvent for world: " + event.getWorldName());

        BingoSession session = plugin.getSession(event.getWorldName());
        if (session != null) {
            Map<UUID, Number> finalScores = session.getFinalScores();
            event.getScores().putAll(finalScores);

            plugin.unregisterSession(event.getWorldName());

            // Explicit FastBoard teardown backstop on conclude.
            // conclude() already schedules removeAll() but calling it here covers the case where
            // MinigameConcludeEvent is dispatched externally (e.g. admin /rga conclude) without
            // going through handleWin(), which means conclude() may not have been called yet.
            plugin.getScoreboardManager().removeAll();

            plugin.getLogger().info("[rgaBingo] Successfully concluded and unregistered session for world: " + event.getWorldName());
        }
    }

    // -------------------------------------------------------------------------
    // Item Acquisition — Unified pathways all route through checkAndCompleteMaterial
    // -------------------------------------------------------------------------

    /**
     * Primary acquisition pathway: physical item pickup from the ground.
     * checkAndCompleteMaterial guards against re-pickup chimes via isAlreadyCompleted().
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        Material pickedUp = event.getItem().getItemStack().getType();
        session.checkAndCompleteMaterial(player, pickedUp);
    }

    /**
     * Crafting acquisition pathway.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        session.checkAndCompleteMaterial(player, result.getType());
    }

    /**
     * Inventory click pathway — covers container pulls (chest, furnace, barrel extractions)
     * and offhand swaps. Uses a 1-tick deferred inventory scan so the inventory state has
     * fully settled before evaluation, rather than relying on the cursor item snapshot
     * which may be mid-transaction.
     *
     * <p>Also intercepts clicks on the BINGO CARD GUI and cancels them to prevent item theft.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Block item movement inside the Bingo Card GUI
        if (event.getView().getTitle().contains("BINGO CARD")) {
            event.setCancelled(true);
            return;
        }

        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        // Defer scan by 1 tick: inventory state is not yet settled at event-fire time
        // (the click transaction is committed after this handler returns).
        Bukkit.getScheduler().runTask(plugin, () -> session.scanInventoryForCompletions(player));
    }

    /**
     * Inventory drag pathway — covers split-stack drag operations into player inventory.
     * Uses the same deferred 1-tick scan pattern as InventoryClickEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> session.scanInventoryForCompletions(player));
    }

    // -------------------------------------------------------------------------
    // Movement / Interaction / Lifecycle
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BingoSession session = plugin.getSessionForPlayer(player);
        if (session == null) {
            return;
        }

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
                return plainName.toLowerCase(Locale.ROOT).contains("bingo card");
            }
        }
        return false;
    }
}
