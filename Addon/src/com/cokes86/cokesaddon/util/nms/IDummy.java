package com.cokes86.cokesaddon.util.nms;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface IDummy {
    byte SKIN_BIT_LAYER = 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;

    void addDamage(double damage);
    double getDamage();

    void display(final Player player);
    Player getBukkitEntity();
    UUID getUniqueID();
    void remove();
    boolean isAlive();
}
