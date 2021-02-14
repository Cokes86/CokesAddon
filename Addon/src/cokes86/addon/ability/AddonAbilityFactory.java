package cokes86.addon.ability;

import cokes86.addon.ability.list.*;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddonAbilityFactory {
	protected static final Map<String, Class<? extends CokesAbility>> abilities = new HashMap<>();
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
		//registerAbility(Cokes.class);

		if (PhantomThief.initPhantomThief()) {
			registerAbility(PhantomThief.class);
			AbilityFactory.registerAbility(PhantomThief.NullAbility.class);
		} else {
			logger.error("팬텀 시프는 해당 버전에서 지원하지 않습니다.");
		}

		//test
		registerAbility(Test.class);
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

	public static List<Class<? extends CokesAbility>> getAddonAbilities() {
		return new ArrayList<>(abilities.values());
	}

	public static List<String> nameValues() {
		return new ArrayList<>(abilities.keySet());
	}
}
