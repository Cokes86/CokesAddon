package com.cokes86.cokesaddon.util.compatibility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame.Participant;

import java.util.List;

public interface IRainStar {

    List<AbilityBase> getAbilitiesInOverlap(Participant participant);

    boolean isOverlap(Participant participant);
}
