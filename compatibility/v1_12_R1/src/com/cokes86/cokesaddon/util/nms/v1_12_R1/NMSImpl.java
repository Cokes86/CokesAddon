package com.cokes86.cokesaddon.util.nms.v1_12_R1;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.INMS;
import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.collect.Pair;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
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
        if (!(arrow instanceof Arrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        ((Arrow) arrow).setCritical(critical);
    }

    @Override
    public boolean isCritical(Entity arrow) {
        if (!(arrow instanceof Arrow)) throw new IllegalArgumentException("arrow must be an instance of Arrow");
        return ((Arrow) arrow).isCritical();
    }

    private static final ItemStack SPLASH_POTION = new ItemStack(Items.SPLASH_POTION);

    @Override
    public boolean damageMagicFixed(@NotNull org.bukkit.entity.Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.damageEntity(magic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull org.bukkit.entity.Entity entity, float damage) {
        net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.damageEntity(DamageSource.WITHER, damage);
    }

    @Override
    public boolean damageVoid(org.bukkit.entity.@NotNull Entity entity, float damage) {
        net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.damageEntity(DamageSource.OUT_OF_WORLD, damage);
    }

    private EntityDamageSourceIndirect magic(net.minecraft.server.v1_12_R1.Entity nmsEntity, EntityPlayer nmsDamager) {
        EntityDamageSourceIndirect source;
        if (nmsDamager != null) {
            source = new EntityDamageSourceIndirect("magic", new EntityPotion(nmsEntity.getWorld(), nmsDamager, SPLASH_POTION), nmsDamager) {{
                setMagic(); setIgnoreArmor(); m();
            }};
        } else {
            source = new EntityDamageSourceIndirect("magic", new EntityPotion(nmsEntity.getWorld(), nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ, SPLASH_POTION), null){{
                setMagic(); setIgnoreArmor(); m();
            }};
        }
        return source;
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
    public boolean isChangedSkin(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        boolean skin = false;
        boolean nametag = false;

        Iterator<Property> iterator = cp.getProfile().getProperties().get("textures").iterator();

        if (iterator.hasNext() && origin.containsKey(player.getUniqueId())) {
            Property property = iterator.next();
            skin = property != origin.get(player.getUniqueId()).getRight();

            EntityPlayer enp = cp.getHandle();
            nametag = !origin.get(player.getUniqueId()).getLeft().equals(enp.getProfile().getName());
        }

        return skin || nametag;
    }

    @Override
    public IDummy createDummy(Location location, Player player) {
        return new DummyImpl((MinecraftServer) Bukkit.getServer(), (WorldServer) location.getWorld(), location, player);
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
    public void reloadPlayerSkin(Player p) {
        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl ->
                (((CraftPlayer)pl).getHandle()).playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer)pl).getHandle())));

        Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(AbilityWar.getPlugin(), p));
        Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(AbilityWar.getPlugin(), p));
    }
}
