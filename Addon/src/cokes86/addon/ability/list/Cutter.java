package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;

@AbilityManifest(name = "커터", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
		"체력이 $[risk]보다 높은 상태에서 철괴 우클릭 시 체력이 $[risk]가 감소합니다. $[cool]",
		"이후 자연회복을 제외하고 1초당 1씩, 총 $[duration]만큼의 체력을 회복합니다."
})
public class Cutter extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = new Config<Integer>(Cutter.class, "쿨타임", 10, Config.Condition.COOLDOWN) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, duration = new Config<Integer>(Cutter.class, "회복량", 7) {
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, risk = new Config<Integer>(Cutter.class, "코스트", 4) {
		public boolean condition(Integer value) {
			return value > 0;
		}
	};

	private final Cooldown cooldownTimer = new Cooldown(cool.getValue());
	private final Duration durationTimer = new Duration(duration.getValue(), cooldownTimer) {
		@Override
		protected void onDurationProcess(int i) {
			Healths.setHealth(getPlayer(), getPlayer().getHealth() + 1);
		}
	};

	public Cutter(AbstractGame.Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldownTimer.isCooldown() && !durationTimer.isDuration()) {
			if (getPlayer().getHealth() > risk.getValue()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() - risk.getValue());
				durationTimer.start();
				return true;
			} else {
				getPlayer().sendMessage("체력이 부족합니다.");
			}
		}
		return false;
	}
}
