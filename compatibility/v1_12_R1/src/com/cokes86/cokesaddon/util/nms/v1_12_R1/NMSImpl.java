package com.cokes86.cokesaddon.util.nms.v1_12_R1;

import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.INMS;
import com.mojang.authlib.properties.Property;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil.FieldUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class NMSImpl implements INMS {
    private final Set<UUID> hideSet = new HashSet<>();

    private final Map<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> channelHandlers = new HashMap<>();

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
        return new DummyImpl(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) location.getWorld()).getHandle(), location, player);
    }

    @Override
    public void hidePlayer(Player hide) {
        final CraftPlayer craftPlayer = (CraftPlayer) hide;
        craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(10, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().setInvisible(true);
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.MAINHAND, ItemStack.a),
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.OFFHAND, ItemStack.a),
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.HEAD, ItemStack.a),
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.CHEST, ItemStack.a),
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.LEGS, ItemStack.a),
                new PacketPlayOutEntityEquipment(hide.getEntityId(), EnumItemSlot.FEET, ItemStack.a)
        };
        injectSelf(hide);
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            if (player.equals(hide)) continue;
            for (PacketPlayOutEntityEquipment packet : packets) {
                player.getHandle().playerConnection.sendPacket(packet);
            }
            injectPlayer(hide, player);
        }
        hideSet.add(hide.getUniqueId());
    }

    @Override
    public void showPlayer(Player show) {
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(show.getInventory().getItemInMainHand())),
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(show.getInventory().getItemInOffHand())),
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(show.getInventory().getHelmet())),
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(show.getInventory().getChestplate())),
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(show.getInventory().getLeggings())),
                new PacketPlayOutEntityEquipment(show.getEntityId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(show.getInventory().getBoots()))
        };
        for (Entry<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> entry : channelHandlers.entrySet()) {
            final CraftPlayer player = entry.getValue().getLeft();
            try {
                player.getHandle().playerConnection.networkManager.channel.pipeline().remove(entry.getValue().getRight());
            } catch (NoSuchElementException ignored) {}
            if (!player.isValid()) continue;
            for (PacketPlayOutEntityEquipment packet : packets) {
                player.getHandle().playerConnection.sendPacket(packet);
            }
        }
        channelHandlers.remove(show.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                ((CraftPlayer) show).getHandle().setInvisible(false);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
        hideSet.remove(show.getUniqueId());
    }

    @Override
    public void onPlayerJoin(Player hiding, PlayerJoinEvent e) {
        if (hideSet.contains(hiding.getUniqueId())) return;
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.equals(hiding)) {
                    injectSelf(hiding);
                } else {
                    for (PacketPlayOutEntityEquipment packet : new PacketPlayOutEntityEquipment[] {
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.MAINHAND, ItemStack.a),
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.OFFHAND, ItemStack.a),
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.HEAD, ItemStack.a),
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.CHEST, ItemStack.a),
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.LEGS, ItemStack.a),
                            new PacketPlayOutEntityEquipment(hiding.getEntityId(), EnumItemSlot.FEET, ItemStack.a)
                    }) {
                        player.getHandle().playerConnection.sendPacket(packet);
                    }
                    injectPlayer(hiding, player);
                }
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    @Override
    public void onPlayerQuit(Player hiding, PlayerQuitEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (channelHandlers.containsKey(player.getUniqueId())) {
            try {
                player.getHandle().playerConnection.networkManager.channel.pipeline().remove(channelHandlers.remove(player.getUniqueId()).getRight());
            } catch (NoSuchElementException ignored) {
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectPlayer(Player hiding, Player inject) {
        if (!inject.isValid()) return;
        final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
        try {
            BYTE_DATA_WATCHER_OBJECT = FieldUtil.getStaticValue(Entity.class, "Z");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (channelHandlers.containsKey(inject.getUniqueId())) {
            final Pair<CraftPlayer, ChannelOutboundHandlerAdapter> pair = channelHandlers.get(inject.getUniqueId());
            if (!pair.getLeft().isValid()) {
                try {
                    pair.getLeft().getHandle().playerConnection.networkManager.channel.pipeline().remove(pair.getRight());
                } catch (NoSuchElementException ignored) {}
            } else return;
        }
        final ChannelOutboundHandlerAdapter handler = new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
                if (packet instanceof PacketPlayOutEntityEquipment) {
                    if ((int) FieldUtil.getValue(packet, "a") == hiding.getEntityId()) {
                        FieldUtil.setValue(packet, "c", ItemStack.a);
                    }
                } else if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) FieldUtil.getValue(packet, "a") == hiding.getEntityId()) {
                        List<net.minecraft.server.v1_12_R1.DataWatcher.Item<?>> items = FieldUtil.getValue(packet, "b");
                        if (items.size() != 0) {
                            net.minecraft.server.v1_12_R1.DataWatcher.Item<?> item = items.get(0);
                            if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
                                net.minecraft.server.v1_12_R1.DataWatcher.Item<Byte> byteItem = (net.minecraft.server.v1_12_R1.DataWatcher.Item<Byte>) item;
                                byteItem.a((byte) (byteItem.b() | 1 << 5));
                                ((CraftPlayer) hiding).getHandle().setInvisible(true);
                            }
                        }
                    }
                }
                super.write(ctx, packet, promise);
            }
        };
        channelHandlers.put(inject.getUniqueId(), Pair.of((CraftPlayer) inject, handler));
        ((CraftPlayer) inject).getHandle().playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", hashCode() + ":" + inject.getName(), handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectSelf(Player hiding) {
        final CraftPlayer player = (CraftPlayer) hiding;
        final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
        try {
            BYTE_DATA_WATCHER_OBJECT = FieldUtil.getStaticValue(Entity.class, "Z");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (!player.isValid()) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            final Pair<CraftPlayer, ChannelOutboundHandlerAdapter> pair = channelHandlers.get(player.getUniqueId());
            if (!pair.getLeft().isValid()) {
                try {
                    pair.getLeft().getHandle().playerConnection.networkManager.channel.pipeline().remove(pair.getRight());
                } catch (NoSuchElementException ignored) {
                }
            } else return;
        }
        final ChannelOutboundHandlerAdapter handler = new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
                if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) FieldUtil.getValue(packet, "a") == hiding.getEntityId()) {
                        List<net.minecraft.server.v1_12_R1.DataWatcher.Item<?>> items = FieldUtil.getValue(packet, "b");
                        if (items.size() != 0) {
                            net.minecraft.server.v1_12_R1.DataWatcher.Item<?> item = items.get(0);
                            if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
                                net.minecraft.server.v1_12_R1.DataWatcher.Item<Byte> byteItem = (net.minecraft.server.v1_12_R1.DataWatcher.Item<Byte>) item;
                                byteItem.a((byte) (byteItem.b() | 1 << 5));
                                ((CraftPlayer) hiding).getHandle().setInvisible(true);
                            }
                        }
                    }
                }
                super.write(ctx, packet, promise);
            }
        };
        channelHandlers.put(player.getUniqueId(), Pair.of(player, handler));
        player.getHandle().playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", hashCode() + ":" + player.getName(), handler);
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
