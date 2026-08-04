package com.ronlab.bingo.model;

import com.ronlab.bingo.gui.BingoCardGUI;
import com.ronlab.bingo.hud.BingoScoreboardManager;
import com.ronlab.bingo.util.MaterialPoolManager;
import com.ronlab.rga.api.RgaControl;
import com.ronlab.rga.api.event.GameSessionRequestConcludeEvent;
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

    private final String sessionId;
    private final World world;
    private final List<Player> players;
    private final RgaControl rgaControl;
    private final Plugin plugin;
    private final BingoScoreboardManager scoreboardManager;

    private final Map<UUID, BingoCard> playerCards = new ConcurrentHashMap<>();
    private final Map<UUID, Location> frozenLocations = new ConcurrentHashMap<>();

    private BingoSessionState state = BingoSessionState.COUNTDOWN;
    private long elapsedSeconds = 0;
    private BukkitTask timerTask;
    private BukkitTask countdownTask;

    public BingoSession(String sessionId, World world, List<Player> players, RgaControl rgaControl, Plugin plugin, BingoScoreboardManager scoreboardManager) {
        this.sessionId = sessionId;
        this.world = world;
        this.players = new ArrayList<>(players);
        this.rgaControl = rgaControl;
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    public void initialize() {
        // Generate randomized 5x5 Bingo cards per player
        for (Player player : players) {
            List<Material> materials = MaterialPoolManager.getRandomMaterials(25);
            BingoCard card = new BingoCard(5, materials);
            playerCards.put(player.getUniqueId(), card);

            // Store frozen location for countdown
            frozenLocations.put(player.getUniqueId(), player.getLocation());

            // Give Bingo Card item in hotbar slot 8
            giveHotbarCardItem(player);

            // Create HUD board
            scoreboardManager.createBoard(player, this);
        }

        startCountdown();
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
                    for (Player player : players) {
                        if (player.isOnline()) {
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

                    for (Player player : players) {
                        if (player.isOnline()) {
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

    public boolean handleItemAcquisition(Player player, Material material) {
        if (state != BingoSessionState.IN_GAME) {
            return false;
        }

        BingoCard card = playerCards.get(player.getUniqueId());
        if (card == null) {
            return false;
        }

        boolean updated = card.markItemCompleted(material, player);
        if (updated) {
            // Audio cue
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // Actionbar message
            String formattedName = formatMaterialName(material);
            player.sendActionBar(Component.text("✔ Found: " + formattedName + "!", NamedTextColor.GREEN, TextDecoration.BOLD));

            // Dynamic HUD update
            scoreboardManager.updateAll(this);

            // If player currently has card GUI open, refresh it
            if (player.getOpenInventory().getTitle().startsWith(BingoCardGUI.GUI_TITLE_PREFIX)) {
                BingoCardGUI.openGUI(player, card);
            }

            // Check Win Condition (First line or blackout)
            if (card.hasBingoLine()) {
                handleWin(player);
            }

            return true;
        }

        return false;
    }

    private void handleWin(Player winner) {
        state = BingoSessionState.CONCLUDED;

        // Broadcast victory message
        Component victoryMsg = Component.text("BINGO! ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(winner.getName() + " completed a Bingo line in " + formatTime(elapsedSeconds) + "!", NamedTextColor.GREEN));

        for (Player p : players) {
            if (p.isOnline()) {
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

        // Transition winner to spectator via RGA control and request session conclusion
        if (rgaControl != null) {
            rgaControl.setSpectator(winner, true);
            rgaControl.requestConclude(sessionId, winner.getName());
        }

        // Fire GameSessionRequestConcludeEvent to notify RGA orchestrator
        GameSessionRequestConcludeEvent concludeEvent = new GameSessionRequestConcludeEvent(sessionId, winner.getName(), "BINGO_LINE_COMPLETED");
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

        for (Player p : players) {
            scoreboardManager.removeBoard(p);
        }

        playerCards.clear();
        frozenLocations.clear();
    }

    public boolean isFrozen(Player player) {
        return state == BingoSessionState.COUNTDOWN && frozenLocations.containsKey(player.getUniqueId());
    }

    public Location getFrozenLocation(Player player) {
        return frozenLocations.get(player.getUniqueId());
    }

    public String getSessionId() {
        return sessionId;
    }

    public World getWorld() {
        return world;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
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
