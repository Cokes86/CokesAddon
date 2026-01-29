package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.library.SoundLib;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;

@AbilityManifest(name = "겜블러", rank = Rank.B, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b겜블§f: 매 $[GM_PERIOD]마다 받는 대미지와 주는 대미지가",
		"  $[MIN]% ~ $[MAX]% 사이로 랜덤하게 변경됩니다.",
		"§7철괴 우클릭 §8- §c패널티 다이스§f: §b겜블§f의 주기가 $[PD_PERIOD]로 조정됩니다.",
		"  다만, §b겜블§f로 수치가 랜덤하게 변경되지 아니하고",
		"  능력 사용 시점 수치의 $[PD_PENALTY]%만큼 안좋은 쪽으로 증감합니다.",
		"  두 스탯 중 하나라도 최악으로 치닿게 되면 종료됩니다. $[PD_COOLDOWN]",
		"[아이디어 제공자 §aRainStar_§f]"
})
public class Gambler extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> GM_PERIOD = Config.of(Gambler.class, "period", 10, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
			"# 겜블 주기",
			"# 기본값: 10(초)");
	private static final Config<Integer> VALUE1 = Config.of(Gambler.class, "value-1", 75, FunctionalInterfaces.positive(),
			"# 겜블 최소/최대치 값 중 하나",
			"# 두 숫자중 작은 값이 최소치, 큰 값이 최대치로 자동 변경됩니다.",
			"# 기본값: 75(%) 최소 / 150(%) 최대");
	private static final Config<Integer> VALUE2 = Config.of(Gambler.class, "value-2", 150, FunctionalInterfaces.positive(),
			"# 겜블 최소/최대치 값 중 하나",
			"# 두 숫자중 작은 값이 최소치, 큰 값이 최대치로 자동 변경됩니다.",
			"# 기본값: 75(%) 최소 / 150(%) 최대");
	private static final Config<Integer> PD_PENALTY = Config.of(Gambler.class, "penalty", 10, FunctionalInterfaces.positive(),
			"# 패널티 다이스로 안좋은 쪽으로 증감될 수치",
			"# 기본값: 10(%)");
	private static final Config<Integer> PD_COOLDOWN = Config.of(Gambler.class, "cooldown", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
			"# 패널티 다이스 쿨타임",
			"# 기본값: 60(초)");
	private static final Config<Integer> PD_PERIOD = Config.of(Gambler.class, "pd-period", 30, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
			"# 패널티 다이스 중 겜블 주기",
			"# 기본값: 30(초)");

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

	private boolean penaltyDice = false;

	private Pair<Integer, Integer> penaltyDiceInitial = Pair.of(0, 0);

	private final Cooldown cooldown = new Cooldown(PD_COOLDOWN.getValue());

	private int give = 0; // 주는 대미지
	private int receive = 0; // 받는 대미지

	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer passive = new AbilityTimer(GM_PERIOD.getValue()) {

		@Override
		protected void onStart() {
			if (penaltyDice) {
				give -= penaltyDiceInitial.getLeft() * PD_PENALTY.getValue()/100;
				receive += penaltyDiceInitial.getRight() * PD_PENALTY.getValue()/100;
				if (give <= MIN) {
					give = MIN;
					penaltyDice = false;
					passive.setMaximumCount(GM_PERIOD.getValue());
					passive.setCount(GM_PERIOD.getValue());
					cooldown.start();
				} else if (receive >= MAX) {
					receive = MAX;
					penaltyDice = false;
					passive.setMaximumCount(GM_PERIOD.getValue());
					passive.setCount(GM_PERIOD.getValue());
					cooldown.start();
				}
			} else {
				give = MIN + (int) ((MAX - MIN) * Math.random());
				receive = MIN + (int) ((MAX - MIN) * Math.random());
			}
		}

		@Override
		protected void run(int count) {
			String givePrefix = (give > 100 ? "§a" : (give < 100 ? "§c" : ""));
			String receivePrefix = (receive > 100 ? "§c" : (receive < 100 ? "§a" : ""));
			String penaltyDicePrefix = penaltyDice ? "§l" : "";
			ac.update("주는 대미지: " + givePrefix + penaltyDicePrefix + give + "%§r §a|§f 받는 대미지: " + receivePrefix + penaltyDicePrefix + receive+"%");

			if (count <= 5 && count >= 1) {
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			}
		}

		@Override
		protected void onEnd() {
			passive.start();
			SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
		}
	};

	public Gambler(Participant participant) {
		super(participant);
		passive.register();
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown() && !penaltyDice) {
			penaltyDice = true;
			passive.setMaximumCount(PD_PERIOD.getValue());
			passive.setCount(PD_PERIOD.getValue());
			getPlayer().sendMessage("[갬블러] 패널티 다이스를 발동합니다.");
			penaltyDiceInitial = Pair.of(give, receive);
			return true;
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
