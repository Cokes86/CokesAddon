package com.cokes86.cokesaddon.util.timer;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class CombatTimer extends AbilityBase.AbilityTimer {

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
    }

    public boolean isCombat() {
        return combat;
    }

    public int getCombatTime() {
        return combatTime;
    }

    public int getNonCombatTime() {
        return nonCombatTime;
    }

    @Override
    protected void run(int count) {

        if (combat) {
            if (--combatTime <= 0) {
                combat = false;
                nonCombatTime = 0;
            }
        } else {
            nonCombatTime++;
        }

        if (channel != null) {
            if (combat) {
                channel.update(
                        "§c전투 §7(" +
                                String.format("%.1f", combatTime / 20.0) +
                                "초)"
                );
            } else {
                channel.update(
                        "§a비전투 §7(" +
                                String.format("%.1f", nonCombatTime / 20.0) +
                                "초)"
                );
            }
        }
    }
}