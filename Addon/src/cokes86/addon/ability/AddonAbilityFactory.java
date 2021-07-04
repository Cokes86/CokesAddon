package cokes86.addon.ability;

import cokes86.addon.ability.list.*;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;

import java.lang.annotation.*;
import java.util.*;

public class AddonAbilityFactory {
	protected static final Map<String, Class<? extends CokesAbility>> abilities = new HashMap<>();

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
		registerAbilities(PhantomThief.class);

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

		//1.3.1
		registerAbility(Cokes.class);

		//1.4.0
		registerAbility(Emily.class);

		//1.4.1
		registerAbility(Rude.class);
		registerAbility(Iris.class);

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

	public static void registerAbilities(Class<? extends CokesAbility> clazz) {
		SupportNMS support = clazz.getAnnotation(SupportNMS.class);
		if (support != null) {
			try {
				Class<? extends CokesAbility> clazz2 = Class.forName(clazz.getName().toLowerCase(Locale.ROOT)+"."+ServerVersion.getName()).asSubclass(clazz);
				registerAbility(clazz2);
				return;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("해당 버전에 호환되지 않습니다. : " + clazz.getName());
				return;
			}
		}
		System.out.println("해당 버전에 호환되지 않습니다. : " + clazz.getName());
	}

	public static List<String> nameValues() {
		return new ArrayList<>(abilities.keySet());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface SupportNMS { }
}
