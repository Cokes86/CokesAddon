package com.cokes86.cokesaddon.util.compatibility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame.Participant;
import rainstar.abilitywar.game.Chaos.Overlap.Overlap;

import java.util.List;

public class RainStar implements IRainStar {
    @Override
    public List<AbilityBase> getAbilitiesInOverlap(Participant participant) {
        if (participant.getAbility() instanceof Overlap) {
            return ((Overlap) participant.getAbility()).getAbilities();
        }
        return null;
    }

    @Override
    public boolean isOverlap(Participant participant) {
        return participant.getAbility() instanceof Overlap;
    }
}
