package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "인내심", rank = Rank.B, species = Species.HUMAN, explain = {
		"매 $[period]초마다 상대방에게 주는 피해가 $[upg]%p씩 상승하며 최대 $[max]%까지 상승합니다.",
		"상대방을 공격할 시 이는 초기화되어 100%로 돌아갑니다",
		"※능력 아이디어: RainStar_"}
)
public class Perseverance extends CokesAbility {
	private static final Config<Integer> max = new Config<Integer>(Perseverance.class, "최대치(%)", 200) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, upg = new Config<Integer>(Perseverance.class, "성장치(%p)", 20) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	private static final Config<Double> period = new Config<Double>(Perseverance.class, "주기", 3.5) {
		@Override
		public boolean condition(Double value) {
			return value > 0;
		}
	};
	public double give = 100;
	ActionbarChannel ac = newActionbarChannel();
	AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int seconds) {
			if (seconds % (period.getValue() * 20) == 0)
				give += upg.getValue();
			if (give >= max.getValue()) give = max.getValue();
			ac.update("상대방에게 주는 대미지: " + (give) + "%");
		}
	}.setPeriod(TimeUnit.TICKS, 1);

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
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (damager.equals(getPlayer())) {
			e.setDamage(e.getDamage() * give / 100.00);
			give = 100;
			ac.update("상대방에게 주는 대미지: " + (give) + "%");
		}
	}
}
