package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "겜블러", rank = Rank.B, species = Species.HUMAN, explain = {
		"매 $[du]마다 받는 대미지와 주는 대미지가",
		"$[min]%에서 $[max]% 사이로 랜덤하게 변경됩니다.",
		"게임 중 단 한 번 철괴 우클릭 시 다음 차례에는 비율이 바뀌지 않고 고정됩니다.",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class Gambler extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> du = new Config<Integer>(Gambler.class, "주기", 15, Config.Condition.TIME) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, min = new Config<Integer>(Gambler.class, "최소치(%)", 75) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, max = new Config<Integer>(Gambler.class, "최대치(%)", 150) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	static {
		if (min.getValue() > max.getValue()) {
			min.setValue(min.getDefaultValue());
			max.setValue(max.getDefaultValue());
		}
	}

	private int go = 1;

	private int give = 0; // 주는 대미지
	private int receive = 0; // 받는 대미지

	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer passive = new AbilityTimer(du.getValue()) {

		@Override
		protected void onStart() {
			if (go == 0) {
				go = -1;
			} else {
				give = min.getValue() + (int) ((max.getValue() - min.getValue()) * Math.random());
				receive = min.getValue() + (int) ((max.getValue() - min.getValue()) * Math.random());
			}
		}

		@Override
		protected void run(int Count) {
			String giver = (give > 100 ? "§a" : (give < 100 ? "§c" : ""));
			String receiver = (receive > 100 ? "§c" : (receive < 100 ? "§a" : ""));
			String cool = go == 0 ? "" : " §a|§f 남은 시간: " + TimeUtil.parseTimeAsString(getCount());

			ac.update("주는 대미지: " + giver + give + "% §a|§f 받는 대미지: " + receiver + receive + "%§f" + cool);
		}

		@Override
		protected void onEnd() {
			passive.start();
		}
	};

	public Gambler(Participant participant) {
		super(participant);
		passive.register();
	}

	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (go == 1) {
				go = 0;
				getPlayer().sendMessage("자신이 지금 적용받고 있는 비율을 연장합니다.");
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		if (e.getDamager().equals(getPlayer())) {
			e.setDamage(e.getDamage() * ((double) give / 100));
		}
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * ((double) receive / 100));
		}
	}
}
