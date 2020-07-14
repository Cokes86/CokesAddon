package cokes86.addon.ability.list.phantomthief;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.LocationPlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
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
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class PhantomThief extends AbilityBase implements ActiveHandler, TargetHandler {
    DurationTimer phantom_1 = new Phantom1(), phantom_2 = new Phantom2();
    CooldownTimer c;
    AbstractGame.Participant target;
    private final Class<? extends PhantomThief> phantomThief;

    static {
        AbilityFactory.registerAbility(NullAbility.class);
    }

    public PhantomThief(AbstractGame.Participant participant, Class<? extends PhantomThief> phantomThief) {
        super(participant);
        this.phantomThief = phantomThief;
        Config<Integer> cool = new CooldownConfig();
        this.c = new CooldownTimer(cool.getValue());
    }

    @Override
    public boolean ActiveSkill(Material arg0, ClickType arg1) {
        if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK) && !phantom_1.isRunning()
                && !phantom_2.isRunning() && !c.isCooldown()) {
            Predicate<Entity> predicate = LocationPlusUtil.HAVE_ABILITY().and(LocationPlusUtil.STRICT(getParticipant()));
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
                after.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(after.getWorld()), location.getY(), after.getBlockX(), after.getBlockZ()) + 0.1);
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
                    && Objects.equals(((Projectile) e.getDamager()).getShooter(), target.getPlayer()))) {
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

    public abstract void hide();
    public abstract void show();

    @SubscribeEvent
    public abstract void onJoin(PlayerJoinEvent e);

    @SubscribeEvent
    public abstract void onQuit(PlayerQuitEvent e);

    class Phantom1 extends DurationTimer {
        public Phantom1() {
            super(15);
        }

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
    }

    class Phantom2 extends DurationTimer {
        public Phantom2() {
            super(10);
        }

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
                        mix.setAbility(phantomThief, continued);
                    } else {
                        mix.setAbility(continued, phantomThief);
                    }
                } else {
                    target.setAbility(phantomThief);
                }
                target.getPlayer().sendMessage("당신의 능력이 팬텀시프가 되었습니다 /aw check");
                ac.unregister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class CooldownConfig extends Config<Integer> {

        public CooldownConfig() {
            super(phantomThief, "쿨타임", 90, 1);
        }

        @Override
        public boolean Condition(Integer value) {
            return false;
        }
    }
}
