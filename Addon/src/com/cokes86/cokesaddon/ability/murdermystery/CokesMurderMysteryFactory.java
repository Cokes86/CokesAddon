package com.cokes86.cokesaddon.ability.murdermystery;

import com.cokes86.cokesaddon.ability.murdermystery.innocent.TaxiDriver;
import com.cokes86.cokesaddon.ability.murdermystery.murder.DisguiserMurderer;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.list.murdermystery.CharacterType;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;

public class CokesMurderMysteryFactory {
    static {
        registerMurderJob(DisguiserMurderer.class, "변장술사");
        registerInnocentJob(TaxiDriver.class, "택시기사");
    }

    public static void registerMurderJob(Class<? extends AbstractMurderer> murder, String name) {
        AbilityFactory.registerAbility(murder);
        JobList.registerJob(murder, name, CharacterType.MURDER);
    }

    public static void registerInnocentJob(Class<? extends AbstractInnocent> innocent, String name) {
        AbilityFactory.registerAbility(innocent);
        JobList.registerJob(innocent, name, CharacterType.INNOCENT);
    }
}
