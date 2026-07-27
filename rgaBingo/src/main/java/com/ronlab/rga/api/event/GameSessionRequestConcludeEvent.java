package com.ronlab.rga.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameSessionRequestConcludeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String sessionId;
    private final String winnerName;
    private final String reason;

    public GameSessionRequestConcludeEvent(String sessionId, String winnerName, String reason) {
        this.sessionId = sessionId;
        this.winnerName = winnerName;
        this.reason = reason;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
