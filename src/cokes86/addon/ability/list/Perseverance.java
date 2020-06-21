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

@AbilityManifest(
		name = "인내심",
		rank = Rank.B,
		species = Species.HUMAN,
		explain = {"매 $[period]마다 상대방에게 주는 피해가 $[upg]%p씩 상승하며 최대 $[max]%까지 상승합니다.",
		"상대방을 공격할 시 이는 초기화되며, 최대치 상태에서 공격할 경우",
		"$[dura]동안 상대방을 공격할 수 없게 됩니다.",
		"※능력 아이디어: RainStar_"}
)
public class Perseverance extends AbilityBase {
	public static Config<Integer> dura = new Config<Integer>(Perseverance.class, "그로기시간", 3 ,2) {
		@Override
		public boolean Condition(Integer value) {
			return value >=0;
		}
	},
	max = new Config<Integer>(Perseverance.class, "최대치(%)", 250) {
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
	}, period = new Config<Integer>(Perseverance.class, "주기", 2, 2) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	
	public void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			passive_1.start();
		default:
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
			if (seconds % period.getValue() == 0)
			give += upg.getValue();
			if (give >= max.getValue()) give = max.getValue();
			ac.update("상대방에게 주는 대미지: " + (give) + "%");
		}
	};
	
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
				give = 100;
				if (give == max.getValue()) {
					passive_1.stop(false);
					passive_2.start();
				}
				ac.update("상대방에게 주는 대미지: " + (give) + "%");
			}
		}
		
		if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (Objects.equals(arrow.getShooter(), getPlayer())) {
				if (passive_2.isRunning()) e.setCancelled(true);
				else {
					e.setDamage(e.getDamage() * give/100.00);
					give = 100;
					if (give == max.getValue()) {
						passive_1.stop(false);
						passive_2.start();
					}
				}
			}
		}
	}
}
