package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.util.*;
import com.cokes86.cokesaddon.util.timer.*;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.*;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "에너지 테이커", rank = Rank.A, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c테이킹§f: 받은 피해를 종류에 따라 저장합니다.",
        "  근거리 피해는 §a근거리 스택§f, 투사체 피해는 §e원거리 스택§f,",
        "  마법·독·위더·화염 피해는 §b마법 스택§f으로 저장됩니다.",
        "  각 스택은 받은 피해 2당 1씩 저장되며 최대 $[MAX_STACK]까지 저장됩니다.",
        "  비전투상태에서는 $[PERIOD]마다 모든 스택이 1씩 감소합니다.",
        "  §8명시되지 않은 피해는 스택으로 저장하지 않습니다. 전투상태는 7초간 유지됩니다.",
        "§7철괴 우클릭 §8- §b인핸스§f: §c테이킹§f으로 받은 스택에 비례해",
        "  $[DURATION] 동안 공격을 강화합니다. $[COOLDOWN]",
        "  지속시간동안 §c테이킹§f은 발동되지 않으며",
        "  종료 시 얻은 모든 스택이 제거됩니다.",
        "  [검/도끼] §a근거리 스택§f/2 + §b마법 스택§f/2",
        "  [활] §e원거리 스택§f/2 + §b마법 스택§f/2",
})
public class EnergyTaker extends CokesAbility implements ActiveHandler {
    private final ActionbarChannel channel = newActionbarChannel();

    private int melee = 0;
    private int distance = 0;
    private int magic = 0;

    private final Config<Integer> COOLDOWN = Config.of(EnergyTaker.class, "cooldown", 30, FunctionalInterfaces.COOLDOWN,
            "# 인핸스 쿨타임",
            "# 기본값: 30 (초)");

    private final Config<Integer> DURATION = Config.of(EnergyTaker.class, "duration", 15, FunctionalInterfaces.TIME,
            "# 인핸스 지속시간",
            "# 기본값: 15 (초)");

    private final Config<Integer> MAX_STACK = Config.positive(EnergyTaker.class, "max-stack", 8,
            "# 스택 당 최대치",
            "# 기본값: 8");
    private final Config<Integer> PERIOD = Config.positive(EnergyTaker.class, "period", 5,
            "# 비전투 도중 스택이 없어지는 주기",
            "# 기본값: 5 (초)");

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final Duration duration = new Duration(DURATION.getValue(), cooldown) {
        @Override
        protected void onDurationProcess(int i) {}

        @Override
        protected void onDurationEnd() {
            melee = 0;
            distance = 0;
            magic = 0;
            updateActionbar();
        }

        @Override
        protected void onDurationSilentEnd() {
            onDurationEnd();
        }
    };

    private final CombatTimer combatTimer = new CombatTimer(this, 140, true) {
        @Override
        public void combatAction(int count) {}

        @Override
        public void nonCombatAction(int count) {
            if (count % (PERIOD.getValue()*20) == 0) {
                melee = (int) Math.max(0.0, melee-1);
                distance = (int) Math.max(0.0, distance-1);
                magic = (int) Math.max(0.0, magic-1);
                updateActionbar();
            }
        }
    };

    public EnergyTaker(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent(eventPriority = EventPriority.MONITOR)
    public void damageCheck(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (e.getEntity().equals(getPlayer())) {
            combatTimer.engage();
            if (duration.isRunning()) return;
            switch (e.getCause()) {
                case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK: {
                    melee += Math.min(MAX_STACK.getValue(), (int) (e.getFinalDamage()/2));
                    break;
                }
                case PROJECTILE: {
                    distance += Math.min(MAX_STACK.getValue(), (int) (e.getFinalDamage()/2));
                    break;
                }
                case POISON: case WITHER: case MAGIC: case FIRE: case FIRE_TICK: {
                    magic += Math.min(MAX_STACK.getValue(), (int) (e.getFinalDamage()/2));
                    break;
                }
            }
        }

        Entity damager = CokesUtil.getDamager(e.getDamager());
        if (damager.equals(getPlayer())) {
            combatTimer.engage();
        }
        updateActionbar();
    }

    @SubscribeEvent()
    public void damage(EntityDamageByEntityEvent e) {
        Entity damager = CokesUtil.getDamager(e.getDamager());
        if (damager.equals(getPlayer())) {
            if (duration.isRunning()) {
                switch (e.getCause()) {
                    case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK: {
                        e.setDamage(e.getDamage() + (double) melee/2 + (double) magic/2);
                        break;
                    }
                    case PROJECTILE: {
                        e.setDamage(e.getDamage() + (double) distance/2 + (double) magic/2);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (!cooldown.isCooldown() && !duration.isDuration()) {
                return duration.start();
            }
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            if (combatTimer.isPaused()) combatTimer.resume();
            else combatTimer.start();
            updateActionbar();
        } else {
            combatTimer.pause();
            updateActionbar();
        }
    }

    private void updateActionbar() {
        channel.update(String.format("§a%d §e%d §b%d", melee, distance, magic));
    }
}
