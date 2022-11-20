package com.cokes86.cokesaddon.util.nms.v1_19_R1;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.mojang.authlib.GameProfile;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_19_R1.network.EmptyNetworkHandler;
import daybreak.abilitywar.utils.base.minecraft.nms.v1_19_R1.network.EmptyNetworkManager;
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
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@SuppressWarnings("RedundantCast")
public class DummyImpl extends EntityPlayer implements IDummy {

    private static GameProfile createProfile(final Player player) {
        final GameProfile profile = new GameProfile(UUID.randomUUID(), player.getDisplayName());
        profile.getProperties().put("textures", ((EntityHuman)((CraftPlayer)player).getHandle()).getGameProfile().getProperties().get("textures").iterator().next());
        return profile;
    }

    private final JavaPlugin plugin = AbilityWar.getPlugin();
    private double damages = 0;
    private final EmptyNetworkManager networkManager;

    public DummyImpl(final MinecraftServer server, final WorldServer world, final Location location, final Player player) {
        super(server, world, createProfile(player), null);
        this.gameMode.changeGameModeForPlayer(EnumGamemode.SURVIVAL);
        try {
            this.networkManager = new EmptyNetworkManager(EnumProtocolDirection.CLIENTBOUND);
            this.connection = new EmptyNetworkHandler(server, networkManager, this);
            networkManager.setListener(connection);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        ((Entity) this).getEntityData().set(new DataWatcherObject<>(17, DataWatcherRegistry.BYTE), SKIN_BIT_LAYER);
        new BukkitRunnable() {
            @Override
            public void run() {
                world.addFreshEntity(DummyImpl.this, SpawnReason.CUSTOM);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
        ((Entity) this).setPos(location.getX(), location.getY(), location.getZ());
        this.spawnInvulnerableTime = 0;
    }

    @Override
    public void tick() {
        if (!isAlive() || !plugin.isEnabled()) {
            remove();
            return;
        }
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
    public void display(final Player player) {
        final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().connection;
        playerConnection.send(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, this));
        playerConnection.send(new PacketPlayOutNamedEntitySpawn(this));
        new BukkitRunnable() {
            @Override
            public void run() {
                playerConnection.send(new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, DummyImpl.this));
            }
        }.runTaskLater(AbilityWar.getPlugin(), 50L);
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return super.getBukkitEntity();
    }

    @Override
    public UUID getUniqueID() {
        return ((Entity) this).getUUID();
    }

    @Override
    public void die(DamageSource damagesource) {
        networkManager.setConnected(false);
        super.die(damagesource);
    }

    @Override
    public void remove() {
        ((Entity) this).discard();
        getLevel().getChunkSource().removeEntity(this);
        final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((Entity) this).getId());
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            player.getHandle().connection.send(packet);
        }
    }

    @Override
    public boolean isAlive() {
        return ((EntityLiving) this).isAlive();
    }
}
