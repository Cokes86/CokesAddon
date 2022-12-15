package com.cokes86.cokesaddon.game.edible.roulette.list;

import com.cokes86.cokesaddon.game.edible.roulette.RouletteEffect;

import daybreak.abilitywar.game.AbstractGame.Participant;

public class Teleport extends RouletteEffect {
    private final Participant a, b;

    public Teleport(Participant a, Participant b) {
        this.a = a;
        this.b = b;
    }

    public boolean apply() {
        b.getPlayer().teleport(a.getPlayer().getLocation());
        return true;
    }

    public String rouletteName() {
        return b.getPlayer().getName()+"이(가) "+a.getPlayer().getName()+"에게 텔레포트";
    }
}
