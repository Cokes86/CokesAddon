package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import org.jetbrains.annotations.NotNull;

public abstract class RouletteTimerEffect implements RouletteEffect {
    @Override
    public void apply(Participant... target) {
        new RouletteTimer(target[0], TaskType.INFINITE, -1).start();
    }

    public abstract void apply(TaskType taskType, int maximumCount, Participant... target);

    @Override
    public int requireTarget() {
        return 1;
    }

    public void onStart(){}

    public void run(int count){}

    public void onEnd(){}

    public void onSilentEnd(){}

    private class RouletteTimer extends GameTimer {
        public RouletteTimer(Participant participant, @NotNull TaskType taskType, int maximumCount) {
            participant.getGame().super(taskType, maximumCount);
        }

        @Override
        protected void onStart() {
            RouletteTimerEffect.this.onStart();
        }

        @Override
        protected void run(int count) {
            RouletteTimerEffect.this.run(count);
        }

        @Override
        protected void onEnd() {
            RouletteTimerEffect.this.onEnd();
        }

        @Override
        protected void onSilentEnd() {
            RouletteTimerEffect.this.onSilentEnd();
        }
    }

    public @interface RouletteTimerManifest {
        TaskType taskType();
        int maximumCount();
    }
}
