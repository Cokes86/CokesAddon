package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

//레스님꺼랑 또 조합하고 싶은디
@AbilityManifest(name = "因話饈", rank = Rank.A, species = Species.UNDEAD, explain = {
        ""
})
public class WhatMaking extends CokesSynergy {
    public WhatMaking(Participant participant) {
        super(participant);
    }
}
