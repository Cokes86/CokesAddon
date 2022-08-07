package com.cokes86.cokesaddon.util.dummy;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface DummyImpl {
    void display(final Player player);
    Player getBukkitEntity();
    UUID getUniqueID();
    DummyImpl create(final Server server, final World world, final Location location, Player player);
    void remove();
    boolean isAlive();
}
