package com.ronlab.rga.api.event;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MinigameConcludeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String sessionId;
    private final World world;

    public MinigameConcludeEvent(String sessionId, World world) {
        this.sessionId = sessionId;
        this.world = world;
    }

    public String getSessionId() {
        return sessionId;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
