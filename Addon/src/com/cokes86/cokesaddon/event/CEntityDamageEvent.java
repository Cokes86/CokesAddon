package com.cokes86.cokesaddon.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.Nullable;

public class CEntityDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Entity damager;
    private final EntityDamageEvent e;
    private boolean cancelled;

    public CEntityDamageEvent(EntityDamageEvent e) {
        this.e = e;
        if (e instanceof EntityDamageByEntityEvent) {
            damager = ((EntityDamageByEntityEvent) e).getDamager();
        } else {
            damager = null;
        }
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public @Nullable Entity getDamager() {
        return damager;
    }

    @Override
    public boolean isCancelled() {
        return e.isCancelled() || cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        e.setCancelled(b);
        cancelled = b;
    }

    public Entity getEntity() {
        return e.getEntity();
    }

    public double getDamage(){
        return e.getDamage();
    }

    public double getFinalDamage() {
        return e.getFinalDamage();
    }

    public void setDamage(double damage) {
        e.setDamage(damage);
    }

    public DamageCause getCause() {
        return e.getCause();
    }
}
