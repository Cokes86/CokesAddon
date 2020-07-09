package cokes86.addon.ability.list;

import java.util.Objects;

import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(
		name = "인내심",
		rank = Rank.B,
		species = Species.HUMAN,
		explain = {"매 $[period]초마다 상대방에게 주는 피해가 $[upg]%p씩 상승하며 최대 $[max]%까지 상승합니다.",
		"상대방을 공격할 시 이는 초기화되며, 최대치 상태에서 공격할 경우",
		"$[dura]초동안 상대방을 공격할 수 없게 됩니다.",
		"※능력 아이디어: RainStar_"}
)
public class Perseverance extends AbilityBase {
	private static final Config<Integer> dura = new Config<Integer>(Perseverance.class, "그로기시간", 3 ,2) {
		@Override
		public boolean Condition(Integer value) {
			return value >=0;
		}
	},
	max = new Config<Integer>(Perseverance.class, "최대치(%)", 200) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	},
	upg = new Config<Integer>(Perseverance.class, "성장치(%p)", 20) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	private static final Config<Double> period = new Config<Double>(Perseverance.class, "주기", 3.5) {
		@Override
		public boolean Condition(Double value) {
			return value > 0;
		}
	};
	
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive_1.start();
		}
	}
	
	ActionbarChannel ac = newActionbarChannel();

	public double give = 100;

	public Perseverance(Participant participant) {
		super(participant);
	}
	
	Timer passive_1 = new Timer() {

		@Override
		protected void run(int seconds) {
			if (seconds % (period.getValue()*20) == 0)
			give += upg.getValue();
			if (give >= max.getValue()) give = max.getValue();
			ac.update("상대방에게 주는 대미지: " + (give) + "%");
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	
	Timer passive_2 = new Timer(dura.getValue()) {

		@Override
		protected void run(int seconds) {		
			ac.update("그로기상태");
		}
		
		protected void onEnd() {
			ac.update(null);
			passive_1.start();
		}
	};
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			if (passive_2.isRunning()) e.setCancelled(true);
			else {
				e.setDamage(e.getDamage() * give/100.00);
				if (give == max.getValue()) {
					passive_1.stop(false);
					passive_2.start();
				}
				give = 100;
				ac.update("상대방에게 주는 대미지: " + (give) + "%");
			}
		}
		
		if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (Objects.equals(arrow.getShooter(), getPlayer())) {
				if (passive_2.isRunning()) e.setCancelled(true);
				else {
					e.setDamage(e.getDamage() * give/100.00);
					if (give == max.getValue()) {
						passive_1.stop(false);
						passive_2.start();
					}
					give = 100;
					ac.update("상대방에게 주는 대미지: " + (give) + "%");
				}
			}
		}
	}
}
