package cokes86.addon.gamemode.huntingrunner.effects;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.Update;
import daybreak.abilitywar.game.AbstractGame.Participant;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class RunnerEffect extends AbilityBase {

    public RunnerEffect(Participant participant) {
        super(participant);
    }

    public void onUpdate(Update update) {
    }

    public String toString() {
        EffectManifest manifest = getClass().getAnnotation(EffectManifest.class);
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("특수 효과 | §b" + manifest.name());
        for (String explain : manifest.explain()) {
            joiner.add(explain);
        }
        return joiner.toString();
    }
}
