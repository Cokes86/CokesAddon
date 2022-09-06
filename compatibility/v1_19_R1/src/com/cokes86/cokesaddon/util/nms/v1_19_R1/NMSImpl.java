package com.cokes86.cokesaddon.util.nms.v1_19_R1;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.INMS;
import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.collect.Pair;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class NMSImpl implements INMS {
    @Override
    public void setCritical(Entity arrow, boolean critical) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of AbstractArrow");
        ((Arrow) arrow).setCritical(critical);
    }

    @Override
    public boolean isCritical(Entity arrow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an instance of AbstractArrow");
        return ((Arrow) arrow).isCritical();
    }

    @Override
    public boolean damageMagicFixed(@NotNull org.bukkit.entity.Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.hurt(DamageSource.indirectMagic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull org.bukkit.entity.Entity entity, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.hurt(DamageSource.WITHER, damage);
    }

    @Override
    public boolean damageVoid(org.bukkit.entity.@NotNull Entity entity, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.hurt(DamageSource.OUT_OF_WORLD, damage);
    }


    private static final Map<UUID, Pair<String, Property>> origin = new HashMap<>();

    @Override
    public void changeSkin(Player player, UUID uuid) {
        if (origin.containsKey(uuid)) {
            CraftPlayer cp = (CraftPlayer) player;
            cp.getProfile().getProperties().removeAll("textures");
            cp.getProfile().getProperties().put("textures", origin.get(uuid).getRight());
        }
    }

    @Override
    public void saveSkinData() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (origin.containsKey(pl.getUniqueId())) continue;
            if (pl.getName().isEmpty()) continue;
            CraftPlayer cp = (CraftPlayer) pl;

            Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();
            if (iterator.hasNext()) {
                origin.put(pl.getUniqueId(), Pair.of(pl.getName(), iterator.next()));
            }
        }
    }

    @Override
    public void addSkinData(UUID uuid) {
        if (origin.containsKey(uuid)) return;
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        CraftPlayer cp = (CraftPlayer) pl;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();
        if (iterator.hasNext()) {
            origin.put(uuid, Pair.of(pl.getName(), iterator.next()));
        }
    }

    @Override
    public void clearSkinData() {
        origin.clear();
    }

    @Override
    public void setPlayerNameTag(Player p, UUID uuid) {
        if (origin.containsKey(uuid)) {
            try {
                EntityPlayer enp = ((CraftPlayer)p).getHandle();

                Object obj = enp.getClass().getMethod("getProfile", new Class[0]).invoke(enp);
                Field nameField = obj.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(obj, origin.get(uuid).getLeft());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isChangedSkin(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        boolean skin = false;
        boolean nameTag = false;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();

        if (iterator.hasNext() && origin.containsKey(player.getUniqueId())) {
            Property property = iterator.next();
            skin = property != origin.get(player.getUniqueId()).getRight();

            EntityPlayer enp = cp.getHandle();
            nameTag = !origin.get(player.getUniqueId()).getLeft().equals(enp.getGameProfile().getName());
        }

        return skin || nameTag;
    }

    @Override
    public void reloadPlayerSkin(Player p) {
        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).connection.send(new PacketPlayOutPlayerInfo(
                        EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).connection.send(new PacketPlayOutPlayerInfo(
                        EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(AbilityWar.getPlugin(), p));
        Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(AbilityWar.getPlugin(), p));
    }

    @Override
    public IDummy createDummy(Location location, Player player) {
        return new DummyImpl(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) location.getWorld()).getHandle(), location, player);
    }

    private final Map<Player, StealthImpl> hideMap = new HashMap<>();

    @Override
    public void hidePlayer(Player hide) {
        StealthImpl impl = new StealthImpl(hide);
        impl.hidePlayer();
        hideMap.put(hide, impl);
    }

    @Override
    public void showPlayer(Player show) {
        StealthImpl impl = hideMap.remove(show);
        if (impl != null) {
            impl.showPlayer();
        }
    }
}
