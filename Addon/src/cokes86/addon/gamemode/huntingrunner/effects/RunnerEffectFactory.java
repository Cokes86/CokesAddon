package cokes86.addon.gamemode.huntingrunner.effects;

import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;

import java.util.*;

public class RunnerEffectFactory {
    private static final Map<String, Class<? extends RunnerEffect>> effectMap = new HashMap<>();

    public static void registerRunnerEffect(Class<? extends RunnerEffect> effect) {
        if (!effectMap.containsValue(effect) && !AbilityFactory.isRegistered(effect)) {
            AbilityManifest manifest = effect.getAnnotation(AbilityManifest.class);
            if (!effectMap.containsKey(manifest.name())) {
                effectMap.put(manifest.name(), effect);
                AbilityFactory.registerAbility(effect);
            } else {
                System.out.println("러너 이펙트 이름이 중첩되었습니다. : " + effect.getName());
            }
        } else {
            System.out.println("이미 등록된 러너 이펙트입니다. : " + effect.getName());
        }
    }

    public static boolean isRegistered(Class<? extends RunnerEffect> effect) {
        return effectMap.containsValue(effect);
    }

    public static Collection<Class<? extends RunnerEffect>> getRunnerEffects() {
        return effectMap.values();
    }
}
