package com.cokes86.cokesaddon.game.module.roulette;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.module.DeathManager;

import java.util.ArrayList;
import java.util.List;

public abstract class RouletteEffect {
    public abstract boolean apply(Participant participant);

    public static <T extends RouletteEffect> T create(Class<T> effect) {
        return null;
    }

    public List<Participant> getParticipantsWithoutEliminater() {
        ArrayList<Participant> others = new ArrayList<>();
        for (Participant check : GameManager.getGame().getParticipants()) {
            if (check.getGame().hasModule(DeathManager.class)) {
                if (check.getGame().getModule(DeathManager.class).isExcluded(check.getPlayer())) {
                    continue;
                }
            }
            others.add(check);
        }
        return others;
    }
}
