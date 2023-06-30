package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.entity.Entity;

@AbilityManifest(name = "인내심", rank = Rank.B, species = Species.HUMAN, explain = {
		"매 $[period]초마다 상대방에게 주는 대미지가 $[upg]%p씩 상승하며 최대 $[max]%까지 상승합니다.",
		"상대방을 공격할 시 이는 0%로 변경됩니다",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class Perseverance extends CokesAbility {
	private static final Config<Integer> max = Config.of(Perseverance.class, "최대치(%)", 100, FunctionalInterfaces.positive());
	private static final Config<Integer> upg = Config.of(Perseverance.class, "성장치(%p)", 20, FunctionalInterfaces.positive());
	private static final Config<Integer> period = Config.of(Perseverance.class, "주기", 90, FunctionalInterfaces.positive(), FunctionalInterfaces.tickToSecond());

	private double give = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int seconds) {
			give += upg.getValue();
			if (give >= max.getValue()) {
				give = max.getValue();
				stop(false);
			}
			ac.update("상대방에게 주는 대미지 " + (give) + "% 증가");
		}
	}.setPeriod(TimeUnit.TICKS, period.getValue()).setInitialDelay(TimeUnit.TICKS, period.getValue());

	public Perseverance(Participant participant) {
		super(participant);
		passive.register();
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getDamager() == null) return;
		Entity damager = CokesUtil.getDamager(e.getDamager());

		if (damager.equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 + give / 100.00));
			give = 0;
			ac.update("상대방에게 주는 대미지 " + (give) + "% 증가");
			if (!passive.isRunning()) passive.start();
			passive.setCount(1);
		}
	}
}
