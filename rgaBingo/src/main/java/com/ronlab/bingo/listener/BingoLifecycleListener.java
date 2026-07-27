package com.ronlab.bingo.listener;

import com.ronlab.bingo.BingoPlugin;
import com.ronlab.bingo.model.BingoSession;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

public class BingoLifecycleListener implements Listener {

    private final BingoPlugin plugin;
    private final Logger logger;

    public BingoLifecycleListener(BingoPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onMinigameStart(MinigameStartEvent event) {
        if (event.getId() == null) {
            return;
        }

        String normalizedId = event.getId().toLowerCase().trim().replace("_", "").replace("-", "");
        if (!normalizedId.equals("bingo") && !normalizedId.equals("ultimatebingo")) {
            logger.fine("Ignoring MinigameStartEvent for non-matching minigame ID: " + event.getId());
            return;
        }

        logger.info("Initializing rgaBingo session: " + event.getSessionId() + " in world: " + event.getWorld().getName());

        BingoSession session = new BingoSession(
                event.getSessionId(),
                event.getWorld(),
                event.getPlayers(),
                event.getRgaControl(),
                plugin,
                plugin.getScoreboardManager()
        );

        plugin.registerSession(event.getSessionId(), session);
        session.initialize();
    }

    @EventHandler
    public void onMinigameConclude(MinigameConcludeEvent event) {
        BingoSession session = plugin.getSession(event.getSessionId());
        if (session != null) {
            logger.info("Concluding rgaBingo session: " + event.getSessionId());
            session.conclude();
            plugin.unregisterSession(event.getSessionId());
        }
    }
}
