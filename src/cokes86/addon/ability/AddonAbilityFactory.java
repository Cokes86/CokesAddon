package cokes86.addon.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cokes86.addon.ability.list.Aris;
import cokes86.addon.ability.list.Blocks;
import cokes86.addon.ability.list.CreeperController;
import cokes86.addon.ability.list.DataMining;
import cokes86.addon.ability.list.Elva;
import cokes86.addon.ability.list.EnchantArrow;
import cokes86.addon.ability.list.Freud;
import cokes86.addon.ability.list.Gambler;
import cokes86.addon.ability.list.GodsBless;
import cokes86.addon.ability.list.Harmony;
import cokes86.addon.ability.list.Mir;
import cokes86.addon.ability.list.Ovisni;
import cokes86.addon.ability.list.Perseverance;
import cokes86.addon.ability.list.PhantomThief;
import cokes86.addon.ability.list.Pocker;
import cokes86.addon.ability.list.Queen;
import cokes86.addon.ability.list.Rabbit;
import cokes86.addon.ability.list.Rei;
import cokes86.addon.ability.list.Reincarnation;
import cokes86.addon.ability.list.Resurrection;
import cokes86.addon.ability.list.Revenge;
import cokes86.addon.ability.list.Rune;
import cokes86.addon.ability.list.Sealer;
import cokes86.addon.ability.list.Seth;
import cokes86.addon.ability.list.Summoner;
import cokes86.addon.ability.list.Thorn;
import cokes86.addon.ability.list.Unbelief;
import cokes86.addon.ability.list.VigilanteLeader;
import cokes86.addon.ability.list.Wily;
import cokes86.addon.ability.list.Xyz;
import cokes86.addon.ability.synergy.LureOfRoses;
import cokes86.addon.ability.synergy.RevengeArrow;
import cokes86.addon.ability.synergy.RoyalStraightFlush;
import cokes86.addon.ability.synergy.TheEnd;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.list.Stalker;
import daybreak.abilitywar.game.list.mixability.synergy.Synergy;
import daybreak.abilitywar.game.list.mixability.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;

public class AddonAbilityFactory {
	protected static Map<String, Class<? extends AbilityBase>> abilities = new HashMap<>();
	protected static Map<String, Class<? extends Synergy>> synergies = new HashMap<>();
	
	static {
		registerAbility(PhantomThief.class);
		registerAbility(Seth.class);
		registerAbility(Rabbit.class);
		registerAbility(Ovisni.class);
		registerAbility(Resurrection.class);
		registerAbility(Blocks.class);
		registerAbility(Summoner.class);
		registerAbility(Unbelief.class);
		registerAbility(Mir.class);
		registerAbility(Aris.class);
		registerAbility(Sealer.class);
		registerAbility(Rei.class);
		registerAbility(GodsBless.class);
		registerAbility(Gambler.class);
		registerAbility(Perseverance.class);
		registerAbility(Rune.class);
		registerAbility(Thorn.class);
		registerAbility(Revenge.class);
		registerAbility(EnchantArrow.class);
		registerAbility(VigilanteLeader.class);
		registerAbility(Pocker.class);
		registerAbility(DataMining.class);
		registerAbility(Xyz.class);
		registerAbility(CreeperController.class);
		registerAbility(Elva.class);
		registerAbility(Reincarnation.class);
		registerAbility(Queen.class);
		registerAbility(Freud.class);
		registerAbility(Wily.class);
		registerAbility(Harmony.class);
		
		registerSynergy(Pocker.class, Pocker.class, RoyalStraightFlush.class);
		registerSynergy(GodsBless.class, Xyz.class, TheEnd.class);
		registerSynergy(GodsBless.class, GodsBless.class, TheEnd.class);
		registerSynergy(Xyz.class, Xyz.class, TheEnd.class);
		registerSynergy(Stalker.class, Reincarnation.class, LureOfRoses.class);
		registerSynergy(Revenge.class, Elva.class, RevengeArrow.class);
	}
	
	public static void registerAbility(Class<? extends AbilityBase> clazz) {
		if (!abilities.containsValue(clazz)) {
			AbilityFactory.registerAbility(clazz);
			if (AbilityFactory.isRegistered(clazz)) {
				AbilityList.registerAbility(clazz);
				AbilityManifest am = clazz.getAnnotation(AbilityManifest.class);
				abilities.put(am.name(), clazz);
			} else {
				System.out.println("등록에 실패하였습니다. : "+clazz.getName());
			}
		} else {
			System.out.println("이미 애드온에 등록된 능력입니다 : "+clazz.getName());
		}
	}
	
	public static void registerSynergy(Class<? extends AbilityBase> first, Class<? extends AbilityBase> second, Class<? extends Synergy> synergy) {
		if (SynergyFactory.getSynergy(first, second) == null) {
			SynergyFactory.registerSynergy(first, second, synergy);
			if (!synergies.containsValue(synergy)) {
				AbilityManifest am = synergy.getAnnotation(AbilityManifest.class);
				synergies.put(am.name(), synergy);
			}
		} else {
			System.out.println("이미 등록된 시너지 능력입니다 : "+synergy.getName());
		}
	}
	
	public static List<Class<? extends AbilityBase>> getAddonAbilities() {
		return new ArrayList<>(abilities.values());
	}
	
	public static List<Class<? extends Synergy>> getAddonSynergies() {
		return new ArrayList<>(synergies.values());
	}
	
	public static List<String> nameValues() {
		return new ArrayList<>(abilities.keySet());
	}
	
	public static List<String> nameSynergyValues() {
		return new ArrayList<>(synergies.keySet());
	}

	public static Class<? extends AbilityBase> getByString(String abilityName) {
		if (abilities.containsKey(abilityName)) return abilities.get(abilityName);
		else if (synergies.containsKey(abilityName)) return synergies.get(abilityName);
		else return null;
	}
}
