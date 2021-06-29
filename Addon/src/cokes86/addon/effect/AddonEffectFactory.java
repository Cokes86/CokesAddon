package cokes86.addon.effect;

import cokes86.addon.effect.list.*;
import daybreak.abilitywar.game.AbstractGame;

public enum AddonEffectFactory {
    CAUGHT(Caught.class),
    DAMAGE_DOWN(DamageDown.class),
    SEAL(Seal.class),
    GOD_OF_BREAK(GodOfBreak.class),
    NIGHTMARE(Nightmare.class);

    Class<? extends AbstractGame.Effect> clazz;

    AddonEffectFactory(Class<? extends AbstractGame.Effect> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends AbstractGame.Effect> getEffectClass() {
        return clazz;
    }

    public static void load() {
        for (AddonEffectFactory factory : AddonEffectFactory.values()) {
            try {
                Class.forName(factory.getEffectClass().getName());
            } catch (Exception ignored) {}
        }
    }
}
