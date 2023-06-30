package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.Participant;

public abstract class RouletteSingleEffect implements RouletteEffect {
    @Override
    public void apply(Participant... target) {
        effect(target[0]);
    }

    protected abstract void effect(Participant participant);

    @Override
    public int requireTarget() {
        return 1;
    }
}
