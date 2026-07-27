package com.ronlab.bingo;

import com.ronlab.bingo.hud.BingoScoreboardManager;
import com.ronlab.bingo.listener.BingoLifecycleListener;
import com.ronlab.bingo.listener.BingoListener;
import com.ronlab.bingo.model.BingoSession;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BingoPlugin extends JavaPlugin {

    private final Map<String, BingoSession> activeSessions = new ConcurrentHashMap<>();
    private BingoScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing rgaBingo v" + getPluginMeta().getVersion() + " (CPMK Companion Mode)");

        scoreboardManager = new BingoScoreboardManager();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BingoLifecycleListener(this), this);
        getServer().getPluginManager().registerEvents(new BingoListener(this), this);

        getLogger().info("rgaBingo successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling rgaBingo...");

        for (BingoSession session : activeSessions.values()) {
            session.conclude();
        }
        activeSessions.clear();

        if (scoreboardManager != null) {
            scoreboardManager.removeAll();
        }

        getLogger().info("rgaBingo disabled.");
    }

    public void registerSession(String sessionId, BingoSession session) {
        activeSessions.put(sessionId, session);
    }

    public void unregisterSession(String sessionId) {
        BingoSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.conclude();
        }
    }

    public BingoSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public BingoSession getSessionForPlayer(Player player) {
        for (BingoSession session : activeSessions.values()) {
            for (Player p : session.getPlayers()) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    return session;
                }
            }
        }
        return null;
    }

    public BingoScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
