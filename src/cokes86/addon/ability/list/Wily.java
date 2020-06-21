package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "윌리", rank = Rank.B, species = Species.HUMAN, explain = { "철괴 우클릭시 자신 주변 $[range]블럭 내 모든 플레이어는",
		"$[duration] 기절합니다. $[cooldown]" })
public class Wily extends AbilityBase implements ActiveHandler {

	private static Config<Integer> range = new Config<Integer>(Wily.class, "범위", 5) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, duration = new Config<Integer>(Wily.class, "지속시간", 2, 2) {

		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}

	}, cooldown = new Config<Integer>(Wily.class, "쿨타임", 30, 1) {

		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}

	};

	CooldownTimer cooldownTimer = new CooldownTimer(cooldown.getValue());

	public Wily(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && !cooldownTimer.isCooldown()) {
			for (Player p : LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(), range.getValue())) {
				Stun.apply(getGame().getParticipant(p), TimeUnit.SECONDS, duration.getValue());
			}
			SoundLib.BLOCK_ANVIL_FALL
					.playSound(LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(), range.getValue()));
			cooldownTimer.start();
			return true;
		}
		return false;
	}
}
