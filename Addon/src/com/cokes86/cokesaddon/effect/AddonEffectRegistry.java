package com.cokes86.cokesaddon.effect;

import com.cokes86.cokesaddon.effect.list.*;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddonEffectRegistry {
    private static final Map<String, Class<? extends Effect>> effects = new HashMap<>();

    static {
        registerEffect(ArmorBroken.class);
        registerEffect(Caught.class);
        registerEffect(DamageDown.class);
        registerEffect(GodOfBreak.class);
        registerEffect(Nightmare.class);
        registerEffect(Seal.class);
        registerEffect(Warp.class);
        registerEffect(Suffle.class);

        //2.0.0
        registerEffect(Debuging.class);
        registerEffect(Dizziness.class);
    }

    public static <T extends Effect> void registerEffect(Class<T> effect) {
        if (effect.getAnnotation(EffectManifest.class) == null) return;
        effects.put(effect.getAnnotation(EffectManifest.class).name(),effect);
        EffectRegistry.registerEffect(effect);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Effect> EffectRegistration<T> getRegistration(Class<T> effect) {
        return (EffectRegistration<T>) EffectRegistry.getRegistration(effect);
    }

    public static void nameValues() {
        new ArrayList<>(effects.keySet());
    }
}
