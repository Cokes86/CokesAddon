package com.cokes86.cokesaddon.gamemode.abilitymanhunt;

import com.cokes86.cokesaddon.gamemode.abilitymanhunt.effects.RunnerEffect;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "러너", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
        "당신은 러너입니다. 당신의 승리 조건은 엔더드래곤 잡기입니다.",
        "헌터를 피해 당신의 승리로 향해가세요!",
        "$(RUNNER_EFFECT)"
})
public class RunnerAbility extends AbilityBase {
    private RunnerEffect RUNNER_EFFECT = null;

    public RunnerEffect getRunnerEffect() {
        return RUNNER_EFFECT;
    }

    public void setRunnerEffect(RunnerEffect runnerEffect) {
        this.RUNNER_EFFECT = runnerEffect;
    }

    public void setRunnerEffect(Class<? extends RunnerEffect> runnerEffect) throws ReflectiveOperationException {
        this.RUNNER_EFFECT = RunnerEffect.create(runnerEffect, getParticipant());
    }

    public RunnerAbility(Participant arg0) throws IllegalStateException {
        super(arg0);
    }

    @Override
    protected void onUpdate(Update update) {
        RUNNER_EFFECT.onUpdate(update);
    }
}
