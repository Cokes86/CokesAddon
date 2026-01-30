package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.effect.list.Debuging;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.timer.HitHologramTimer;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.Observer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;

@AbilityManifest(name = "코크스", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL, explain = {
        "§7패시브 §8- §a커스터마이징§f: 능력 효과가 자신에게 이롭게 나올 확률이 증가합니다.",
        "§7패시브 §8- §c랜더마이즈§f: 대미지가 0% ~ $[RANDOMIZE_MAXIMUM]% 사이로 랜덤하게 조정됩니다.",
        "§7철괴 우클릭 §8- §c이펙트 맛 좀 봐라!§f: 무작위 대상에게 무작위 상태이상을",
        "  1 ~ $[EFFECT_DURATION]초(이동계는 1 ~ $[MOVEMENT_DURATION]초) 부여합니다. $[RIGHT_COOL]",
        "  해당 능력에서 특수 상태이상 §4§n디버깅§f이 등장합니다.",
        "  §4§n디버깅§f은 1 ~ $[DEBUGING_DURATION]초 부여합니다.",
        "§7철괴 좌클릭 §8- §c화려한 슬롯머신§f: §a슬롯머신 §f하나를 가동합니다. $[LEFT_COOL]",
        "  총 5개의 슬롯이 돌아가며, 각 슬롯마다 효과를 $[LEFT_DURATION]간 부여합니다.",
        "  §8<C>: 주는 대미지 0.75 증가. | <O>: 받는 대미지 0.5 감소.",
        "  §8<K>: 회복량 0.125배 증가. | <E>: 재생 버프 | <S>: 저항 버프",
        "§c[§4디버깅§c]§f 액티브 스킬을 사용할 시, 해당 상태이상은 기절로 변합니다."
})
public class Cokes extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RIGHT_COOL = Config.cooldown(Cokes.class, "effect-cooldown", 60,
            "이펙트 맛 좀 봐라 쿨타임",
            "기본값 : 60 (초)");
    private static final Config<Integer> LEFT_COOL = Config.cooldown(Cokes.class, "slot-cooldown", 75,
            "화려한 슬롯머신 쿨타임",
            "기본값 : 75 (초)");
    private static final Config<Integer> LEFT_DURATION = Config.cooldown(Cokes.class, "slot-duration", 10,
            "화려한 슬롯머신 지속시간",
            "기본값 : 10 (초)");

    private static final Config<Integer> EFFECT_DURATION = Config.time(Cokes.class, "normal-effect-duration", 15,
            "이펙트 맛 좀 봐라 중 일반 상태이상 최대 지속시간",
            "기본값 : 15 (초)");
    private static final Config<Integer> MOVEMENT_DURATION = Config.time(Cokes.class, "move-effect-duration", 5,
            "이펙트 맛 좀 봐라 중 이동 관련 상태이상 최대 지속시간",
            "기본값 : 5 (초)");
    private static final Config<Integer> DEBUGING_DURATION = Config.time(Cokes.class, "debuging-duration", 45,
            "이펙트 맛 좀 봐라 중 디버깅 상태이상 최대 지속시간",
            "기본값 : 45 (초)");

    private static final Config<Double> RANDOMIZE_MAXIMUM = Config.of(Cokes.class, "randomize-maximum", 180.0,
            "랜더마이즈 최대 확률",
            "기본값 : 180 (%%)");

    private final Cooldown rightCool = new Cooldown(RIGHT_COOL.getValue(), "이펙트");
    private final Cooldown leftCool = new Cooldown(LEFT_COOL.getValue(), "슬롯머신");
    private final SlotMachineTimer slot = new SlotMachineTimer();

    public Cokes(AbstractGame.Participant arg0) {
        super(arg0);
    }

    private final Predicate<Entity> predicate = entity -> {
        if (entity == null) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())
                    || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                return false;
            }
            if (getGame() instanceof Teamable) {
                final Teamable teamGame = (Teamable) getGame();
                final AbstractGame.Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return true;
    };

    //랜더마이즈
    private double debugDamage = 1;
    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getDamager() != null && e.getDamager().equals(getPlayer())) {
            double temp = new Random().nextDouble() * RANDOMIZE_MAXIMUM.getValue()/100.0;
            if (temp < 1 && debugDamage < 1) {
                temp = new Random().nextDouble() * RANDOMIZE_MAXIMUM.getValue()/100.0;
            }
            debugDamage = temp;
            e.setDamage(e.getDamage() * debugDamage);
            HitHologramTimer timer = HitHologramTimer.create(this, e.getEntity().getLocation(), "a");
            timer.attachObserver(new Observer() {
                @Override
                public void run(int count) {
                    if (count <= 20) {
                        final DecimalFormat format = new DecimalFormat("000");
                        StringBuilder builder = new StringBuilder();
                        builder.append(format.format(debugDamage*100));
                        int index = count <= 7 ? 0 : (count <= 12 ? 1 : (count <= 19 ? 2 : 3));
                        builder.insert(index, "§k");
                        String color = debugDamage < 1 ? "§c" : "§a";
                        timer.setText(color+ "§l" + builder + color+"§l%");
                    }
                }
            });
            timer.start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !slot.isRunning() && !leftCool.isCooldown()) {
            slot.start();  //슬롯머신
        } else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !rightCool.isCooldown()) {
            //이펙트 맛 좀 봐라
            final Random random = new Random();

            AbstractGame.Participant participant = random.pick(getGame().getParticipants().toArray(new AbstractGame.Participant[0]));
            if (participant.equals(getParticipant())) {
                participant = random.pick(getGame().getParticipants().toArray(new AbstractGame.Participant[0]));
            }
            if (!predicate.test(participant.getPlayer())) {
                return ActiveSkill(material, clickType);
            }
            EffectRegistry.EffectRegistration<?> registration = random.pick(EffectRegistry.values().toArray(new EffectRegistry.EffectRegistration[0]));
            if (!registration.isTypeOf(Debuging.class)) {
                registration = random.pick(EffectRegistry.values().toArray(new EffectRegistry.EffectRegistration[0]));
            }

            int second = random.nextInt(EFFECT_DURATION.getValue())+1;
            if (registration.getEffectType().contains(EffectType.MOVEMENT_RESTRICTION) || registration.getEffectType().contains(EffectType.MOVEMENT_INTERRUPT)
                    || registration.getEffectType().contains(EffectType.SIGHT_RESTRICTION) || registration.getEffectType().contains(EffectType.SIGHT_CONTROL)) {
                second = random.nextInt(MOVEMENT_DURATION.getValue())+1;
            }
            if (registration.isTypeOf(Debuging.class)) {
                second = random.nextInt(DEBUGING_DURATION.getValue())+1;
            }

            AbstractGame.Effect e = registration.apply(participant, TimeUnit.SECONDS, second);
            if (e == null) {
                getPlayer().sendMessage("§c[!] 오류가 발생하여 이펙트를 부여하지 못했습니다. 다시 시도해주세요.");
                return false;
            }
            getPlayer().sendMessage(participant.getPlayer().getName()+"에게 "+registration.getManifest().displayName()+" §f"+second+"초 부여!");
            participant.getPlayer().sendMessage("[§b코크스§f] "+registration.getManifest().displayName()+"§f 효과를 "+second+"초 받습니다!");
            return rightCool.start();
        }
        return false;
    }

    private class SlotMachineTimer extends AbilityTimer implements Listener {
        private final ChatColor[] chatColors = {
                ChatColor.YELLOW,
                ChatColor.RED,
                ChatColor.GOLD,
                ChatColor.LIGHT_PURPLE,
                ChatColor.DARK_PURPLE,
                ChatColor.AQUA
        };
        private final Map<String, Integer> results = new HashMap<>();
        private final Random random = new Random();
        private final SlotDuration duration = new SlotDuration();

        public boolean isRunning() {
            return super.isRunning() || duration.isRunning();
        }

        public SlotMachineTimer() {
            super(TaskType.NORMAL,5);
            setPeriod(TimeUnit.TICKS, 10);
        }

        private StringJoiner joiner;

        public void onStart() {
            results.clear();
            results.put("C", 0);
            results.put("O", 0);
            results.put("K", 0);
            results.put("E", 0);
            results.put("S", 0);
            joiner = new StringJoiner("§f ");
        }

        public void run(int count) {
            String _get = random.pick(results.keySet().toArray(new String[0]));
            if (results.get(_get) > 0) {
                _get = random.pick(results.keySet().toArray(new String[0]));
            }
            joiner.add(random.pick(chatColors)+"<"+_get+">");
            results.put(_get, results.get(_get) + 1);
            NMS.sendTitle(getPlayer(),"",joiner.toString(),0,20,0);
            SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
        }

        public void onSilentEnd() {
            NMS.clearTitle(getPlayer());
        }

        public void onEnd() {
            duration.start();
            NMS.clearTitle(getPlayer());

            if (results.get("C") == 1 && results.get("O") == 1 && results.get("K") == 1 && results.get("E") == 1 && results.get("S") == 1) {
                getPlayer().sendMessage("[코크스] §a화려하게 터진다!");
                SoundLib.ENTITY_FIREWORK_ROCKET_BLAST.playSound(getPlayer());
                results.put("C", 3);
                results.put("O", 3);
                results.put("K", 3);
                results.put("E", 2);
                results.put("S", 2);

                new AbilityTimer(TaskType.NORMAL,5) {
                    public void run(int count) {
                        switch (count) {
                            case 1: {
                                NMS.sendTitle(getPlayer(),"","§c<C> §6<O> <K> <E> <S>",0,20,0);
                                break;
                            }
                            case 2: {
                                NMS.sendTitle(getPlayer(),"","§6<C> §c<O> §6<K> <E> <S>",0,20,0);
                                break;
                            }
                            case 3: {
                                NMS.sendTitle(getPlayer(),"","§6<C> <O> §c<K> §6<E> <S>",0,20,0);
                                break;
                            }
                            case 4: {
                                NMS.sendTitle(getPlayer(),"","§6<C> <O> <K> §c<E> §6<S>",0,20,0);
                                break;
                            }
                            case 5: {
                                NMS.sendTitle(getPlayer(),"","§6<C> <O> <K> <E> §c<S>",0,20,0);
                                break;
                            }
                        }
                    }

                    public void onSilentEnd() {
                        NMS.clearTitle(getPlayer());
                    }

                    public void onEnd() {
                        onSilentEnd();
                    }
                }.setPeriod(TimeUnit.TICKS,4).start();
            }

            if (Math.min(results.get("C"),3) != 0) {
                getPlayer().sendMessage("[코크스] 주는 대미지 "+ Math.min(results.get("C"),3)*0.75 +" 증가");
            }
            if (Math.min(results.get("O"),3) != 0) {
                getPlayer().sendMessage("[코크스] 받는 대미지 "+ Math.min(results.get("O"),3)*0.5 +" 감소");
            }
            if (Math.min(results.get("K"),3) != 0) {
                getPlayer().sendMessage("[코크스] 회복량 "+ Math.min(results.get("K"),3)*0.125 +"배 증가");
            }
            if (Math.min(results.get("E"),2) != 0) {
                getPlayer().sendMessage("[코크스] 재생 "+ Math.min(results.get("E"),2) +" 부여");
                PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 20*15, Math.min(results.get("E"),2) -1, true);
            }
            if (Math.min(results.get("S"),2) != 0) {
                getPlayer().sendMessage("[코크스] 저항 "+ Math.min(results.get("S"),2) +" 부여");
                PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 20*15, Math.min(results.get("S"),2) -1, true);
            }

            SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
        }

        class SlotDuration extends Duration {

            public SlotDuration() {
                super(LEFT_DURATION.getValue(), leftCool, "슬롯머신");
            }

            public void onDurationStart() {
                Bukkit.getPluginManager().registerEvents(SlotMachineTimer.this, AbilityWar.getPlugin());
            }

            @Override
            protected void onDurationProcess(int i) { }

            public void onDurationEnd() {
                HandlerList.unregisterAll(SlotMachineTimer.this);
            }

            public void onDurationSilentEnd() {
                HandlerList.unregisterAll(SlotMachineTimer.this);
            }
        }

        @EventHandler
        public void onEntityDamage(CEntityDamageEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setDamage(e.getDamage()-Math.min(results.get("O"),3)*0.5);
            }

            Entity attacker = e.getDamager();
            if (attacker != null) {
                if (NMS.isArrow(attacker)) {
                    Projectile arrow = (Projectile) attacker;
                    if (arrow.getShooter() instanceof Entity) {
                        attacker = (Entity) arrow.getShooter();
                    }
                }

                if (attacker.equals(getPlayer())) {
                    e.setDamage(e.getDamage() + Math.min(results.get("C"),3)*0.75);
                }
            }
        }

        @EventHandler
        public void onEntityRegainHealth(EntityRegainHealthEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setAmount(e.getAmount()*(1 + Math.min(results.get("K"),3)*0.125));
            }
        }
    }
}