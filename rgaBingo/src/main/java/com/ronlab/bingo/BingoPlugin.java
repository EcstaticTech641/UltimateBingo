package com.ronlab.bingo;

import com.ronlab.bingo.hud.BingoScoreboardManager;
import com.ronlab.bingo.listener.BingoEventListener;
import com.ronlab.bingo.model.BingoSession;
import com.ronlab.rga.api.RGASessionControl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BingoPlugin extends JavaPlugin {

    private final Map<String, BingoSession> activeSessions = new ConcurrentHashMap<>();
    private BingoScoreboardManager scoreboardManager;
    private RGASessionControl rgaSessionControl;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Initializing rgaBingo v" + getPluginMeta().getVersion() + " (CPMK Companion Mode)");

        scoreboardManager = new BingoScoreboardManager();

        // Resolve RGA Session Control service if available
        RegisteredServiceProvider<RGASessionControl> rsp = Bukkit.getServicesManager().getRegistration(RGASessionControl.class);
        if (rsp != null) {
            rgaSessionControl = rsp.getProvider();
            getLogger().info("Successfully hooked into RGASessionControl service.");
        } else {
            getLogger().info("RGASessionControl service not found in ServicesManager; operating in standalone event bus mode.");
        }

        // Register consolidated event listener
        getServer().getPluginManager().registerEvents(new BingoEventListener(this), this);

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

    public void registerSession(String worldName, BingoSession session) {
        activeSessions.put(worldName, session);
    }

    public void unregisterSession(String worldName) {
        BingoSession session = activeSessions.remove(worldName);
        if (session != null) {
            session.conclude();
        }
    }

    public BingoSession getSession(String worldName) {
        return activeSessions.get(worldName);
    }

    public BingoSession getSessionForPlayer(Player player) {
        for (BingoSession session : activeSessions.values()) {
            if (session.getPlayerUuids().contains(player.getUniqueId())) {
                return session;
            }
        }
        return null;
    }

    public BingoScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public RGASessionControl getRgaSessionControl() {
        return rgaSessionControl;
    }
}
