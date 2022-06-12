package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
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

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;

@AbilityManifest(name = "코크스", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL, explain = {
        "도박에 미쳐버린 개발자",
        "§7철괴 우클릭 §9- §c이펙트 맛 좀 봐라!§f: 무작위 대상에게 무작위 상태이상을 1 ~ $[EFFECT_DURATION]초로 부여합니다. $[RIGHT_COOL]",
        "  이 중 움직임과 관련된 상태이상은 1 ~ $[MOVEMENT_DURATION]초 부여합니다.",
        "§7철괴 좌클릭 §8- §c화려한 슬롯머신§f: §a슬롯머신 §f하나를 가동합니다. $[LEFT_COOL]",
        "  총 5개의 슬롯이 돌아가며, 각 슬롯마다 효과를 $[LEFT_DURATION]간 부여합니다.",
        "  2개 이상의 같은 슬롯일 경우, 이 효과로 버프 효과가 아닌 것은 최대 3회 중첩,",
        "  버프는 2단계까지만 중첩됩니다.",
        "  <C>: 주는 대미지 0.75 증가.",
        "  <O>: 받는 대미지 0.5 감소.",
        "  <K>: 회복량 0.125배 증가.",
        "  <E>: 재생 버프",
        "  <S>: 저항 버프"
})
public class Cokes extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RIGHT_COOL = Config.of(Cokes.class, "이펙트_쿨타임", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
    private static final Config<Integer> LEFT_COOL = Config.of(Cokes.class, "슬롯머신_쿨타임", 90, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
    private static final Config<Integer> LEFT_DURATION = Config.of(Cokes.class, "슬롯머신_지속시간", 10, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);

    private static final Config<Integer> EFFECT_DURATION = Config.of(Cokes.class, "이펙트_일반상태이상_지속시간", 15, FunctionalInterfaces.upper(1));
    private static final Config<Integer> MOVEMENT_DURATION = Config.of(Cokes.class, "이펙트_이동상태이상_지속시간", 5, FunctionalInterfaces.upper(1));

    private final Cooldown rightCool = new Cooldown(RIGHT_COOL.getValue(), "이펙트");
    private final Cooldown leftCool = new Cooldown(LEFT_COOL.getValue(), "슬롯머신");
    private final SlotMachineTimer slot = new SlotMachineTimer();

    public Cokes(AbstractGame.Participant arg0) {
        super(arg0);
    }

    private final Predicate<Entity> predicate = entity -> {
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                return !game.getDeathManager().isExcluded(entity.getUniqueId());
            }
        }
        return true;
    };

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !slot.isRunning() && !leftCool.isCooldown()) {
            slot.start();
        } else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !rightCool.isCooldown()) {
            final Random random = new Random();

            AbstractGame.Participant participant = random.pick(getGame().getParticipants().toArray(new AbstractGame.Participant[0]));
            if (!predicate.test(participant.getPlayer())) {
                return ActiveSkill(material, clickType);
            }
            EffectRegistry.EffectRegistration<?> registration = random.pick(EffectRegistry.values().toArray(new EffectRegistry.EffectRegistration[0]));
            int second = random.nextInt(EFFECT_DURATION.getValue())+1;
            if (registration.getEffectType().contains(EffectType.MOVEMENT_RESTRICTION) || registration.getEffectType().contains(EffectType.MOVEMENT_INTERRUPT)) {
                second = random.nextInt(MOVEMENT_DURATION.getValue())+1;
            }

            AbstractGame.Effect e = registration.apply(participant, TimeUnit.SECONDS, second);
            if (e == null) {
                return ActiveSkill(material, clickType);
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
            String get = random.pick(results.keySet().toArray(new String[0]));
            joiner.add(random.pick(chatColors)+"<"+get+">");
            results.put(get, results.get(get) + 1);
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
                getPlayer().sendMessage("화려하게 터진다!");
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
                                NMS.sendTitle(getPlayer(),"","§6<C> §c<O> §c<K> §6<E> <S>",0,20,0);
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
                getPlayer().sendMessage("주는 대미지 "+ Math.min(results.get("C"),3)*0.75 +" 증가");
            }
            if (Math.min(results.get("O"),3) != 0) {
                getPlayer().sendMessage("받는 대미지 "+ Math.min(results.get("O"),3)*0.5 +" 감소");
            }
            if (Math.min(results.get("K"),3) != 0) {
                getPlayer().sendMessage("회복량 "+ Math.min(results.get("K"),3)*0.125 +"배 증가");
            }
            if (Math.min(results.get("E"),2) != 0) {
                getPlayer().sendMessage("재생 "+ Math.min(results.get("E"),2) +" 부여");
                PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 20*15, Math.min(results.get("E"),2) -1, true);
            }
            if (Math.min(results.get("S"),2) != 0) {
                getPlayer().sendMessage("저항 "+ Math.min(results.get("S"),2) +" 부여");
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