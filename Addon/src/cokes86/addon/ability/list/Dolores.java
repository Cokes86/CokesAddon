package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame;

@AbilityManifest(name = "돌로레스", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 남에 의해 고통 받는 자: ",
        "철괴 우클릭 - 남에게 고통을 주는 자: "
})
public class Dolores extends CokesAbility {
    public Dolores(AbstractGame.Participant arg0) {
        super(arg0);
    }
}
