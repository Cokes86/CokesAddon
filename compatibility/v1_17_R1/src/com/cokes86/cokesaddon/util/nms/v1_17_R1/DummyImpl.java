package com.cokes86.cokesaddon.util.nms.v1_17_R1;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.mojang.authlib.GameProfile;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_17_R1.network.EmptyNetworkHandler;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_17_R1.network.EmptyNetworkManager;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@SuppressWarnings("all")
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
        super(server, world, createProfile(skin));
        this.gameMode.setGameMode(EnumGamemode.SURVIVAL);
        try {
            this.networkManager = new EmptyNetworkManager(EnumProtocolDirection.CLIENTBOUND);
            this.connection = new EmptyNetworkHandler(server, networkManager, this);
            networkManager.setPacketListener(connection);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        getDataWatcher().set(new DataWatcherObject<>(17, DataWatcherRegistry.BYTE), SKIN_BIT_LAYER);
        new BukkitRunnable() {
            @Override
            public void run() {
                world.addEntity(DummyImpl.this, SpawnReason.CUSTOM);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
        ((Entity) this).setPosition(location.getX(), location.getY(), location.getZ());
        this.invulnerableTime = 0;
        this.hologram = NMS.newHologram(world.getWorld(), ((Entity) this).locX(), ((Entity) this).locY() + 2, ((Entity) this).locZ(), skin.getDisplayName());
        this.origin = skin;
    }

    @Override
    public void tick() {
        if (!isAlive() || hologram.isUnregistered() || !plugin.isEnabled()) {
            remove();
            return;
        }
        hologram.teleport(getWorld().getWorld(), ((Entity) this).locX(), ((Entity) this).locY() + 2, ((Entity) this).locZ(), ((Entity) this).getYRot(), ((Entity) this).getXRot());
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
    public void display(final Player player) {
        hologram.display(player);
        final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().connection;
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
        ((WorldServer) getWorld()).getChunkProvider().removeEntity(this);
        if (!hologram.isUnregistered()) {
            hologram.unregister();
        }
        final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(getId());
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            player.getHandle().connection.sendPacket(packet);
        }
    }

    @Override
    public boolean isAlive() {
        return ((EntityLiving) this).isAlive();
    }
}