package com.cokes86.cokesaddon.game.module.roulette.effect;

import daybreak.abilitywar.game.AbstractGame.Participant;

public interface RouletteEffect {
    void apply(Participant... target);

    default int requireTarget() {
        return 0;
    }
}
