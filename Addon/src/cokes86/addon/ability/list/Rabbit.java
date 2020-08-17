package cokes86.addon.ability.list;

import java.util.function.Predicate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(
		name = "토끼",
		rank = Rank.B,
		species = Species.ANIMAL,
		explain = {"항상 점프강화2 버프를 얻습니다.",
		"주변 $[range]블럭 이내에 플레이어가 있을 시 속도강화2 버프를 얻습니다."}
)
public class Rabbit extends CokesAbility {
	private static final Config<Integer> range = new Config<Integer>(Rabbit.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};

	public Rabbit(Participant arg0) {
		super(arg0);
		Passive.register();
	}

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				return !game.getDeathManager().isExcluded(entity.getUniqueId());
			}
			return getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue();
		}
		return true;
	};
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			Passive.setPeriod(TimeUnit.TICKS, 1).start();
		} else {
			PotionEffects.SPEED.removePotionEffect(getPlayer());
			PotionEffects.JUMP.removePotionEffect(getPlayer());
		}
	}

	AbilityTimer Passive = new AbilityTimer() {
		@Override
		protected void run(int arg0) {
			PotionEffects.JUMP.addPotionEffect(getPlayer(), Integer.MAX_VALUE,1,true);
			if (!LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate).isEmpty()) {
				PotionEffects.SPEED.addPotionEffect(getPlayer(), Integer.MAX_VALUE,1,true);
			} else {
				PotionEffects.SPEED.removePotionEffect(getPlayer());
			}
		}
	};
}
