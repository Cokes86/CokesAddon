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
	private static final Config<Integer> max = new Config<>(Perseverance.class, "최대치(%)", 200);
	private static final Config<Integer> upg = new Config<>(Perseverance.class, "성장치(%p)", 20);
	private static final Config<Double> period = new Config<>(Perseverance.class, "주기", 3.5);
	private double give = 100;
	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int seconds) {
			give += upg.getValue();
			if (give == max.getValue()) {
				give = max.getValue();
				stop(false);
			}
			ac.update("상대방에게 주는 대미지: " + (give) + "%");
		}
	}.setPeriod(TimeUnit.TICKS, (int) (period.getValue()*20)).setInitialDelay(TimeUnit.TICKS, (int) (period.getValue()*20));

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
			if (!passive.isRunning()) passive.start();
			passive.setCount(1);
		}
	}
}
