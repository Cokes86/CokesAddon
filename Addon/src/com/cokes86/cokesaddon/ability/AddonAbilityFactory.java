package com.cokes86.cokesaddon.ability;

import com.cokes86.cokesaddon.ability.list.*;
import com.cokes86.cokesaddon.ability.list.PhantomThief;
import com.cokes86.cokesaddon.ability.test.Test;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddonAbilityFactory {
	protected static final Map<String, Class<? extends CokesAbility>> abilities = new HashMap<>();
	protected static final Map<String, Class<? extends CokesAbility>> test_abilities = new HashMap<>();

	static {
		registerTestAbility(Test.class);

		registerAbility(Seth.class);
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

		//1.1.4
		registerAbility(Casino.class);

		//1.3.0
		registerAbility(OnlyHitYou.class);

		//1.3.1
		registerAbility(Cokes.class);

		//1.4.0
		registerAbility(Emily.class);

		//1.4.1
		registerAbility(Rude.class);
		registerAbility(Iris.class);

		//1.5.0
		registerAbility(Reisen.class);

		//1.7.0
		registerAbility(Justin.class);
		registerAbility(BlackFeather.class);
		registerAbility(Ain.class);
		registerAbility(Sheep.class);
		registerAbility(Whitney.class);

		//1.8.0
		registerAbility(Kevin.class);
		registerAbility(Boxer.class);

		//1.9.2
		registerAbility(PhantomThief.class);
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

	public static void registerTestAbility(Class<? extends CokesAbility> clazz) {
		if (!test_abilities.containsValue(clazz)) {
			AbilityFactory.registerAbility(clazz);
			if (AbilityFactory.isRegistered(clazz)) {
				AbilityManifest am = clazz.getAnnotation(AbilityManifest.class);
				test_abilities.put(am.name(), clazz);
			} else {
				System.out.println("등록에 실패하였습니다. : " + clazz.getName());
			}
		} else {
			System.out.println("이미 애드온에 등록된 능력입니다 : " + clazz.getName());
		}
	}

	public static List<String> nameValues() {
		return new ArrayList<>(abilities.keySet());
	}

	public static Class<? extends CokesAbility> getTestAbilityByName(String name) {
		return test_abilities.get(name);
	}
}
