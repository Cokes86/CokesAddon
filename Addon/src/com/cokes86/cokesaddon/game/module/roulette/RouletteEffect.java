package com.cokes86.cokesaddon.game.module.roulette;

import daybreak.abilitywar.game.AbstractGame.Participant;

public abstract class RouletteEffect {
    public abstract boolean apply(Participant participant);

    public static <T extends RouletteEffect> T create(Class<T> effect) {
        return null;
    }
}
