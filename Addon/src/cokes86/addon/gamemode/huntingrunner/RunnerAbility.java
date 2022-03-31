package cokes86.addon.gamemode.huntingrunner;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "러너", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
        "당신은 러너입니다. 당신의 승리 조건은 엔더드래곤 잡기입니다.",
        "헌터를 피해 당신의 승리로 향해가세요!",
        "$(SPECIAL_ABILITY)"
})
public class RunnerAbility extends AbilityBase {

    public RunnerAbility(Participant arg0) throws IllegalStateException {
        super(arg0);
    }
    
}
