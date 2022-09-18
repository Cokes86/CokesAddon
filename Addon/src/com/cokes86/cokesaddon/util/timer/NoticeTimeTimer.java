package com.cokes86.cokesaddon.util.timer;

import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;

import java.util.Objects;

public class NoticeTimeTimer extends AbilityTimer {
    private final String prefix;
    private final ActionbarChannel channel;

    public NoticeTimeTimer(Participant participant, String prefix, int time) {
        Objects.requireNonNull(participant.getAbility()).super(time);
        this.prefix = prefix;
        this.channel = participant.actionbar().newChannel();
    }

    @Override
    protected void run(int count) {
        channel.update(prefix+"§f: "+ TimeUtil.parseTimeAsString(count));
    }

    @Override
    protected void onEnd() {
        channel.unregister();
    }

    @Override
    protected void onSilentEnd() {
        channel.unregister();
    }
}
