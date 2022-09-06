package com.cokes86.cokesaddon.gamemode.abilitymanhunt.effects;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame.Participant;

import java.util.StringJoiner;

public class RunnerEffect extends AbilityBase {

    public RunnerEffect(Participant participant) {
        super(participant);
    }

    public void onUpdate(Update update) {
    }

    public String toString() {
        AbilityManifest manifest = getClass().getAnnotation(AbilityManifest.class);
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("특수 효과 | §b" + manifest.name());
        for (String explain : manifest.explain()) {
            joiner.add(explain);
        }
        return joiner.toString();
    }
}
