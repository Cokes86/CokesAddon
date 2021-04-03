package cokes86.addon.synergy.list;

import cokes86.addon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;

@AbilityManifest(name="미르<피닉스>", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        ""
})
@Beta
public class PheonixMir extends CokesSynergy {
    public PheonixMir(AbstractGame.Participant participant) {
        super(participant);
    }
}
