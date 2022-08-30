package com.cokes86.cokesaddon.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ParticipantHideEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancellable = false;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancellable;
    }

    @Override
    public void setCancelled(boolean b) {
        cancellable = b;
    }
}
