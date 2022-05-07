package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;


@AbilityManifest(name = "장난꾸러기 악마", rank = Rank.A, species = Species.UNDEAD, explain = {
})
public class NaughtyDevil extends CokesSynergy {
    public NaughtyDevil(Participant participant) {
        super(participant);
    }
}
