package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.effect.list.GodOfBreak;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Material;

@AbilityManifest(name = "신의 가호", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §c파괴의 신§f: 자신에게 §e파괴의 §c신 §f효과를 $[duration] 부여합니다. $[rightCool]",
		"§7철괴 좌클릭 §8- §c아직 안끝났어§f: 자신에게 적용중인 §e파괴의 §c신 §f효과의 지속시간을 초기화합니다. $[leftCool]",
		"§7상태이상 §8- §e파괴의 §c신§f: 일정 시간동안 자신의 공격력이 3 증가합니다.",
		"  상대방을 살해할 시 공격력이 1 더 추가하고 지속시간을 초기화합니다.",
		"  지속시간 종료 시 자신이 얻은 공격력의 5배에 달하는 대미지를 받습니다."
})
public class GodsBless extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> duration = Config.of(GodsBless.class, "duration", 15, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
	private static final Config<Integer> rightCool = Config.of(GodsBless.class, "right-cooldown", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private static final Config<Integer> leftCool = Config.of(GodsBless.class, "left-cooldown", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private final Cooldown rightCooldown = new Cooldown(rightCool.getValue(), "파괴의 신");
	private final Cooldown leftCooldown = new Cooldown(leftCool.getValue(), "아직 안끝났어");

	private final GodsAttach observer = new GodsAttach();

	public GodsBless(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		GodOfBreak effect = (GodOfBreak) getParticipant().getPrimaryEffect(EffectRegistry.getRegistration(GodOfBreak.class));
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !rightCooldown.isCooldown() && effect == null) {
			GodOfBreak.apply(getParticipant(), TimeUnit.SECONDS, duration.getValue()).attachObserver(observer);
			return true;
		} else if (arg0 == Material.IRON_INGOT && arg1 == ClickType.LEFT_CLICK && !leftCooldown.isCooldown()) {
			if (effect != null) {
				effect.setCount(duration.getValue()*20);
				return leftCooldown.start();
			}
		}
		return false;
	}

	private class GodsAttach implements SimpleTimer.Observer {
		@Override
		public void onEnd() {
			rightCooldown.start();
		}
	}
}
