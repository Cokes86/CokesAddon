package com.cokes86.cokesaddon.util.timer;

import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

public class TimeoutTimer extends SimpleTimer {
    private final Runnable runnable;

    /**
     * 일정 시간 뒤 한 번만 작동하는 타이머를 시작합니다.
     * @param timeUnit 주기 단위
     * @param period 주기
     * @param runnable 실횅할 내용
     */
    public static void start(TimeUnit timeUnit, int period, Runnable runnable) {
        new TimeoutTimer(runnable).setPeriod(timeUnit, period).start();
    }

    public TimeoutTimer(Runnable runnable) {
        super(TaskType.NORMAL, 1);
        this.runnable = runnable;
    }

    @Override
    protected void onEnd() {
        runnable.run();
    }
}
