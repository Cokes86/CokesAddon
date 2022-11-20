package com.cokes86.cokesaddon.util.nms.v1_16_R3;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.INMS;
import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.collect.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.v1_16_R3.*;

import java.lang.reflect.Field;
import java.util.*;

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
    public boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.server.v1_16_R3.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.damageEntity(magic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull Entity entity, float damage) {
        net.minecraft.server.v1_16_R3.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.damageEntity(DamageSource.WITHER, damage);
    }

    @Override
    public boolean damageVoid(@NotNull Entity entity, float damage) {
        net.minecraft.server.v1_16_R3.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.damageEntity(DamageSource.OUT_OF_WORLD, damage);
    }

    @SuppressWarnings("all")
    private EntityDamageSourceIndirect magic(net.minecraft.server.v1_16_R3.Entity nmsEntity, EntityPlayer nmsDamager) {
        EntityDamageSourceIndirect source;
        if (nmsDamager != null) {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.getWorld(), nmsDamager)), nmsDamager) {{
                setMagic(); setIgnoreArmor(); setStarvation();
            }};
        } else {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.getWorld(), nmsEntity.locX(), nmsEntity.locY(), nmsEntity.locZ())), null){{
                setMagic(); setIgnoreArmor(); setStarvation();
            }};
        }
        return source;
    }

    private EntityPotion setItem(final EntityPotion potion) {
        potion.setItem(SPLASH_POTION);
        return potion;
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
    @SuppressWarnings("all")
    public IDummy createDummy(Location location, Player player) {
        return new DummyImpl(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) location.getWorld()).getHandle(), location, player);
    }

    private final Map<Player, StealthImpl> hideMap = new HashMap<>();

    @Override
    public void hidePlayer(Participant hide) {
        if (!hideMap.containsKey(hide.getPlayer())) {
            StealthImpl impl = new StealthImpl(hide.getPlayer());
            impl.hidePlayer();
            hideMap.put(hide.getPlayer(), impl);
            hide.attributes().TARGETABLE.setValue(false);
        }
    }

    @Override
    public void showPlayer(Participant show) {
        StealthImpl impl = hideMap.remove(show.getPlayer());
        if (impl != null) {
            impl.showPlayer();
            show.attributes().TARGETABLE.setValue(true);
        }
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
