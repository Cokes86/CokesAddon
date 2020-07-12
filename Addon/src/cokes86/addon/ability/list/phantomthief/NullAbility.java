package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame;

@AbilityManifest(name="사라짐", rank = AbilityManifest.Rank.C, species = AbilityManifest.Species.OTHERS, explain = {"이런! 능력이 사라지셨네!"})
public class NullAbility extends AbilityBase {

    public NullAbility(AbstractGame.Participant arg0) {
        super(arg0);
    }

}
