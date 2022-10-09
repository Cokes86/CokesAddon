package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import org.bukkit.Material;

@AbilityManifest(name = "겜블러", rank = Rank.B, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c인생은 한방§f: 매 $[PERIOD]마다 받는 대미지와 주는 대미지가",
		"  $[MIN]%에서 $[MAX]% 사이로 랜덤하게 변경됩니다.",
		"§7철괴 우클릭 §8- §c밑장빼기§f: 게임 중 단 한 번만 사용 가능합니다.",
		"  <§c인생은 한방§f>에서 정해진 비율을 $[PERIOD] 연장합니다.",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class Gambler extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> PERIOD = Config.of(Gambler.class, "period", 15, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
			"# 인생은 한방에서 바뀌는 주기 / 밑장빼기 연장 시간",
			"# 기본값: 15(초)");
	private static final Config<Integer> VALUE1 = Config.of(Gambler.class, "value-1", 75, FunctionalInterfaces.positive(),
			"# 인생은 한방 최소/최대치 값 중 하나",
			"# 두 숫자중 작은 값이 최소치, 큰 값이 최대치로 자동 변경됩니다.",
			"# 기본값: 75(%) 최소 / 150(%) 최대");
	private static final Config<Integer> VALUE2 = Config.of(Gambler.class, "value-2", 150, FunctionalInterfaces.positive(),
			"# 인생은 한방 최소/최대치 값 중 하나",
			"# 두 숫자중 작은 값이 최소치, 큰 값이 최대치로 자동 변경됩니다.",
			"# 기본값: 75(%) 최소 / 150(%) 최대");

	private static final int MIN, MAX;

	static {
		if (VALUE1.getValue() > VALUE2.getValue()) {
			MAX = VALUE1.getValue();
			MIN = VALUE2.getValue();
		} else {
			MAX = VALUE2.getValue();
			MIN = VALUE1.getValue();
		}
	}

	private int go = 1;

	private int give = 0; // 주는 대미지
	private int receive = 0; // 받는 대미지

	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer passive = new AbilityTimer(PERIOD.getValue()) {

		@Override
		protected void onStart() {
			if (go == 0) {
				go = -1;
			} else {
				give = MIN + (int) ((MAX - MIN) * Math.random());
				receive = MIN + (int) ((MAX - MIN) * Math.random());
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
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getDamager() != null && e.getDamager().equals(getPlayer())) {
			e.setDamage(e.getDamage() * ((double) give / 100));
		}
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * ((double) receive / 100));
		}
	}
}
