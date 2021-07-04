package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.effect.list.Caught;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.math.geometry.location.LocationIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "아리스", rank = Rank.B, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c체인§f: 5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 $[max_count]만큼 상승합니다.",
		"  사슬무덤의 지속시간, 쿨타임 동안은 §d사슬 카운터§f가 증가하지 않는다.",
		"§7철괴 우클릭 §8- §c사슬무덤§f: $[range]블럭 이내 모든 플레이어를",
		"  (§d사슬 카운터§f/2 + 4)블럭만큼 위로 끌어올린 후",
		"  누적된 §d사슬 카운터§f의 수만큼 §c붙잡힘§f을 부여합니다. $[cool]",
		"§7상태이상 §8- §c붙잡힘§f: 액티브, 타겟팅 능력을 사용할 수 없으며, 움직일 수 없게 됩니다.",
		"  또한 공격을 할 수 없고, 매 2초마다 1의 고정대미지를 받는 대신",
		"  그 이외의 대미지는 받을 수 없습니다. 고정대미지는 체력이 1 이하인 상태에서는 받지 않습니다."
})
@Tips(tip = {
		"참가자들을 공중에 띄우는 동안 1씩 고정대미지를 입히고",
		"낙하대미지까지 덤으로 익힐 수 있을 뿐 더러",
		"그 동안은 공격할 수도, 받을 수도 없기 때문에 도망치기 딱 좋은 능력"
}, stats = @Stats(offense = Level.SIX, survival = Level.EIGHT, crowdControl = Level.TWO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.EASY)
public class Aris extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> range = new Config<>(Aris.class, "범위", 7, integer -> integer > 0),
			cool = new Config<>(Aris.class, "쿨타임", 40, Config.Condition.COOLDOWN),
			max_count = new Config<>(Aris.class, "최대_사슬_카운터", 10, integer -> integer > 0);

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
			}
			if (getGame() instanceof Teamable) {
				Teamable game = (Teamable) getGame();
				return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};

	private int chain = 0;
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final Cooldown cooldown = new Cooldown(cool.getValue(), CooldownDecrease._50);
	private CaughtUpTimer duration = new CaughtUpTimer(null);
	private final AbilityTimer passive = new AbilityTimer() {
		int count = (int) (Wreck.isEnabled(getGame()) ? 5*20 * Wreck.calculateDecreasedAmount(50) : 5*20);
		{
			if (count == 0) {
				count = 10;
			}
		}

		@Override
		protected void run(int arg0) {
			if (!cooldown.isRunning() && (duration == null || !duration.isRunning())) {
				if (arg0 % count == 0) {
					chain++;
					if (chain >= max_count.getValue())
						chain = max_count.getValue();
				}
				actionbarChannel.update("§d사슬 카운터: " + chain);
			} else {
				actionbarChannel.update(null);
			}
		}

		@Override
		protected void onCountSet() {
			super.onCountSet();
			actionbarChannel.update("§d사슬 카운터: " + chain);
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	public Aris(Participant participant) {
		super(participant);
		passive.register();
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK) && (duration == null || !duration.isDuration()) && chain != 0 && !cooldown.isCooldown()) {
			final List<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
			if (players.isEmpty()) {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				return false;
			}
			duration = new CaughtUpTimer(players);
			return duration.start();
		}
		return false;
	}

	class CaughtUpTimer extends Duration {
		private final List<Player> list;
		private final int duration;
		private int cnt = 0;

		public CaughtUpTimer(List<Player> list) {
			super(chain*20 + (chain/2 + 4), cooldown);
			this.list = list;
			this.duration = chain;
			this.setPeriod(TimeUnit.TICKS, 1);
		}

		@Override
		protected void onDurationProcess(int i) {
			cnt++;
			if (cnt >=1 && cnt < (duration/2 + 4)) {
				for (Player player : list) {
					Location location = player.getLocation().clone();
					LocationIterator line = Line.iteratorBetween(location, location.add(0, 1, 0), 10);
					for (Location line_location : line.iterable()) {
						if(line_location.clone().add(0, 1.4, 0).getBlock().getType() == Material.AIR) {
							player.teleport(line_location);
						}
					}
				}
			} else if (cnt == duration / 2 + 5) {
				list.forEach(player -> Caught.apply(getGame().getParticipant(player.getUniqueId()), TimeUnit.SECONDS, duration));
			}
		}
	}
}
