package com.cokes86.cokesaddon.util.compatibility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;

import java.util.List;

public class Compatibility {
    private static final IRainStar RAIN_STAR;

    static {
        try {
            RAIN_STAR = Class.forName("com.cokes86.cokesaddon.util.compatibility.RainStar")
                    .asSubclass(IRainStar.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static List<AbilityBase> getAbililitiesInOverlap(Participant participant) {
        return RAIN_STAR.getAbilitiesInOverlap(participant);
    }

    public static boolean isOverlap(Participant participant) {
        return RAIN_STAR.isOverlap(participant);
    }
}
