package com.cokes86.cokesaddon.util.dummy;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Dummy {
    private final Map<UUID, DummyImpl> dummies = new HashMap<>();
    private static DummyImpl instance;

    public Dummy(final Server server, final World world, final Location location, Player player) {
        try {
            instance = Class.forName("com.cokes86.cokesaddon.util.dummy." + ServerVersion.getName())
                    .asSubclass(DummyImpl.class).getConstructor(Server.class, World.class, Location.class, Player.class).newInstance(server, world, location, player);
        } catch (Exception e) {
            instance = null;
            throw new VersionNotSupportedException();
        }
    }

    public void display(final Player player) {
        instance.display(player);
    }
    public Player getBukkitEntity() {
        return instance.getBukkitEntity();
    }
    public UUID getUniqueID() {
        return instance.getUniqueID();
    }
    public void remove() {
        instance.remove();
    }
    public boolean isAlive()  {
        return instance.isAlive();
    }

    public void create(final Server server, final World world, final Location location, Player player) {
        DummyImpl dummy = instance.create(server, world, location, player);
        dummies.put(dummy.getUniqueID(), dummy);
        for (Player players : Bukkit.getOnlinePlayers()) {
            dummy.display(players);
        }
    }

    public DummyImpl getDummy(UUID uuid) {
        return dummies.getOrDefault(uuid, null);
    }

    public Map<UUID, DummyImpl> getDummies() {
        return dummies;
    }
}
