package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public abstract class RouletteTimerEffect implements RouletteEffect {
    private TaskType taskType = TaskType.INFINITE;
    private int maximunCount = -1;
    private Pair<TimeUnit, Integer> period = Pair.of(TimeUnit.SECONDS, 1);

    @Override
    public void apply(Participant... target) {
        beforeStart(target);
        new RouletteTimer(target[0], taskType, maximunCount).setPeriod(period.getLeft(), period.getRight()).start();
    }

    public abstract void beforeStart(Participant... target);

    @Override
    public int requireTarget() {
        return 1;
    }

    public void onStart(){}

    public void run(int count){}

    public void onEnd(){}

    public void onSilentEnd(){}

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public void setMaximunCount(int maximunCount) {
        this.maximunCount = maximunCount;
    }

    public void setPeriod(TimeUnit timeUnit, int period) {
        this.period = Pair.of(timeUnit, period);
    }

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
}
