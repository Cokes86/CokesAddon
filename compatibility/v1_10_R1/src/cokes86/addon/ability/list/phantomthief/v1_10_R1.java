package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_10_R1.*;
import net.minecraft.server.v1_10_R1.DataWatcher.Item;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_10_R1.CraftServer;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AbilityManifest(name = "팬텀 시프", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 좌클릭시 가장 멀리있는 플레이어의 등 뒤 10칸으로 워프 후, 팬텀 모드가 10초 지속됩니다.",
        "팬텀 모드 동안에는 투명화, 갑옷 삭제효과를 받는 대신, 공격할 수 없고 공격받을 수 없습니다.", "팬텀 모드 동안 대상에게 철괴로 우클릭시 팬텀 모드가 즉시 종료되고,",
        "발광효과를 15초동안 받습니다. 이때 대상은 이 사실을 알 수 있습니다.", "발광효과동안 대상에게 공격을 받지 않았을 경우 해당 플레이어의 능력을 훔치고,",
        "대상은 30초 뒤 팬텀시프로 능력이 바뀝니다.", "반대로 공격을 받았을 경우 1초간 스턴상태가 되며 모두에게 자신의 능력이 공개됩니다.", "※능력 아이디어: RainStar_" })
public class v1_10_R1 extends PhantomThief {
    private final Map<UUID, ChannelOutboundHandlerAdapter> channelHandlers = new HashMap<>();
    private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
    private static final ItemStack NULL_ITEMSTACK = new ItemStack((net.minecraft.server.v1_10_R1.Item) null);

    public v1_10_R1(AbstractGame.Participant participant) {
        super(participant, v1_10_R1.class);
    }

    static {
        try {
            BYTE_DATA_WATCHER_OBJECT = ReflectionUtil.FieldUtil.getStaticValue(Entity.class, "aa");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onJoin(PlayerJoinEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        injectPlayer(player);
    }

    @SubscribeEvent
    public void onQuit(PlayerQuitEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            player.getHandle().playerConnection.networkManager.channel.pipeline().remove(channelHandlers.get(player.getUniqueId()));
            channelHandlers.remove(player.getUniqueId());
        }
    }

    public void show() {
        getParticipant().attributes().TARGETABLE.setValue(true);
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInMainHand())),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInOffHand())),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(getPlayer().getInventory().getHelmet())),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(getPlayer().getInventory().getChestplate())),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(getPlayer().getInventory().getLeggings())),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(getPlayer().getInventory().getBoots()))
        };
        for (Map.Entry<UUID, ChannelOutboundHandlerAdapter> entry : channelHandlers.entrySet()) {
            CraftPlayer player = (CraftPlayer) Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.getHandle().playerConnection.networkManager.channel.pipeline().remove(entry.getValue());
                for (PacketPlayOutEntityEquipment packet : packets) {
                    player.getHandle().playerConnection.sendPacket(packet);
                }
            }
        }
        channelHandlers.clear();
        new BukkitRunnable() {
            @Override
            public void run() {
                ((CraftPlayer) getPlayer()).getHandle().setInvisible(false);
            }
        }.runTaskLater(AbilityWar.getPlugin(), 2L);
    }

    public void hide() {
        getParticipant().attributes().TARGETABLE.setValue(false);
        final CraftPlayer craftPlayer = (CraftPlayer) getPlayer();
        craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(9, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().setInvisible(true);
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.MAINHAND, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.OFFHAND, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.HEAD, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.CHEST, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.LEGS, NULL_ITEMSTACK),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.FEET, NULL_ITEMSTACK)
        };
        for (CraftPlayer player : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
            if (player.equals(getPlayer())) continue;
            for (PacketPlayOutEntityEquipment packet : packets) {
                player.getHandle().playerConnection.sendPacket(packet);
            }
            injectPlayer(player);
        }
    }

    private void injectPlayer(CraftPlayer player) {
        final ChannelOutboundHandlerAdapter handler = new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
                if (packet instanceof PacketPlayOutEntityEquipment) {
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        ReflectionUtil.FieldUtil.setValue(packet, "c", NULL_ITEMSTACK);
                    }
                } else if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) ReflectionUtil.FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        List<Item> items = ReflectionUtil.FieldUtil.getValue(packet, "b");
                        if (items.size() != 0) {
                            Item<?> item = items.get(0);
                            if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
                                Item<Byte> byteItem = (Item<Byte>) item;
                                byteItem.a((byte) (byteItem.b() | 1 << 5));
                                ((CraftPlayer) getPlayer()).getHandle().setInvisible(true);
                            }
                        }
                    }
                }
                super.write(ctx, packet, promise);
            }
        };
        channelHandlers.put(player.getUniqueId(), handler);
        player.getHandle().playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", hashCode() + ":" + player.getName(), handler);
    }
}
