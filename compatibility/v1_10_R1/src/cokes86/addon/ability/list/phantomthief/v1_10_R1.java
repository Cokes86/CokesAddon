package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_10_R1.DataWatcher.Item;
import net.minecraft.server.v1_10_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_10_R1.CraftServer;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class v1_10_R1 implements IPhantom {
    private final Map<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> channelHandlers = new HashMap<>();
    private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
    private static final ItemStack NULL_ITEMSTACK = new ItemStack((net.minecraft.server.v1_10_R1.Item) null);

    static {
        try {
            BYTE_DATA_WATCHER_OBJECT = ReflectionUtil.FieldUtil.getStaticValue(Entity.class, "aa");
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
                for (PacketPlayOutEntityEquipment packet : new PacketPlayOutEntityEquipment[] {
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.MAINHAND, NULL_ITEMSTACK),
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.OFFHAND, NULL_ITEMSTACK),
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.HEAD, NULL_ITEMSTACK),
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.CHEST, NULL_ITEMSTACK),
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.LEGS, NULL_ITEMSTACK),
                        new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.FEET, NULL_ITEMSTACK)
                }) {
                    player.getHandle().playerConnection.sendPacket(packet);
                }
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
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getItemInMainHand())),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getItemInOffHand())),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getHelmet())),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getChestplate())),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getLeggings())),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(owner.getPlayer().getInventory().getBoots()))
        };
        for (Map.Entry<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> entry : channelHandlers.entrySet()) {
            final CraftPlayer player = entry.getValue().getLeft();
            try {
                player.getHandle().playerConnection.networkManager.channel.pipeline().remove(entry.getValue().getRight());
            } catch (NoSuchElementException ignored) {}
            if (!player.isValid()) continue;
            for (PacketPlayOutEntityEquipment packet : packets) {
                player.getHandle().playerConnection.sendPacket(packet);
            }
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
        craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(10, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().setInvisible(true);
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.MAINHAND, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.OFFHAND, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.HEAD, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.CHEST, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.LEGS, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(owner.getPlayer().getEntityId(), EnumItemSlot.FEET, NULL_ITEMSTACK)
        };
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            if (player.equals(owner.getPlayer())) continue;
            for (PacketPlayOutEntityEquipment packet : packets) {
                player.getHandle().playerConnection.sendPacket(packet);
            }
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
