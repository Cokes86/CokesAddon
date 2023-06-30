package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.Participant;

public abstract class RouletteAllEffect implements RouletteEffect {
    @Override
    public void apply(Participant... target) {
        for (Participant participant : target) {
            effect(participant);
        }
    }

    protected abstract void effect(Participant participant);
}
