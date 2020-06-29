package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;

@AbilityManifest(name = "겜블러", rank = Rank.B, species = Species.HUMAN, explain = { "매 $[du]마다 받는 대미지와 주는 대미지의 비율이",
		"$[min]%에서 $[max]% 사이로 랜덤하게 바뀝니다.", "게임 중 단 한 번 철괴 우클릭 시 다음 차례에는 비율이 바뀌지 않고 고정됩니다.", "※능력 아이디어: RainStar_" })
public class Gambler extends AbilityBase implements ActiveHandler {
	public static Config<Integer> du = new Config<Integer>(Gambler.class, "주기", 15, 2) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, min = new Config<Integer>(Gambler.class, "최소치(%)", 50) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, max = new Config<Integer>(Gambler.class, "최대치(%)", 200) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};

	static {
		if (min.getValue() > max.getValue()) {
			min.setValue(min.getDefaultValue());
			max.setValue(max.getDefaultValue());
		}
	}
	int go = 1;

	int give = 0; // 주는 대미지
	int receive = 0; // 받는 대미지

	ActionbarChannel ac = newActionbarChannel();

	public Gambler(Participant participant) {
		super(participant);
	}
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			passive.start();
			break;
		default:
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

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		passive.start();
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

	Timer passive = new Timer(du.getValue()) {

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
			String giver = give > 100 ? "§a" : (give < 0 ? "§c": "") + give;
			String receiver = give > 100 ? "§c" : (give < 0 ? "§a": "") + give;	
			String cool = go == 0 ? "" : ",  남은 시간: " + TimeUtil.parseTimeAsString(getCount());

			ac.update("주는 대미지: " + giver + "%§f,  받는 대미지: " + receiver + "%§f" + cool);
		}

		@Override
		protected void onEnd() {
			passive.start();
		}
	};
}
