package com.cokes86.cokesaddon.synergy;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.ability.list.*;
import com.cokes86.cokesaddon.synergy.list.*;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.list.*;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.addon.AddonLoader;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.annotations.Beta;
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
        registerSynergy(Rei.class, Rei.class, ReiSoulTaker.class);

        //1.4.0
        registerSynergy(Sniper.class, Perseverance.class, ShrineOfGod.class);

        //1.5.0
        registerSynergy(Elva.class, Cokes.class, CokesSoldier.class);

        //1.7.0
        registerSynergy(Aris.class, Magician.class, GravityArrow.class);

        //1.9.3
        registerSynergy(Ovisni.class, Ovisni.class, Vennominon.class);
        registerSynergy(Vennominon.class, Vennominon.class, Vennominaga.class);

        //1.10.0
        registerSynergy(Summoner.class, Summoner.class, RealSummoner.class);
        registerSynergy(Revenge.class, Revenge.class, Nemesis.class);
    }

    public static void registerSynergy(Class<? extends AbilityBase> first, Class<? extends AbilityBase> second, Class<? extends CokesSynergy> synergy) {
        if (first == null) {
            System.out.println("시너지 능력 등록 중 오류가 발생했습니다 [first능력이 null입니다] : "+ synergy.getName());
        }
        else if (second == null) {
            System.out.println("시너지 능력 등록 중 오류가 발생했습니다 [second능력이 null입니다] : "+ synergy.getName());
        }
        else if (SynergyFactory.getSynergy(first, second) == null) {
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
            Addon rainstar = AddonLoader.getAddon("RainStarAddon");
            String package_name = getVersionCheck(rainstar.getDescription().getVersion(), "1.7.2") ? "rainstar.abilitywar.ability." :"RainStarAbility.";
            registerSynergy(Rei.class, getAddonAbilityClass(package_name+"Inferno"), ReiBurningSoul.class);
            registerSynergy(Revenge.class, getAddonAbilityClass(package_name+"Revenger"), Wasinsangdam.class);
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

    // 현재 버전이 비교할 버전보다 높거나 같으면 true, 작으면 fasle를 리턴
    private static boolean getVersionCheck(String appVer, String compareVer){

        // 각각의 버전을 split을 통해 String배열에 담습니다.
        String[] appVerArray = new String[]{};
        if(!"".equals(appVer) && appVer != null ){
            appVerArray = appVer.split("\\.");
        }

        String[] compareVerArray = new String[]{};
        if(!"".equals(compareVer) && compareVer != null ){
            compareVerArray = compareVer.split("\\.");
        }

        // 비교할 버전이 없을 경우 false;
        if(appVerArray.length == 0 || compareVerArray.length == 0) return false;

        // 비교할 버전들 중 버전 길이가 가장 작은 버전을 구함
        int minLength = appVerArray.length;
        if(minLength > compareVerArray.length){
            minLength = compareVerArray.length;
        }

        for (int i=0; i<minLength; i++){
            int appVerSplit = Integer.parseInt(appVerArray[i]);
            int compareVerSplit = Integer.parseInt(compareVerArray[i]);
            if(appVerSplit > compareVerSplit){
                return true;
            }else if(appVerSplit == compareVerSplit){
                continue;
            }else {
                return false;
            }
        }

        return true;
    }
}
