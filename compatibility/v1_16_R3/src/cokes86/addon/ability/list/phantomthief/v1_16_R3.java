package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_16_R3.*;
import net.minecraft.server.v1_16_R3.DataWatcher.Item;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class v1_16_R3 implements IPhantom {
    private final Map<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> channelHandlers = new HashMap<>();
    private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
    private static final List<com.mojang.datafixers.util.Pair<EnumItemSlot, ItemStack>> NULL_PAIR_LIST = Arrays.asList(
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.MAINHAND, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.OFFHAND, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.HEAD, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.CHEST, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.LEGS, ItemStack.b),
            com.mojang.datafixers.util.Pair.of(EnumItemSlot.FEET, ItemStack.b)
    );

    static {
        try {
            BYTE_DATA_WATCHER_OBJECT = ReflectionUtil.FieldUtil.getStaticValue(Entity.class, "S");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPlayerJoin(AbilityBase owner, PlayerJoinEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(owner.getPlayer())) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), NULL_PAIR_LIST));
                injectPlayer(owner, player);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    public void onPlayerQuit(AbilityBase owner, PlayerQuitEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(owner.getPlayer())) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            player.getHandle().playerConnection.networkManager.channel.pipeline().remove(channelHandlers.get(player.getUniqueId()).getRight());
            channelHandlers.remove(player.getUniqueId());
        }
    }

    public void show(AbilityBase owner) {
        owner.getParticipant().attributes().TARGETABLE.setValue(true);
        final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), Arrays.asList(
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getItemInMainHand())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getItemInOffHand())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getHelmet())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getChestplate())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getLeggings())),
                com.mojang.datafixers.util.Pair.of(EnumItemSlot.FEET, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getBoots()))
        ));
        for (Map.Entry<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> entry : channelHandlers.entrySet()) {
            final CraftPlayer player = entry.getValue().getLeft();
            try {
                player.getHandle().playerConnection.networkManager.channel.pipeline().remove(entry.getValue().getRight());
            } catch (NoSuchElementException ignored) {}
            if (!player.isValid()) continue;
            player.getHandle().playerConnection.sendPacket(packet);
        }
        channelHandlers.clear();
        new BukkitRunnable() {
            @Override
            public void run() {
                ((CraftPlayer) owner.getPlayer()).getHandle().setInvisible(false);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    public void hide(AbilityBase owner) {
        owner.getParticipant().attributes().TARGETABLE.setValue(false);
        final CraftPlayer craftPlayer = (CraftPlayer) owner.getPlayer();
        craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(11, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().setInvisible(true);
        final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), NULL_PAIR_LIST);
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            if (player.equals(owner.getPlayer())) continue;
            player.getHandle().playerConnection.sendPacket(packet);
            injectPlayer(owner, player);
        }
    }

    public void injectPlayer(AbilityBase owner, Player player) {
        CraftPlayer player1 = (CraftPlayer) player;
        if (!player.isValid()) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            final Pair<CraftPlayer, ChannelOutboundHandlerAdapter> pair = channelHandlers.get(player.getUniqueId());
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
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == owner.getPlayer().getEntityId()) {
                        ReflectionUtil.FieldUtil.setValue(packet, "c", ItemStack.a);
                    }
                } else if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == owner.getPlayer().getEntityId()) {
                        List<Item<?>> items = ReflectionUtil.FieldUtil.getValue(packet, "b");
                        if (items.size() != 0) {
                            Item<?> item = items.get(0);
                            if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
                                Item<Byte> byteItem = (Item<Byte>) item;
                                byteItem.a((byte) (byteItem.b() | 1 << 5));
                                ((CraftPlayer) owner.getPlayer()).getHandle().setInvisible(true);
                            }
                        }
                    }
                }
                super.write(ctx, packet, promise);
            }
        };
        channelHandlers.put(player.getUniqueId(), Pair.of(player1, handler));
        player1.getHandle().playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", hashCode() + ":" + player.getName(), handler);
    }
}
