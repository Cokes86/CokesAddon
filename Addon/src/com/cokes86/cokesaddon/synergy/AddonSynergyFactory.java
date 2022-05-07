package com.cokes86.cokesaddon.synergy;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.ability.list.*;
import com.cokes86.cokesaddon.synergy.list.*;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.list.*;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.annotations.Beta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AddonSynergyFactory {
    protected static final Map<String, Class<? extends CokesSynergy>> synergies = new HashMap<>();

    static {
        registerSynergy(Poker.class, Poker.class, RoyalStraightFlush.class);
        registerSynergy(Xyz.class, Xyz.class, TheEnd.class);
        registerSynergy(Stalker.class, Reincarnation.class, LureOfRoses.class);
        registerSynergy(Revenge.class, Elva.class, RevengeArrow.class);
        registerSynergy(Aris.class, Assassin.class, AirDisintegration.class);
        registerSynergy(Muse.class, Sealer.class, Purgatory.class);
        registerSynergy(EnchantArrow.class, EnchantArrow.class, ReaperArrow.class);
        registerSynergy(Rune.class, Zeus.class, SlicingMaelstrom.class);

        //1.3.0
        registerSynergy(Rei.class, Rei.class, ReiSoulTaker.class);

        //1.4.0
        registerSynergy(Sniper.class, Perseverance.class, ShrineOfGod.class);

        //1.5.0
        registerSynergy(Elva.class, Cokes.class, CokesSoldier.class);

        //1.7.0
        registerSynergy(Aris.class, Magician.class, GravityArrow.class);
    }

    public static void registerSynergy(Class<? extends AbilityBase> first, Class<? extends AbilityBase> second, Class<? extends CokesSynergy> synergy) {
        if (SynergyFactory.getSynergy(first, second) == null) {
            SynergyFactory.registerSynergy(first, second, synergy);
            if (AbilityFactory.isRegistered(synergy)) {
                AbilityManifest am = synergy.getAnnotation(AbilityManifest.class);
                if (synergy.getAnnotation(Beta.class) == null) synergies.put(am.name(), synergy);
            }
        } else {
            System.out.println("이미 등록된 시너지 능력입니다 : " + synergy.getName());
        }
    }

    @NotNull
    public static List<String> nameValues() {
        return new ArrayList<>(synergies.keySet());
    }

    public static void loadAddonSynergies() {
        if (CokesAddon.isLoadAddon("RainStarAddon")){
            registerSynergy(Rei.class, getAddonAbilityClass("RainStarAbility.Inferno"), ReiBurningSoul.class);
            //registerSynergy(Rude.class, getAddonAbilityClass("RainStarAbility.LittleDevil"), NaughtyDevil.class);
        }
    }

    private static Class<? extends AbilityBase> getAddonAbilityClass(String name) {
        Class<? extends AbilityBase> result;
        try {
            result = Class.forName(name).asSubclass(AbilityBase.class);
        } catch (Exception ignored) {
            result = null;
        }
        return result;
    }
}
