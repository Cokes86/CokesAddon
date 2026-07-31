package com.cokes86.cokesaddon.util.timer;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public abstract class CombatTimer extends AbilityBase.AbilityTimer {

    private final int combatDuration;
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel;

    private boolean combat = false;

    private int combatTime = 0;
    private int nonCombatTime = 0;

    public CombatTimer(
            @NotNull AbilityBase ability,
            int combatDuration,
            boolean noticeActionBar
    ) {
        ability.super(TaskType.INFINITE, -1);

        this.combatDuration = combatDuration;
        this.channel = noticeActionBar ? ability.newActionbarChannel() : null;

        setPeriod(TimeUnit.TICKS, 1);
    }

    public void engage() {
        combat = true;
        combatTime = combatDuration;
        nonCombatTime = 0;
    }

    public boolean isCombat() {
        return combat;
    }

    @Override
    protected void run(int count) {

        if (combat) {
            combatTime--;
            combatAction(combatTime);
            if (combatTime <= 0) {
                combat = false;
                nonCombatTime = 0;
            }
        } else {
            nonCombatTime++;
            nonCombatAction(nonCombatTime);
        }

        if (channel != null) {
            if (combat) {
                channel.update(String.format("§c전투 §7(%.1f초)", combatTime / 20.0));
            } else {
                channel.update(String.format("§a비전투 §7(%.1f초)", nonCombatTime / 20.0));
            }
        }
    }

    public abstract void combatAction(int count);
    public abstract void nonCombatAction(int count);
}