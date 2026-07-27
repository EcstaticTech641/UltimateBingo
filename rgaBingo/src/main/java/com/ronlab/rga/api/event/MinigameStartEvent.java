package com.ronlab.rga.api.event;

import com.ronlab.rga.api.RgaControl;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class MinigameStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String id;
    private final String sessionId;
    private final World world;
    private final List<Player> players;
    private final RgaControl rgaControl;

    public MinigameStartEvent(String id, String sessionId, World world, List<Player> players, RgaControl rgaControl) {
        this.id = id;
        this.sessionId = sessionId;
        this.world = world;
        this.players = players;
        this.rgaControl = rgaControl;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public World getWorld() {
        return world;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public RgaControl getRgaControl() {
        return rgaControl;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
