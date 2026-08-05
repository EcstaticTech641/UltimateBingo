package com.ronlab.bingo.model;

import com.ronlab.bingo.gui.BingoCardGUI;
import com.ronlab.bingo.hud.BingoScoreboardManager;
import com.ronlab.bingo.util.MaterialPoolManager;
import com.ronlab.rga.api.RGASessionControl;
import com.ronlab.rga.api.event.RGAGameRequestConcludeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BingoSession {

    private final String worldName;
    private final List<UUID> playerUuids;
    private final RGASessionControl rgaSessionControl;
    private final Plugin plugin;
    private final BingoScoreboardManager scoreboardManager;

    private final Map<UUID, BingoCard> playerCards = new ConcurrentHashMap<>();
    private final Map<UUID, Location> frozenLocations = new ConcurrentHashMap<>();

    private BingoSessionState state = BingoSessionState.COUNTDOWN;
    private long elapsedSeconds = 0;
    private BukkitTask timerTask;
    private BukkitTask countdownTask;

    public BingoSession(String worldName, List<UUID> playerUuids, RGASessionControl rgaSessionControl, Plugin plugin, BingoScoreboardManager scoreboardManager) {
        this.worldName = worldName;
        this.playerUuids = List.copyOf(playerUuids);
        this.rgaSessionControl = rgaSessionControl;
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    public void initialize() {
        for (UUID uuid : playerUuids) {
            Player player = Bukkit.getPlayer(uuid);
            List<Material> materials = MaterialPoolManager.getRandomMaterials(25);
            BingoCard card = new BingoCard(5, materials);
            playerCards.put(uuid, card);

            if (player != null && player.isOnline()) {
                frozenLocations.put(uuid, player.getLocation());
                giveHotbarCardItem(player);
                scoreboardManager.createBoard(player, this);
            }
        }

        // Silently credit items already in each player's inventory at game start.
        // Scheduled 1 tick after initialize() so all card state is fully committed.
        Bukkit.getScheduler().runTask(plugin, this::snapshotPreGameInventories);

        startCountdown();
    }

    /**
     * Silently marks card items already present in each player's inventory at session start.
     * No chime or actionbar feedback — these are pre-game possessions, not in-game finds.
     */
    private void snapshotPreGameInventories() {
        for (UUID uuid : playerUuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            BingoCard card = playerCards.get(uuid);
            if (card == null) continue;

            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null || stack.getType() == Material.AIR) continue;
                Material mat = stack.getType();
                if (card.isOnCard(mat) && !card.isAlreadyCompleted(mat)) {
                    card.markItemCompleted(mat, player);
                    plugin.getLogger().fine("[rgaBingo] Pre-game inventory credit: " + mat.name() + " for " + player.getName());
                }
            }

            // Refresh HUD to reflect silently credited items
            scoreboardManager.updateBoard(player, this);
        }
    }

    public void giveHotbarCardItem(Player player) {
        ItemStack cardItem = new ItemStack(Material.PAPER);
        ItemMeta meta = cardItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Bingo Card", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Right-click to open your Bingo Card", NamedTextColor.YELLOW));
            meta.lore(lore);
            cardItem.setItemMeta(meta);
        }
        player.getInventory().setItem(8, cardItem);
    }

    private void startCountdown() {
        state = BingoSessionState.COUNTDOWN;

        countdownTask = new BukkitRunnable() {
            int secondsLeft = 3;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    for (UUID uuid : playerUuids) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            Title title = Title.title(
                                    Component.text(String.valueOf(secondsLeft), getCountdownColor(secondsLeft), TextDecoration.BOLD),
                                    Component.text("Get ready!", NamedTextColor.GRAY),
                                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(100))
                            );
                            player.showTitle(title);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, getPitch(secondsLeft));
                        }
                    }
                    secondsLeft--;
                } else {
                    state = BingoSessionState.IN_GAME;
                    frozenLocations.clear();

                    for (UUID uuid : playerUuids) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            Title title = Title.title(
                                    Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD),
                                    Component.text("Collect items to complete a Bingo line!", NamedTextColor.YELLOW),
                                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(200))
                            );
                            player.showTitle(title);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        }
                    }

                    startTimer();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private NamedTextColor getCountdownColor(int seconds) {
        return switch (seconds) {
            case 3 -> NamedTextColor.RED;
            case 2 -> NamedTextColor.GOLD;
            case 1 -> NamedTextColor.YELLOW;
            default -> NamedTextColor.GREEN;
        };
    }

    private float getPitch(int seconds) {
        return switch (seconds) {
            case 3 -> 0.5f;
            case 2 -> 0.7f;
            case 1 -> 1.0f;
            default -> 1.2f;
        };
    }

    private void startTimer() {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == BingoSessionState.IN_GAME) {
                    elapsedSeconds++;
                    scoreboardManager.updateAll(BingoSession.this);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Unified acquisition entry point for all item delivery pathways
     * (pickup, craft, inventory click/drag, container transfer scan).
     *
     * <p>Guards (in order):
     * <ol>
     *   <li>Session must be IN_GAME state.</li>
     *   <li>Material must be on the player's card (fast {@code materialMap} lookup).</li>
     *   <li>Slot must not already be completed (prevents re-pickup chime desync).</li>
     * </ol>
     *
     * @return true if the slot was newly completed during this call.
     */
    public boolean checkAndCompleteMaterial(Player player, Material material) {
        if (state != BingoSessionState.IN_GAME) {
            return false;
        }

        BingoCard card = playerCards.get(player.getUniqueId());
        if (card == null) {
            return false;
        }

        // Guard 1: material not on card — fast exit, no slot work
        if (!card.isOnCard(material)) {
            return false;
        }

        // Guard 2: already completed — prevents re-pickup chime / double state transition
        if (card.isAlreadyCompleted(material)) {
            return false;
        }

        boolean updated = card.markItemCompleted(material, player);
        if (updated) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            String formattedName = formatMaterialName(material);
            player.sendActionBar(Component.text("✔ Found: " + formattedName + "!", NamedTextColor.GREEN, TextDecoration.BOLD));

            scoreboardManager.updateAll(this);

            if (player.getOpenInventory().getTitle().startsWith(BingoCardGUI.GUI_TITLE_PREFIX)) {
                BingoCardGUI.openGUI(player, card);
            }

            if (card.hasBingoLine()) {
                handleWin(player);
            }

            return true;
        }

        return false;
    }

    /**
     * Scans a player's current inventory for any card items not yet completed.
     * Called after inventory-state-changing events (click, drag, container pull)
     * where the settled inventory contents are the authoritative source of truth.
     */
    public void scanInventoryForCompletions(Player player) {
        if (state != BingoSessionState.IN_GAME) return;

        BingoCard card = playerCards.get(player.getUniqueId());
        if (card == null) return;

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            checkAndCompleteMaterial(player, stack.getType());
        }
    }

    /**
     * Returns true if the given player UUID is a registered participant in this session.
     */
    public boolean isParticipant(UUID uuid) {
        return playerUuids.contains(uuid);
    }

    private void handleWin(Player winner) {
        state = BingoSessionState.CONCLUDED;

        Component victoryMsg = Component.text("BINGO! ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(winner.getName() + " completed a Bingo line in " + formatTime(elapsedSeconds) + "!", NamedTextColor.GREEN));

        for (UUID uuid : playerUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(victoryMsg);
                Title title = Title.title(
                        Component.text(winner.getName() + " WINS!", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text("Completed a Bingo Line!", NamedTextColor.GREEN),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(500))
                );
                p.showTitle(title);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        Map<UUID, Number> scores = getFinalScores();

        if (rgaSessionControl != null) {
            rgaSessionControl.setSpectator(winner, true);
            rgaSessionControl.requestSessionConclude(worldName, winner.getName() + " completed a Bingo line!", scores);
        }

        RGAGameRequestConcludeEvent concludeEvent = new RGAGameRequestConcludeEvent("bingo", "Bingo", worldName, playerUuids, winner.getName() + " WINS", scores);
        Bukkit.getPluginManager().callEvent(concludeEvent);

        conclude();
    }

    public void conclude() {
        state = BingoSessionState.CONCLUDED;

        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
        }
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        // Bulk remove all FastBoard HUDs. Using removeAll() as a teardown backstop ensures
        // boards are destroyed for participants who disconnected mid-session, preventing lingering
        // scoreboards when they reconnect to the lobby world.
        Bukkit.getScheduler().runTask(plugin, scoreboardManager::removeAll);

        playerCards.clear();
        frozenLocations.clear();
    }

    public Map<UUID, Number> getFinalScores() {
        Map<UUID, Number> scores = new HashMap<>();
        for (Map.Entry<UUID, BingoCard> entry : playerCards.entrySet()) {
            BingoCard card = entry.getValue();
            scores.put(entry.getKey(), card.getCompletedCount());
        }
        return scores;
    }

    public boolean isFrozen(Player player) {
        return state == BingoSessionState.COUNTDOWN && frozenLocations.containsKey(player.getUniqueId());
    }

    public Location getFrozenLocation(Player player) {
        return frozenLocations.get(player.getUniqueId());
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
        }
        return players;
    }

    public List<UUID> getPlayerUuids() {
        return playerUuids;
    }

    public BingoSessionState getState() {
        return state;
    }

    public BingoCard getCard(Player player) {
        return playerCards.get(player.getUniqueId());
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
