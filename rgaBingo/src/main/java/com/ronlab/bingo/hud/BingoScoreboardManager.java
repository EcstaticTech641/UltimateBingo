package com.ronlab.bingo.hud;

import fr.mrmicky.fastboard.FastBoard;
import com.ronlab.bingo.model.BingoCard;
import com.ronlab.bingo.model.BingoSession;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BingoScoreboardManager {

    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public void createBoard(Player player, BingoSession session) {
        if (boards.containsKey(player.getUniqueId())) {
            removeBoard(player);
        }

        FastBoard board = new FastBoard(player);
        board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "BINGO " + ChatColor.GRAY + "(" + formatTime(session.getElapsedSeconds()) + ")");
        boards.put(player.getUniqueId(), board);
        updateBoard(player, session);
    }

    public void updateBoard(Player player, BingoSession session) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) {
            return;
        }

        board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "BINGO " + ChatColor.GRAY + "(" + formatTime(session.getElapsedSeconds()) + ")");

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "-------------------");

        BingoCard playerCard = session.getCard(player);
        if (playerCard != null) {
            int completed = playerCard.getCompletedCount();
            int total = playerCard.getTotalCount();
            int bingoLines = playerCard.countCompletedLines();

            lines.add(ChatColor.YELLOW + "Your Items: " + ChatColor.GREEN + completed + "/" + total);
            lines.add(ChatColor.YELLOW + "Lines Complete: " + ChatColor.AQUA + bingoLines);
        }

        lines.add("");
        lines.add(ChatColor.GOLD + "Top Players:");

        // Sort players by completed count
        List<Player> players = new ArrayList<>(session.getPlayers());
        players.sort((p1, p2) -> {
            BingoCard c1 = session.getCard(p1);
            BingoCard c2 = session.getCard(p2);
            int count1 = c1 != null ? c1.getCompletedCount() : 0;
            int count2 = c2 != null ? c2.getCompletedCount() : 0;
            return Integer.compare(count2, count1);
        });

        int rank = 1;
        for (Player p : players) {
            if (rank > 5) break; // Display top 5
            BingoCard card = session.getCard(p);
            int count = card != null ? card.getCompletedCount() : 0;
            int linesCount = card != null ? card.countCompletedLines() : 0;
            lines.add(ChatColor.GRAY + "#" + rank + " " + ChatColor.WHITE + p.getName() + ": "
                    + ChatColor.GREEN + count + " items" + ChatColor.GRAY + " (" + linesCount + " L)");
            rank++;
        }

        lines.add(ChatColor.GRAY + "-------------------");
        board.updateLines(lines);
    }

    public void updateAll(BingoSession session) {
        for (Player player : session.getPlayers()) {
            updateBoard(player, session);
        }
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) {
            board.delete();
        }
    }

    public void removeAll() {
        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) {
                board.delete();
            }
        }
        boards.clear();
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
