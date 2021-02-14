package cokes86.addon.synergy;

import cokes86.addon.ability.list.*;
import cokes86.addon.synergy.list.*;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.list.*;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        registerSynergy(Rei.class, Rei.class, SoulTakerRei.class);
    }

    public static void registerSynergy(Class<? extends AbilityBase> first, Class<? extends AbilityBase> second, Class<? extends CokesSynergy> synergy) {
        if (SynergyFactory.getSynergy(first, second) == null) {
            SynergyFactory.registerSynergy(first, second, synergy);
            if (!synergies.containsValue(synergy)) {
                AbilityManifest am = synergy.getAnnotation(AbilityManifest.class);
                synergies.put(am.name(), synergy);
            }
        } else {
            System.out.println("이미 등록된 시너지 능력입니다 : " + synergy.getName());
        }
    }

    @NotNull
    public static List<String> nameValues() {
        return new ArrayList<>(synergies.keySet());
    }
}
