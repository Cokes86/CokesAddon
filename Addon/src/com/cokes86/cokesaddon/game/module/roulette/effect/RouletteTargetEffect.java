package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.Participant;

public abstract class RouletteTargetEffect implements RouletteEffect {
    @Override
    public void apply(Participant... target) {
        effect(target[0], target[1]);
    }

    protected abstract void effect(Participant participant1, Participant participant2);

    @Override
    public int requireTarget() {
        return 2;
    }
}
