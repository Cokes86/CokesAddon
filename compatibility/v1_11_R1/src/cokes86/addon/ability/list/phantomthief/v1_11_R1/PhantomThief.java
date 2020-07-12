package cokes86.addon.ability.list.phantomthief.v1_11_R1;

import cokes86.addon.ability.list.phantomthief.NullAbility;
import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.LocationPlusUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.compat.nms.NMSHandler;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil.FieldUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_11_R1.*;
import net.minecraft.server.v1_11_R1.DataWatcher.Item;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@AbilityManifest(name = "팬텀 시프", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 좌클릭시 가장 멀리있는 플레이어의 등 뒤 10칸으로 워프 후, 팬텀 모드가 10초 지속됩니다.",
        "팬텀 모드 동안에는 투명화, 갑옷 삭제효과를 받는 대신, 공격할 수 없고 공격받을 수 없습니다.", "팬텀 모드 동안 대상에게 철괴로 우클릭시 팬텀 모드가 즉시 종료되고,",
        "발광효과를 15초동안 받습니다. 이때 대상은 이 사실을 알 수 있습니다.", "발광효과동안 대상에게 공격을 받지 않았을 경우 해당 플레이어의 능력을 훔치고,",
        "대상은 30초 뒤 팬텀시프로 능력이 바뀝니다.", "반대로 공격을 받았을 경우 1초간 스턴상태가 되며 모두에게 자신의 능력이 공개됩니다.", "※능력 아이디어: RainStar_" })
public class PhantomThief extends AbilityBase implements ActiveHandler, TargetHandler {
    AbstractGame.Participant target;
    private final Map<UUID, ChannelOutboundHandlerAdapter> channelHandlers = new HashMap<>();
    CooldownTimer c = new CooldownTimer(cool.getValue());
    private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;

    public PhantomThief(AbstractGame.Participant participant) {
        super(participant);
    }

    private static final Config<Integer> cool = new Config<Integer>(PhantomThief.class, "쿨타임", 90, 1) {
        @Override
        public boolean Condition(Integer value) {
            return value >= 0;
        }
    };

    static {
        AbilityFactory.registerAbility(NullAbility.class);
        try {
            BYTE_DATA_WATCHER_OBJECT = ReflectionUtil.FieldUtil.getStaticValue(Entity.class, "Z");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    DurationTimer phantom_1 = new DurationTimer(15) {
        public void onDurationStart() {
            hide();
        }

        @Override
        protected void onDurationProcess(int i) {}

        public void onDurationSilentEnd() {
            show();
        }

        public void onDurationEnd() {
            onSilentEnd();
            c.start();
        }
    };

    DurationTimer phantom_2 = new DurationTimer(10) {

        @Override
        protected void onDurationProcess(int arg0) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
            switch (arg0) {
                case 10:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.C));
                    break;
                case 9:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.D));
                    break;
                case 8:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.E));
                    break;
                case 7:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.F));
                    break;
                case 6:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.G));
                    break;
                case 5:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.A));
                    break;
                case 4:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.B));
                    break;
                case 3:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.C));
                    break;
                case 2:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.D));
                    break;
                case 1:
                    SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.E));
                    break;
            }
        }

        protected void onDurationSilentEnd() {
            getPlayer().removePotionEffect(PotionEffectType.GLOWING);
        }

        protected void onDurationEnd() {
            getPlayer().removePotionEffect(PotionEffectType.GLOWING);
            try {
                AbstractGame.Participant targ = target;
                if (targ != null && targ.hasAbility() && getParticipant().hasAbility()) {
                    if (targ.getAbility().getClass() == Mix.class && getParticipant().getAbility().getClass() == Mix.class) {
                        Mix mix = (Mix) getParticipant().getAbility();
                        if (mix.hasAbility()) {
                            boolean first;
                            Class<? extends AbilityBase> continued;
                            if (mix.hasSynergy()) {
                                Pair<AbilityFactory.AbilityRegistration, AbilityFactory.AbilityRegistration> pair = SynergyFactory.getSynergyBase(mix.getSynergy().getRegistration());
                                AbilityFactory.AbilityRegistration stealed;
                                if (mix.getFirst().getClass() == PhantomThief.class) {
                                    stealed = pair.getLeft();
                                    continued = pair.getRight().getAbilityClass();
                                    mix.setAbility(stealed.getAbilityClass(), mix.getSecond().getClass());
                                    ((Mix) targ.getAbility()).setAbility(NullAbility.class, continued);
                                    first = true;
                                } else {
                                    stealed = pair.getRight();
                                    continued = pair.getLeft().getAbilityClass();
                                    mix.setAbility(mix.getFirst().getClass(), stealed.getAbilityClass());
                                    ((Mix) targ.getAbility()).setAbility(continued, NullAbility.class);
                                    first = false;
                                }
                                getPlayer().sendMessage("능력을 훔쳤습니다. => " + stealed.getManifest().name());
                            } else {
                                if (mix.getFirst().getClass() == PhantomThief.class) {
                                    mix.setAbility(((Mix) targ.getAbility()).getFirst().getClass(), mix.getSecond().getClass());
                                    getPlayer().sendMessage("능력을 훔쳤습니다. => " + ((Mix) targ.getAbility()).getFirst().getName());
                                    ((Mix) targ.getAbility()).setAbility(NullAbility.class,
                                            ((Mix) targ.getAbility()).getSecond().getClass());
                                    continued = ((Mix) targ.getAbility()).getSecond().getClass();
                                    first = true;
                                } else {
                                    mix.setAbility(mix.getFirst().getClass(), ((Mix) targ.getAbility()).getSecond().getClass());
                                    getPlayer().sendMessage("능력을 훔쳤습니다. => " + ((Mix) targ.getAbility()).getSecond().getName());
                                    ((Mix) targ.getAbility()).setAbility(
                                            ((Mix) targ.getAbility()).getFirst().getClass(), NullAbility.class);
                                    continued = ((Mix) targ.getAbility()).getFirst().getClass();
                                    first = false;
                                }
                            }
                            new PhantomThiefTimer(targ, first, continued);
                            target.getPlayer().sendMessage("팬텀시프가 당신의 능력을 훔쳤습니다. 30초뒤 자신의 능력 중 하나가 팬텀시프로 바뀝니다.");
                        }
                    } else {
                        getParticipant().removeAbility();
                        getParticipant().setAbility(targ.getAbility().getClass());
                        getPlayer().sendMessage("능력을 훔쳤습니다. => " + targ.getAbility().getName());

                        targ.setAbility(NullAbility.class);
                        target.getPlayer().sendMessage("팬텀시프가 당신의 능력을 훔쳤습니다. 30초뒤 자신의 능력이 팬텀시프로 바뀝니다.");
                        new PhantomThiefTimer(targ);
                    }
                } else {
                    getPlayer().sendMessage("이런! 상대방이 능력이 없네요. 다시 시도해봐요~!");
                }
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    };

    @SubscribeEvent
    private void onJoin(PlayerJoinEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        injectPlayer(player);
    }

    @SubscribeEvent
    private void onQuit(PlayerQuitEvent e) {
        final CraftPlayer player = (CraftPlayer) e.getPlayer();
        if (player.equals(getPlayer())) return;
        if (channelHandlers.containsKey(player.getUniqueId())) {
            player.getHandle().playerConnection.networkManager.channel.pipeline().remove(channelHandlers.get(player.getUniqueId()));
            channelHandlers.remove(player.getUniqueId());
        }
    }

    private void show() {
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

    private void hide() {
        getParticipant().attributes().TARGETABLE.setValue(false);
        final CraftPlayer craftPlayer = (CraftPlayer) getPlayer();
        craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(10, DataWatcherRegistry.b), 0);
        craftPlayer.getHandle().setInvisible(true);
        final PacketPlayOutEntityEquipment[] packets = {
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.MAINHAND, ItemStack.a),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.OFFHAND, ItemStack.a),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.HEAD, ItemStack.a),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.CHEST, ItemStack.a),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.LEGS, ItemStack.a),
                new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), EnumItemSlot.FEET, ItemStack.a)
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
                    if ((int) FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        FieldUtil.setValue(packet, "c", ItemStack.a);
                    }
                } else if (packet instanceof PacketPlayOutEntityMetadata) {
                    if ((int) FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
                        List<Item<?>> items = FieldUtil.getValue(packet, "b");
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

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && phantom_1.isRunning()) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        if (e.getDamager().equals(getPlayer()) && phantom_1.isRunning()) {
            e.setCancelled(true);
        }

        if (e.getEntity().equals(getPlayer()) && phantom_2.isRunning()) {
            if (e.getDamager().equals(target.getPlayer()) || (e.getDamager() instanceof Projectile
                    && ((Projectile) e.getDamager()).getShooter().equals(target.getPlayer()))) {
                target = null;
                phantom_2.stop(true);
                getPlayer().removePotionEffect(PotionEffectType.GLOWING);
                Stun.apply(getParticipant(), TimeUnit.SECONDS, 1);
                Bukkit.broadcastMessage(getPlayer().getName() + "님의 능력은 §e팬텀시프§f입니다.");
                c.start();
                c.setCount(c.getCount() / 2);
            }
        }
    }

    @Override
    public boolean ActiveSkill(Material arg0, ClickType arg1) {
        if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK) && !phantom_1.isRunning()
                && !phantom_2.isRunning() && !c.isCooldown()) {
            Predicate<org.bukkit.entity.Entity> predicate = LocationPlusUtil.HAVE_ABILITY().and(LocationPlusUtil.STRICT(getParticipant()));
            Player p = LocationPlusUtil.getFarthestEntity(Player.class, getPlayer().getLocation(), predicate);

            if (p != null && getGame().isParticipating(p)) {
                this.target = getGame().getParticipant(p);
                Location location = p.getLocation().clone();
                float playerYaw = location.getYaw();

                double radYaw = Math.toRadians(playerYaw + 180);

                double x = -Math.sin(radYaw);
                double z = Math.cos(radYaw);
                Vector velocity = new Vector(x, 0, z);
                velocity.normalize().multiply(10);

                Location after = p.getLocation().add(velocity);
                after.setY(LocationUtil.getFloorYAt(after.getWorld(), location.getY(), after.getBlockX(), after.getBlockZ()) + 0.1);
                getPlayer().teleport(after);

                getPlayer().sendMessage("§e"+p.getName()+"§f님이 목표입니다. 해당 플레이어에게 다가가 철괴로 우클릭하세요.");

                return phantom_1.start();
            } else {
                getPlayer().sendMessage("조건에 맞는 가장 먼 플레이어가 존재하지 않습니다.");
            }
        }
        return false;
    }

    @Override
    public void TargetSkill(Material arg0, LivingEntity arg1) {
        if (arg0.equals(Material.IRON_INGOT) && arg1.equals(target.getPlayer()) && phantom_1.isRunning()) {
            phantom_1.stop(true);
            phantom_2.start();
            getPlayer().sendMessage("능력 훔치기를 시도합니다!");
            NMSHandler.getNMS().sendTitle(target.getPlayer(),"경  고", "팬텀시프가 당신의 능력을 훔칠려 합니다.", 10, 30, 10);
        }
    }

    class PhantomThiefTimer extends Timer {
        final AbstractGame.Participant target;
        final AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac;
        final boolean mix;
        private boolean first;
        private Class<? extends AbilityBase> continued;

        public PhantomThiefTimer(AbstractGame.Participant target) {
            super(30);
            this.target = target;
            this.ac = target.actionbar().newChannel();
            this.mix = false;
            this.start();
        }

        public PhantomThiefTimer(AbstractGame.Participant target, boolean first, Class<? extends AbilityBase> continued) {
            super(30);
            this.target = target;
            this.ac = target.actionbar().newChannel();
            this.mix = true;
            this.first = first;
            this.continued = continued;
            this.start();
        }

        @Override
        protected void run(int arg0) {
            ac.update("팬텀시프가 되기까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 전");
        }

        @Override
        protected void onEnd() {
            try {
                if (mix) {
                    Mix mix = (Mix) target.getAbility();
                    if (first) {
                        mix.setAbility(PhantomThief.class, continued);
                    } else {
                        mix.setAbility(continued, PhantomThief.class);
                    }
                } else {
                    target.setAbility(PhantomThief.class);
                }
                target.getPlayer().sendMessage("당신의 능력이 팬텀시프가 되었습니다 /aw check");
                ac.unregister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
