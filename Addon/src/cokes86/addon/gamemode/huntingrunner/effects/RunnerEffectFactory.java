package cokes86.addon.gamemode.huntingrunner.effects;

import java.util.HashMap;
import java.util.Map;

public class RunnerEffectFactory {
    private static final Map<String, Class<? extends RunnerEffect>> effectMap = new HashMap<>();

    public static void registerRunnerEffect(Class<? extends RunnerEffect> effect) {
        if (!effectMap.containsValue(effect)) {
            EffectManifest manifest = effect.getAnnotation(EffectManifest.class);
            if (!effectMap.containsKey(manifest.name())) {
                effectMap.put(manifest.name(), effect);
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
}
