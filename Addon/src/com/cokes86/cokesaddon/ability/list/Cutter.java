package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;

@AbilityManifest(name = "커터", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN, explain = {
		"체력이 $[risk]보다 높은 상태에서 철괴 우클릭 시 체력이 $[risk]가 감소합니다. $[cool]",
		"이후 자연회복을 제외하고 1초당 1씩, 총 $[duration]만큼의 체력을 회복합니다."
})
@Lite
public class Cutter extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = Config.of(Cutter.class, "cooldown", 10, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
			"쿨타임",
			"기본값: 10 (초)");
	private static final Config<Integer> duration = Config.of(Cutter.class, "duration", 7, FunctionalInterfaces.positive(),
			"체력회복 지속시간",
			"기본값: 7 (초)");
	private static final Config<Integer> risk = Config.of(Cutter.class, "skill-cost", 4, FunctionalInterfaces.positive(),
			"스킬의 코스트",
			"기본값: 4");

	private final Cooldown cooldownTimer = new Cooldown(cool.getValue());
	private final Duration durationTimer = new Duration(duration.getValue(), cooldownTimer) {
		@Override
		protected void onDurationProcess(int i) {
			CokesUtil.healPlayer(getPlayer(), 1);
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
				return durationTimer.start();
			} else {
				getPlayer().sendMessage("체력이 부족합니다.");
			}
		}
		return false;
	}
}
