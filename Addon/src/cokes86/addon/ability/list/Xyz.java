package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@AbilityManifest(name = "엑시즈", rank = Rank.S, species = Species.HUMAN, explain = {
		"게임 중 1회에 한하여 철괴 우클릭 시 자신과",
		"주변 $[range]블럭 이내 플레이어는 게임 스폰으로 이동합니다.",
		"이후 $[duration] 이내 엑시즈를 잡지 못하였을 시",
		"스폰으로 이동된 모든 플레이어는 사망하게 됩니다.",
		"이로 인한 사망으로 인해 부활계 능력은 발동하지 않습니다.",
		"지속시간동안 스폰으로 이동된 모든 플레이어는",
		"신속2, 힘1 버프가 상시로 주어집니다."
})
public class Xyz extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> range = new Config<Integer>(Xyz.class, "범위", 10) {
		@Override
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	}, duration = new Config<Integer>(Xyz.class, "지속시간", 40, 2) {
		@Override
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	};
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
		return false;
	};
	XyzTimer xyzTime = new XyzTimer();
	private List<Player> targets = null;

	public Xyz(Participant arg0) throws IllegalStateException {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && targets == null && xyzTime.isRunning()) {
			targets = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
			if (targets.size() != 0) {
				xyzTime.start();
			} else {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				targets = null;
			}
		}
		return false;
	}

	class XyzTimer extends Duration implements Listener {
		Location location = Settings.getSpawnLocation().toBukkitLocation();
		Map<Player, ActionbarChannel> map = new HashMap<>();
		public XyzTimer() {
			super(duration.getValue() * 20);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			this.setPeriod(TimeUnit.TICKS, 1);
		}

		@Override
		protected void onDurationStart() {
			for (Player p : targets) {
				p.teleport(location);
				map.put(p, getGame().getParticipant(p).actionbar().newChannel());
				p.sendMessage("엑시즈 " + getPlayer().getName() + "님이 당신을 소멸시킬려 합니다.");
			}
			getPlayer().teleport(location);
		}

		@Override
		protected void onDurationProcess(int arg0) {
			int x = (int) getPlayer().getLocation().getX(), y = (int) getPlayer().getLocation().getY(), z = (int) getPlayer().getLocation().getZ();
			for (Entry<Player, ActionbarChannel> entry : map.entrySet()) {
				PotionEffects.SPEED.addPotionEffect(entry.getKey(), 30, 1, true);
				PotionEffects.INCREASE_DAMAGE.addPotionEffect(entry.getKey(), 30, 0, true);
				entry.getValue().update("남은 시간 : " + TimeUtil.parseTimeAsString(this.getFixedCount()) + " | 위치 x: " + x + " y: " + y + " z: " + z);
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
			for (Player p : map.keySet()) {
				p.setHealth(0.0);
			}
		}

		@Override
		protected void onDurationSilentEnd() {
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			if (e.getEntity().equals(getPlayer())) {
				for (Player p : map.keySet()) {
					p.sendMessage("엑시즈가 사망하였습니다.");
				}
				stop(true);
			} else if (map.containsKey(e.getEntity())) {
				map.remove(e.getEntity());
			}
		}

	}

	;

}
