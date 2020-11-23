package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

@AbilityManifest(name = "신의 가호", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §c파괴의 신§r: 지속시간 $[duration] 동안 상대방을 공격할 시 $[base]의 대미지가 추가됩니다.",
		"  지속시간동안 플레이어 1명을 죽일 시 추가대미지가 $[add]씩 증가하며",
		"  지속시간이 $[duration]로 변경됩니다.",
		"  지속시간 종료 시 (지금까지 얻은 추가대미지 * $[multiply])의 대미지를 받습니다. $[cool]"
})
public class GodsBless extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> duration = new Config<Integer>(GodsBless.class, "지속시간", 15, 2) {
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	}, cool = new Config<Integer>(GodsBless.class, "쿨타임", 60, 1) {
		public boolean condition(Integer arg0) {
			return arg0 >= 0;
		}
	}, base = new Config<Integer>(GodsBless.class, "추가대미지.기본", 3) {
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	}, add = new Config<Integer>(GodsBless.class, "추가대미지.상승량", 1) {
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	};
	private static final Config<Double> multiply = new Config<Double>(GodsBless.class, "배율", 5.0) {
		public boolean condition(Double arg0) {
			return arg0 > 1;
		}
	};
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown c = new Cooldown(cool.getValue());
	private final BlessTimer bless = new BlessTimer();

	public GodsBless(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !c.isCooldown() && !bless.isRunning()) {
			bless.start();
			return true;
		}
		return false;
	}

	class BlessTimer extends AbilityTimer implements Listener {
		int add_damage = base.getValue();

		public BlessTimer() {
			super(duration.getValue());
			this.setPeriod(TimeUnit.SECONDS, 1);
		}

		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int arg0) {
			ac.update("추가 대미지: " + add_damage + " | 남은 시간: " + TimeUtil.parseTimeAsString(this.getFixedCount()));
		}

		protected void onCountSet() {
			ac.update("추가 대미지: " + add_damage + " | 남은 시간: " + TimeUtil.parseTimeAsString(this.getFixedCount()));
		}

		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}

		protected void onEnd() {
			onSilentEnd();
			getPlayer().damage(add_damage * multiply.getValue(), getPlayer());
			ac.update(null);
		}

		@EventHandler
		public void onParticipantDeath(ParticipantDeathEvent e) {
			if (!e.getParticipant().equals(getParticipant())) {
				if (e.getPlayer().getKiller() != null && e.getPlayer().getKiller().equals(getPlayer())) {
					add_damage += add.getValue();
					this.setCount(duration.getValue());
				}
			}
		}
	}
}
