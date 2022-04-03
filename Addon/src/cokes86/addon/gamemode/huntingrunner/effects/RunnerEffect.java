package cokes86.addon.gamemode.huntingrunner.effects;

import daybreak.abilitywar.ability.AbilityBase.Update;
import daybreak.abilitywar.game.AbstractGame.Participant;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class RunnerEffect {

    public static <T extends RunnerEffect> T create(final @NotNull Class<T> effectClass, final @NotNull Participant participant) throws ReflectiveOperationException {
        if (!RunnerEffectFactory.isRegistered(effectClass)) {
            throw new IllegalArgumentException(effectClass.getSimpleName() + " 이펙트는 AbilityFactory에 등록되지 않은 능력입니다.");
        }
        try {
            return effectClass.cast(participant);
        } catch (Error error) {
            if (error instanceof OutOfMemoryError) {
                throw error;
            }
            throw new ReflectiveOperationException(error);
        }
    }

    public Participant getParticipant() {
        return participant;
    }

    private final Participant participant;

    public RunnerEffect(Participant participant) {
        this.participant = participant;
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
