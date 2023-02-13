package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

@AbilityManifest(name = "휘트니", rank = Rank.S, species = Species.HUMAN, explain = {
        "철괴 우클릭시 $[DURATION]간 유지되고 최대 6번 중첩가능한 버프를 부여합니다. $[COOLDOWN_ONE]",
        "중첩사용 시 기존 효과와 더불어 새로운 효과를 부여받습니다.",
        "지속시간이 끝나거나 6번 중첩 후 쿨타임은 $[COOLDOWN_TWO]로 적용합니다.",
        "* 1회 사용 시: 신속 1단계 부여",
        "* 2회 사용 시: $[RECOVERY_PERIOD]마다 체력 $[RECOVERY] 회복",
        "* 3회 사용 시: 상대방 공격 시 출혈 $[BLEEDING] 부여. §c쿨타임§8: §f2초",
        "* 4회 사용 시: 상대방 공격 시 주었던 최종 대미지의 $[VAMPIRE]% 회복.",
        "* 5회 사용 시: 상대방에게 주는 대미지 $[DAMAGE] 증가",
        "* 6회 사용 시: 상대방에게 받는 대미지 $[DEFENCE]% 감소",
}, summarize = {
        "철괴 우클릭 시 $[DURATION]간 유지되는 버프 획득. $[COOLDOWN_ONE]",
        "버프는 최대 6단계. 단계마다 누적 적용. 6단계 적용 후 쿨타임은 $[COOLDOWN_TWO]",
        "1단계: 신속 1단계 부여 | 2단계: $[RECOVERY_PERIOD]마다 체력 $[RECOVERY] 회복",
        "3단계: 상대방 공격 시 출혈 $[BLEEDING] 부여 | 4단계: 상대방 공격 시 주었던 최종 대미지의 $[VAMPIRE]% 회복.",
        "5단계: 상대방에게 주는 대미지 $[DAMAGE] 증가 | 6단계: 상대방에게 받는 대미지 $[DEFENCE]% 감소"
})
@Tips(difficulty = Difficulty.EASY, stats = @Stats(
    crowdControl = Level.ONE,
    survival = Level.SEVEN,
    offense = Level.FIVE,
    utility = Level.THREE,
    mobility = Level.SIX
), tip = {
    "해당 능력은 버프의 지속시간과 쿨타임이 같이 돌아갑니다.",
    "쿨타임이 끝나면 지속시간이 끝나기 전에 능력을 재사용해야합니다.",
    "항상 쿨타임과 지속시간을 꾸준히 확인하세요!"
})
public class Whitney extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> DURATION = Config.of(Whitney.class, "duration", 20, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 버프 지속시간",
            "# 기본값: 20 (초)");
    private static final Config<Integer> COOLDOWN_ONE = Config.of(Whitney.class, "cooldown-one", 15, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 기본 쿨타임",
            "# 기본값: 15 (초)");
    private static final Config<Integer> COOLDOWN_TWO = Config.of(Whitney.class, "cooldown-two", 120, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 지속 종료 시 혹은 6중첩 이후 쿨타임",
            "# 기본값: 120 (초)");
    private static final Config<Integer> RECOVERY_PERIOD = Config.of(Whitney.class, "recovery-period", 5, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 2중첩 회복 주기",
            "# 기본값: 5 (초)");
    private static final Config<Integer> RECOVERY = Config.of(Whitney.class, "recovery", 1, FunctionalInterfaces.positive(),
            "# 2중첩 주기당 회복량",
            "# 기본값: 1");
    private static final Config<Integer> BLEEDING = Config.of(Whitney.class, "bleeding", 2, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 3중첩 출혈 부여랑",
            "# 기본값: 2 (초)");
    private static final Config<Integer> DAMAGE = Config.of(Whitney.class, "damage", 3, FunctionalInterfaces.positive(),
            "# 5중첩 대미지 증가랑",
            "# 기본값: 3");

    private static final Config<Double> DEFENCE = Config.of(Whitney.class, "defence", 20.0,
            FunctionalInterfaces.<Double>positive().and(FunctionalInterfaces.lower(100.0)),
            "# 6중첩 대미지 감소랑",
            "# 기본값: 20.0 (%)");
    private static final Config<Double> VAMPIRE = Config.of(Whitney.class, "vampire", 15.0,
            FunctionalInterfaces.<Double>positive().and(FunctionalInterfaces.lower(100.0)),
            "# 4중첩 흡혈량",
            "# 기본값: 15.0 (%)");

    private final WhitneyBuffTimer timer = new WhitneyBuffTimer();
    private final Cooldown bleed_cooldown = new Cooldown(2, "출혈 부여");

    public Whitney(Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !timer.isCooldown()) {
            return timer.start();
        }
        return false;
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && timer.getStack() == 6) {
            e.setDamage(e.getDamage() * (1 - DEFENCE.getValue()/100.0));
        }

        if (e.getDamager() == null) return;
        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity) {
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager.equals(getPlayer()) && e.getEntity() instanceof Player && !e.isCancelled()) {
            if (timer.getStack() >= 3 && !bleed_cooldown.isRunning()) {
                Bleed.apply(getGame(), (Player)e.getEntity(), TimeUnit.SECONDS, BLEEDING.getValue());
                bleed_cooldown.start();
            }
            if (timer.getStack() >= 5) {
                e.setDamage(e.getDamage() * (1 + DAMAGE.getValue()/100.0));
            }
            if (timer.getStack() >= 4) {
                final double finaldamage = e.getFinalDamage();
                double vampire_value = finaldamage * VAMPIRE.getValue() / 100.0;
                Healths.setHealth(getPlayer(), getPlayer().getHealth() + vampire_value);
            }
        }
    }

    private class WhitneyBuffTimer extends AbilityTimer {
        private int stack = 0;
        private final Cooldown cooldown_one = new Cooldown(COOLDOWN_ONE.getValue(), 50);
        private final Cooldown cooldown_two = new Cooldown(COOLDOWN_TWO.getValue(), 50);
        private final int duration;
        private final ActionbarChannel channel = newActionbarChannel();

        public WhitneyBuffTimer() {
            super((int) (DURATION.getValue() * 20 * (Wreck.isEnabled(Whitney.this.getGame()) ? Wreck.calculateDecreasedAmount(50) : 1)));
            this.duration = DURATION.getValue() * 20;
            register();
            if (Settings.getCooldownDecrease() == CooldownDecrease._100) {
                cooldown_one.setCooldown((int) (COOLDOWN_ONE.getValue() * 0.5));
                cooldown_two.setCooldown((int) (COOLDOWN_TWO.getValue() * 0.5));
                this.setMaximumCount((int) (DURATION.getValue() * 20 * 0.5));
            }
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void run(int count) {
            int k = 0;
            channel.update(ChatColor.GOLD.toString() + stack + "단계 버프 지속 시간 " + ChatColor.WHITE + ": " + ChatColor.YELLOW + TimeUtil.parseTimeAsString(getFixedCount()));

            if (stack >= 1) {
                PotionEffects.SPEED.addPotionEffect(getPlayer(), 10, 0, true);
            }

            if (stack >= 2) {
                if (++k % (20 * RECOVERY_PERIOD.getValue()) == 0) {
                    Healths.setHealth(getPlayer(), getPlayer().getHealth() + RECOVERY.getValue());
                }
            }
        }

        @Override
        protected void onEnd() {
            if (stack < 6) {
                SoundLib.ENTITY_VILLAGER_NO.playSound(getPlayer());
                cooldown_two.start();
            }
            stack = 0;
            channel.update(null);

        }

        @Override
        protected void onSilentEnd() {
            if (stack < 6) {
                SoundLib.ENTITY_VILLAGER_NO.playSound(getPlayer());
                cooldown_two.start();
            }
            stack = 0;
            channel.update(null);
        }

        public boolean isCooldown(){
            return cooldown_one.isCooldown() || cooldown_two.isCooldown();
        }

        public boolean start() {
            boolean isStart = false;
            if (!isCooldown()) {
                stack += 1;
                SoundLib.ENTITY_FOX_TELEPORT.playSound(getPlayer(), 1.0f, 2.0f);
                if (stack == 1) {
                    isStart = cooldown_one.start() && super.start();
                } else if (stack == 6) {
                    this.setCount(duration);
                    isStart = cooldown_two.start();
                } else if (stack > 6) {
                    stack = 6;
                    getPlayer().sendMessage("§c[!]§f 휘트니의 버프 스택은 최대 6단계까지만 가능합니다.");
                } else {
                    this.setCount(duration);
                    isStart = cooldown_one.start();
                }
            }
            return isStart;
        }

        public int getStack() {
            return stack;
        }
    }
}
