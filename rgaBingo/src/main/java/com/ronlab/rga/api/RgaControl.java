package com.ronlab.rga.api;

import org.bukkit.entity.Player;

/**
 * Controller interface provided by RonlabGameAssistant (RGA)
 * to manage session state and spectator mode.
 */
public interface RgaControl {
    void setSpectator(Player player, boolean spectator);
    void requestConclude(String sessionId, String winnerName);
}
