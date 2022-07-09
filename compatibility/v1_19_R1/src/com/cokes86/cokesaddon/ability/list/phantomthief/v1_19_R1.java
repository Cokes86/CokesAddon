package com.cokes86.cokesaddon.ability.list.phantomthief;

import com.cokes86.cokesaddon.ability.list.PhantomThief;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher.Item;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@SuppressWarnings("unchecked")
public class v1_19_R1 extends PhantomThief {
    private final Map<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> channelHandlers = new HashMap<>();
    private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
    private static final List<com.mojang.datafixers.util.Pair<EnumItemSlot, ItemStack>> NULL_PAIR_LIST = Arrays.asList(
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.a, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.b, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.f, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.e, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.d, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.c, ItemStack.b)
    );

    static {
        try {
            BYTE_DATA_WATCHER_OBJECT = ReflectionUtil.FieldUtil.getStaticValue(Entity.class, "Z");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public v1_19_R1(AbstractGame.Participant participant) {
        super(participant);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerJoinEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getHandle().b.a(new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), NULL_PAIR_LIST));
                injectPlayer(player);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerQuitEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            player.getHandle().b.a().m.pipeline().remove(channelHandlers.get(player.getUniqueId()).getRight());
            channelHandlers.remove(player.getUniqueId());
        }
    }

    public void show() {
        getParticipant().attributes().TARGETABLE.setValue(true);
        final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), Arrays.asList(
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.a, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInMainHand())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.b, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInOffHand())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.f, CraftItemStack.asNMSCopy(getPlayer().getInventory().getHelmet())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.e, CraftItemStack.asNMSCopy(getPlayer().getInventory().getChestplate())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.d, CraftItemStack.asNMSCopy(getPlayer().getInventory().getLeggings())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.c, CraftItemStack.asNMSCopy(getPlayer().getInventory().getBoots()))
        ));
        for (Map.Entry<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> entry : channelHandlers.entrySet()) {
            final CraftPlayer player = entry.getValue().getLeft();
            try {
                player.getHandle().b.a().m.pipeline().remove(entry.getValue().getRight());
            } catch (NoSuchElementException ignored) {}
            if (!player.isValid()) continue;
            player.getHandle().b.a(packet);
        }
        channelHandlers.clear();
        new BukkitRunnable() {
            @Override
            public void run() {
                ((CraftPlayer) getPlayer()).getHandle().j(false);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    public void hide() {
        getParticipant().attributes().TARGETABLE.setValue(false);
        final CraftPlayer craftPlayer = (CraftPlayer) getPlayer();
        craftPlayer.getHandle().ai().a(new DataWatcherObject<>(11, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().j(true);
        final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), NULL_PAIR_LIST);
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            if (player.equals(getPlayer())) continue;
            player.getHandle().b.a(packet);
            injectPlayer(player);
        }
    }

    public void injectPlayer(Player player) {
        CraftPlayer player1 = (CraftPlayer) player;
        if (!player.isValid()) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            final Pair<CraftPlayer, ChannelOutboundHandlerAdapter> pair = channelHandlers.get(player.getUniqueId());
            if (!pair.getLeft().isValid()) {
                try {
                    pair.getLeft().getHandle().b.a().m.pipeline().remove(pair.getRight());
                } catch (NoSuchElementException ignored) {}
            } else return;
        }
        final ChannelOutboundHandlerAdapter handler = new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
                if (packet instanceof PacketPlayOutEntityEquipment) {
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        ReflectionUtil.FieldUtil.setValue(packet, "c", ItemStack.a);
                    }
                } else if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        List<Item<?>> items = ReflectionUtil.FieldUtil.getValue(packet, "b");
                        if (items.size() != 0) {
                            Item<?> item = items.get(0);
                            if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
                                Item<Byte> byteItem = (Item<Byte>) item;
                                byteItem.a((byte) (byteItem.b() | 1 << 5));
                                ((CraftPlayer) getPlayer()).getHandle().j(true);
                            }
                        }
                    }
                }
                super.write(ctx, packet, promise);
            }
        };
        channelHandlers.put(player.getUniqueId(), Pair.of(player1, handler));
        player1.getHandle().b.a().m.pipeline().addBefore("packet_handler", hashCode() + ":" + player.getName(), handler);
    }
}
