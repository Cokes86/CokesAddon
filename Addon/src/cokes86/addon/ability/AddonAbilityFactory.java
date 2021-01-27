package cokes86.addon.ability;

import cokes86.addon.ability.list.*;
import cokes86.addon.ability.synergy.*;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.list.*;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddonAbilityFactory {
	protected static final Map<String, Class<? extends CokesAbility>> abilities = new HashMap<>();
	protected static final Map<String, Class<? extends CokesSynergy>> synergies = new HashMap<>();
	private static final Logger logger = Logger.getLogger(AddonAbilityFactory.class);

	static {
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
		registerAbility(Poker.class);
		registerAbility(DataMining.class);
		registerAbility(Xyz.class);
		registerAbility(Keily.class);
		registerAbility(Elva.class);
		registerAbility(Reincarnation.class);
		registerAbility(Queen.class);
		registerAbility(Freud.class);
		registerAbility(Harmony.class);
		registerAbility(Cutter.class);

		//1.1
		registerAbility(Fish.class);
		registerAbility(Disguise.class);

		//1.1.3
		registerAbility(Boxer.class);

		//1.1.4
		registerAbility(Justin.class);
		registerAbility(Casino.class);

		//1.3.0
		registerAbility(OnlyHitYou.class);

		if (PhantomThief.initPhantomThief()) {
			registerAbility(PhantomThief.class);
			AbilityFactory.registerAbility(PhantomThief.NullAbility.class);
		} else {
			logger.error("팬텀 시프는 해당 버전에서 지원하지 않습니다.");
		}

		//test
		AbilityFactory.registerAbility(Test.class);

		registerSynergy(Poker.class, Poker.class, RoyalStraightFlush.class);
		registerSynergy(Xyz.class, Xyz.class, TheEnd.class);
		registerSynergy(Stalker.class, Reincarnation.class, LureOfRoses.class);
		registerSynergy(Revenge.class, Elva.class, RevengeArrow.class);
		registerSynergy(Aris.class, Assassin.class, AirDisintegration.class);
		registerSynergy(Muse.class, Sealer.class, Purgatory.class);
		registerSynergy(EnchantArrow.class, EnchantArrow.class, ReaperArrow.class);
		registerSynergy(Rune.class, Zeus.class, SlicingMaelstrom.class);
	}

	public static void registerAbility(Class<? extends CokesAbility> clazz) {
		if (!abilities.containsValue(clazz)) {
			AbilityFactory.registerAbility(clazz);
			if (AbilityFactory.isRegistered(clazz)) {
				AbilityList.registerAbility(clazz);
				AbilityManifest am = clazz.getAnnotation(AbilityManifest.class);
				if (clazz.getAnnotation(Beta.class) == null) abilities.put(am.name(), clazz);
			} else {
				System.out.println("등록에 실패하였습니다. : " + clazz.getName());
			}
		} else {
			System.out.println("이미 애드온에 등록된 능력입니다 : " + clazz.getName());
		}
	}

	public static void registerAbility(String className) {
		try {
			Class<? extends CokesAbility> clazz = Class.forName(className).asSubclass(CokesAbility.class);
			registerAbility(clazz);
		} catch (ClassNotFoundException e) {
			logger.error("§e" + className + " §f클래스는 존재하지 않습니다.");
		} catch (ClassCastException e) {
			logger.error("§e" + className + " §f클래스는 AbilityBase를 확장하지 않습니다.");
		}

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

	public static List<Class<? extends CokesAbility>> getAddonAbilities() {
		return new ArrayList<>(abilities.values());
	}

	public static List<Class<? extends CokesSynergy>> getAddonSynergies() {
		return new ArrayList<>(synergies.values());
	}

	public static List<String> nameValues() {
		return new ArrayList<>(abilities.keySet());
	}

	public static List<String> nameSynergyValues() {
		return new ArrayList<>(synergies.keySet());
	}

	public static Class<? extends CokesAbility> getAbilityByString(String abilityName) {
		return abilities.getOrDefault(abilityName, null);
	}

	public static Class<? extends CokesSynergy> getSynergyByString(String abilityName) {
		return synergies.getOrDefault(abilityName, null);
	}
}
