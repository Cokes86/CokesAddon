package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;

@AbilityManifest(name = "코크스", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL, explain = {
        "도박에 미쳐버린 개발자",
        "§7철괴 우클릭 §9- §c이펙트 맛 좀 봐라!§r: 무작위 대상에게 무작위 상태이상을 1 ~ 15초로 부여합니다. $[RIGHT_COOL]",
        "§7철괴 좌클릭 §8- §c화려한 슬롯머신§r: §a슬롯머신 §r하나를 가동합니다. $[LEFT_COOL]",
        "  총 5개의 슬롯이 돌아가며, 각 슬롯마다 효과를 $[LEFT_DURATION]간 얻습니다.",
        "  2개 이상의 같은 슬롯일 경우, 이 효과는 최대 3번 합적용됩니다.",
        "  <C>: 주는 대미지 0.75 상승.",
        "  <O>: 받는 대미지 0.75 감소.",
        "  <K>: 회복량 0.25배 증가.",
        "  <E>: 재생 버프",
        "  <S>: 저항 버프"
})
public class Cokes extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RIGHT_COOL = new Config<>(Cokes.class, "이펙트_쿨타임", 60, Config.Condition.COOLDOWN),
    LEFT_COOL = new Config<>(Cokes.class, "슬롯머신_쿨타임", 30, Config.Condition.COOLDOWN),
    LEFT_DURATION = new Config<>(Cokes.class, "슬롯머신_지속시간", 10, Config.Condition.TIME);

    private final Cooldown rightCool = new Cooldown(RIGHT_COOL.getValue(), "이펙트"),
            leftCool = new Cooldown(LEFT_COOL.getValue(), "슬롯머신");
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
            int second = random.nextInt(15)+1;
            AbstractGame.Effect e = registration.apply(participant, TimeUnit.SECONDS, second);
            if (e == null) {
                return ActiveSkill(material, clickType);
            }
            getPlayer().sendMessage(participant.getPlayer().getName()+"에게 "+registration.getManifest().displayName()+" §r"+second+"초 부여!");
            participant.getPlayer().sendMessage("[§b코크스§r] "+registration.getManifest().displayName()+"§r 효과를 "+second+"초 받습니다!");
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
            joiner = new StringJoiner("§r ");
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
                results.put("E", 3);
                results.put("S", 3);

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
                getPlayer().sendMessage("받는 대미지 "+ Math.min(results.get("O"),3)*0.75 +" 감소");
            }
            if (Math.min(results.get("K"),3) != 0) {
                getPlayer().sendMessage("회복량 "+ Math.min(results.get("K"),3)*0.25 +"배 증가");
            }
            if (Math.min(results.get("E"),3) != 0) {
                getPlayer().sendMessage("재생 "+ Math.min(results.get("E"),3) +" 부여");
                PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 20*15, Math.min(results.get("E"),3) -1, true);
            }
            if (Math.min(results.get("S"),3) != 0) {
                getPlayer().sendMessage("저항 "+ Math.min(results.get("S"),3) +" 부여");
                PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 20*15, Math.min(results.get("S"),3) -1, true);
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
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setDamage(e.getDamage()-Math.min(results.get("O"),3)*0.75);
            }

            Entity attacker = e.getDamager();
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

        @EventHandler
        public void onEntityRegainHealth(EntityRegainHealthEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setAmount(e.getAmount()*(1 + Math.min(results.get("K"),3)*0.25));
            }
        }
    }
}