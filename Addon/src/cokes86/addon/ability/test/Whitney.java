package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.CokesAbility.Config.Condition;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "휘트니", rank = Rank.S, species = Species.HUMAN, explain = {
        "철괴 우클릭시 $[DURATION]간 유지되고 최대 6번 중첩가능한 버프를 부여합니다. $[COOLDOWN_ONE]",
        "중첩사용 시 기존 효과와 더불어 새로운 효과를 부여받습니다.",
        "지속시간이 끝나거나 6번 중첩 후 쿨타임은 $[COOLDOWN_TWO]로 적용합니다.",
        "* 1회 사용 시: 신속 1단계 부여",
        "* 2회 사용 시: $[RECOVERY_PERIOD]마다 체력 $[RECOVERY] 회복",
        "* 3회 사용 시: 상대 공격 시 출혈 $[BLEEDING] 부여",
        "* 4회 사용 시: 상대 공격 시 주었던 최종 대미지의 $[VAMPIRE]% 회복.",
        "* 5회 사용 시: 상대에게 주는 대미지 $[DAMAGE] 증가",
        "* 6회 사용 시: 상대에게 받는 대미지 $[DEFENCE]% 감소",
})
public class Whitney extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> DURATION = new Config<>(Whitney.class, "duration", 20, Condition.TIME,
            "# 휘트니 버프 지속시간",
            "# 기본값: 20 (초)");
    private static final Config<Integer> COOLDOWN_ONE = new Config<>(Whitney.class, "cooldown-one", 15, Condition.COOLDOWN,
            "# 휘트니 기본 쿨타임",
            "# 기본값: 15 (초)");
    private static final Config<Integer> COOLDOWN_TWO = new Config<>(Whitney.class, "cooldown-two", 120, Condition.TIME,
            "# 휘트니 추가 쿨타임",
            "# 기본값: 120 (초)");
    private static final Config<Integer> RECOVERY_PERIOD = new Config<>(Whitney.class, "recovery-period", 5, Condition.TIME,
            "# 휘트니 2중첩 회복 주기",
            "# 기본값: 5 (초)");
    private static final Config<Integer> RECOVERY = new Config<>(Whitney.class, "recovery", 1, PredicateUnit.positive(),
            "# 휘트니 2중첩 주기당 회복량",
            "# 기본값: 1");
    private static final Config<Integer> BLEEDING = new Config<>(Whitney.class, "bleeding", 2, Condition.TIME,
            "# 휘트니 3중첩 출혈 부여랑",
            "# 기본값: 2 (초)");
    private static final Config<Integer> DAMAGE = new Config<>(Whitney.class, "damage", 3, PredicateUnit.positive(),
            "# 휘트니 5중첩 대미지 증가랑",
            "# 기본값: 3");

    private static final Config<Double> DEFENCE = new Config<>(Whitney.class, "defence", 20.0, PredicateUnit.between(0.0, 100.0, false),
            "# 휘트니 6중첩 대미지 감소랑",
            "# 기본값: 20.0 (%)");
    private static final Config<Double> VAMPIRE = new Config<>(Whitney.class, "vampire", 15.0, PredicateUnit.between(0.0, 100.0, true),
            "# 휘트니 4중첩 흡혈량",
            "# 기본값: 15.0 (%)");

    private final WhitneyBuffTimer timer = new WhitneyBuffTimer();

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
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && timer.getStack() == 6) {
            e.setDamage(e.getDamage() * (1 - DEFENCE.getValue()/100.0));
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity) {
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager.equals(getPlayer()) && e.getEntity() instanceof Player) {
            if (timer.getStack() >= 3) {
                Bleed.apply(getGame(), (Player)e.getEntity(), TimeUnit.SECONDS, BLEEDING.getValue());
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
        private final Cooldown cooldown_one = new Cooldown(COOLDOWN_ONE.getValue(), CooldownDecrease._25);
        private final Cooldown cooldown_two = new Cooldown(COOLDOWN_TWO.getValue(), CooldownDecrease._25);
        private final int duration;
        private final ActionbarChannel channel = newActionbarChannel();

        public WhitneyBuffTimer() {
            super(DURATION.getValue() * 20);
            this.duration = DURATION.getValue() * 20;
            register();
        }

        @Override
        protected void run(int count) {
            channel.update(ChatColor.GOLD + "지속 시간 " + ChatColor.WHITE + ": " + ChatColor.YELLOW + TimeUtil.parseTimeAsString(getFixedCount()));

            if (stack >= 1) {
                PotionEffects.SPEED.addPotionEffect(getPlayer(), 10, 0, true);
            }

            if (stack >= 2 && count % (20 * RECOVERY_PERIOD.getValue()) == 0) {
                Healths.setHealth(getPlayer(), getPlayer().getHealth() + RECOVERY.getValue());
            }
        }

        @Override
        protected void onEnd() {
            if (stack < 6) {
                cooldown_two.start();
            }
            stack = 0;
        }

        @Override
        protected void onSilentEnd() {
            if (stack < 6) {
                cooldown_two.start();
            }
            stack = 0;
        }

        public boolean isCooldown(){
            return cooldown_one.isCooldown() || cooldown_two.isCooldown();
        }

        public boolean start() {
            boolean isStart = false;
            if (!isCooldown()) {
                stack += 1;
                SoundLib.ENTITY_ENDERMAN_TELEPORT.playSound(getPlayer());
                if (stack == 1) {
                    isStart = cooldown_one.start() && super.start();
                } else if (stack == 6) {
                    this.setCount(duration);
                    isStart = cooldown_two.start();
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
