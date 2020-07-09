package cokes86.addon.ability;

import java.util.*;

import cokes86.addon.ability.list.*;
import cokes86.addon.ability.synergy.*;
import daybreak.abilitywar.ability.*;
import daybreak.abilitywar.ability.list.*;
import daybreak.abilitywar.game.list.mix.synergy.*;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;

public class AddonAbilityFactory {
	private static final Logger logger = Logger.getLogger(AddonAbilityFactory.class);
	protected static Map<String, Class<? extends AbilityBase>> abilities = new HashMap<>();
	protected static Map<String, Class<? extends Synergy>> synergies = new HashMap<>();
	
	static {
		registerAbility("cokes86.addon.ability.list."+ ServerVersion.getVersion().name()+".PhantomThief");
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
		registerAbility(Keily.class);
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
		registerSynergy(Aris.class, Assassin.class, AirDisintegration.class);
		registerSynergy(Muse.class, Sealer.class, Purgatory.class);
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

	public static void registerAbility(String className) {
		try{
			Class<? extends AbilityBase> clazz = Class.forName(className).asSubclass(AbilityBase.class);
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
		} catch (ClassNotFoundException e) {
			logger.error("§e" + className + " §f클래스는 존재하지 않습니다.");
		} catch (ClassCastException e) {
			logger.error("§e" + className + " §f클래스는 AbilityBase를 확장하지 않습니다.");
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
		else return synergies.getOrDefault(abilityName, null);
	}
}
