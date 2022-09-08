package com.cokes86.cokesaddon.util.nms.v1_13_R2;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.mojang.authlib.GameProfile;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_13_R2.network.EmptyNetworkHandler;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_13_R2.network.EmptyNetworkManager;
import net.minecraft.server.v1_13_R2.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class DummyImpl extends EntityPlayer implements IDummy {

    private static GameProfile createProfile(final Player player) {
        final GameProfile profile = new GameProfile(UUID.randomUUID(), player.getDisplayName());
        profile.getProperties().put("textures", ((CraftPlayer)player).getHandle().getProfile().getProperties().get("textures").iterator().next());
        return profile;
    }

    private final JavaPlugin plugin = AbilityWar.getPlugin();
    private final IHologram hologram;
    private double damages = 0;
    private final EmptyNetworkManager networkManager;
    private final Player origin;

    public DummyImpl(final MinecraftServer server, final WorldServer world, final Location location, Player skin) {
        super(server, world, createProfile(skin), new PlayerInteractManager(world));
        this.playerInteractManager.setGameMode(EnumGamemode.SURVIVAL);
        try {
            this.networkManager = new EmptyNetworkManager(EnumProtocolDirection.CLIENTBOUND);
            this.playerConnection = new EmptyNetworkHandler(server, networkManager, this);
            networkManager.setPacketListener(playerConnection);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        getDataWatcher().set(new DataWatcherObject<>(14, DataWatcherRegistry.a), SKIN_BIT_LAYER);
        new BukkitRunnable() {
            @Override
            public void run() {
                world.addEntity(DummyImpl.this, SpawnReason.CUSTOM);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
        setPosition(location.getX(), location.getY(), location.getZ());
        this.invulnerableTicks = 0;
        this.hologram = NMS.newHologram(world.getWorld(), locX, locY + 2, locZ, skin.getDisplayName());
        this.origin = skin;
    }

    @Override
    public void tick() {
        if (!isAlive() || hologram.isUnregistered() || !plugin.isEnabled()) {
            remove();
            return;
        }
        hologram.teleport(getWorld().getWorld(), locX, locY + 2, locZ, yaw, pitch);
        super.tick();
    }

    @Override
    public void addDamage(final double damage) {
        this.damages += damage;
    }

    @Override
    public double getDamage() {
        return damages;
    }

    @Override
    public void die(DamageSource damagesource) {
        networkManager.setConnected(false);
        super.die(damagesource);
    }

    @Override
    public void die() {
        networkManager.setConnected(false);
        super.die();
    }

    @Override
    public void display(final Player player) {
        hologram.display(player);
        final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, this));
        playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(this));
        new BukkitRunnable() {
            @Override
            public void run() {
                playerConnection.sendPacket(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, DummyImpl.this));
            }
        }.runTaskLater(AbilityWar.getPlugin(), 50L);
    }

    private void updateHologram() {
        if (hologram.isUnregistered()) {
            return;
        }
        hologram.setText(origin.getDisplayName());
    }

    private double floor(final double a) {
        return Math.floor(a * 1000) / 1000;
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return super.getBukkitEntity();
    }

    @Override
    public void remove() {
        die();
        getWorld().removeEntity(this);
        if (!hologram.isUnregistered()) {
            hologram.unregister();
        }
        final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(getId());
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            player.getHandle().playerConnection.sendPacket(packet);
        }
    }

}