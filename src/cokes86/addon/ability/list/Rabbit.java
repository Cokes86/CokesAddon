package cokes86.addon.ability.list;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;

@AbilityManifest(
		name = "토끼",
		rank = Rank.B,
		species = Species.ANIMAL,
		explain = {"항상 점프강화2 버프를 얻습니다.",
		"주변 $[range]블럭 이내에 플레이어가 있을 시 속도강화2 버프를 얻습니다."}
)
public class Rabbit extends AbilityBase {
	public static Config<Integer> range = new Config<Integer>(Rabbit.class, "범위", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	public Rabbit(Participant arg0) {
		super(arg0);
	}
	
	protected void onUpdate(Update update) {
		if (update.equals(Update.RESTRICTION_CLEAR)) {
			Passive.setPeriod(TimeUnit.TICKS,1).start();
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		Passive.setPeriod(TimeUnit.TICKS,1).start();
	}

	Timer Passive = new Timer() {
		@Override
		protected void run(int arg0) {
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 40, 1));
			if (!LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(), range.getValue()).isEmpty()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
			}
		}
	};
}
