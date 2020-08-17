package cokes86.addon.ability.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;

@AbilityManifest(name = "아리스", rank = Rank.B, species = Species.HUMAN, explain = {
		"5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 $[max_count]만큼 상승합니다.",
		"철괴 우클릭시 $[range]블럭 이내 모든 플레이어를 공중에 고정시킵니다. $[cool]",
		"공중에는 누적된 §d사슬 카운터§f만큼 초로 환산되어 고정시키며,",
		"(§d사슬 카운터§f/2 + 4)블럭만큼 위로 고정시킵니다.",
		"공중에 고정되어있는 플레이어는 그동안 움직일 수 없으며,",
		"매 2초마다 1의 고정대미지를 입습니다. 그 이외의 대미지는 받지 않으며,",
		"고정대미지는 체력이 1 이하인 상태에서는 받지 않습니다.",
		"지속시간과 쿨타임동안 §d사슬 카운터§f는 증가하지 않습니다." })
public class Aris extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> range = new Config<Integer>(Aris.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Aris.class, "쿨타임", 40, 1) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, max_count = new Config<Integer>(Aris.class, "최대_사슬_카운터", 10) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
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
		return true;
	};

	private final Map<Player, Pair<Location, ActionbarChannel>> holding = new HashMap<>();

	int chain = 0;
	ActionbarChannel ac = newActionbarChannel();

	AbilityTimer passive = new AbilityTimer() {
		int count = (int) (5 * (WRECK.isEnabled(getGame()) ? WRECK.calculateDecreasedAmount(50) : 1));
		@Override
		protected void run(int arg0) {
			if (!c.isRunning()) {
				if (arg0 % count == 0) {
					chain++;
					if (chain >= max_count.getValue())
						chain = max_count.getValue();
				}
				ac.update("§d사슬 카운터: " + chain);
			} else {
				ac.update(null);
			}
		}
	};

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	ChainTimer d = new ChainTimer();
	Cooldown c = new Cooldown(cool.getValue(), CooldownDecrease._50);

	public Aris(Participant participant) {
		super(participant);
		passive.register();
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if ((!d.isDuration() || d == null) && chain != 0) {
				ArrayList<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
				if (players.isEmpty()) {
					getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
					return false;
				}
				d.setPlayerList(players);
				d.start();
				d.setCount(chain * 20);
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player t = (Player) e.getEntity();
			if (holding.containsKey(t)) {
				e.setCancelled(true);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (holding.containsKey(e.getPlayer())) e.setCancelled(true);
	}
	
	class ChainTimer extends Duration {
		private List<Player> players;

		public ChainTimer() {
			super(Aris.this.chain*20);
			this.setPeriod(TimeUnit.TICKS, 1);
			this.players = new ArrayList<>();
		}

		@Override
		protected void onDurationStart() {
			passive.stop(false);
			ac.update(null);
			for (Player p : players) {
				ActionbarChannel channel = getGame().getParticipant(p).actionbar().newChannel();
				holding.put(p, Pair.of(p.getLocation().clone().add(0, chain/2.0 + 4, 0), channel));
			}

		}

		@Override
		protected void onDurationProcess(int seconds) {
			for (Player player : holding.keySet()) {
				holding.get(player).getRight().update("고정 지속시간: " + TimeUtil.parseTimeAsString(getFixedCount()));
				player.teleport(holding.get(player).getLeft());
				if (seconds % 40 == 0 && Damages.canDamage(player, getPlayer(), DamageCause.ENTITY_ATTACK, 1) && player.getHealth() > 1) {
					player.setHealth(player.getHealth() - 1);
				}
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			for (Player p : holding.keySet()) {
				holding.get(p).getRight().unregister();
			}
			holding.clear();
			chain = 0;
			c.start();
			passive.start();
		}

		public void setPlayerList(List<Player> list) {
			this.players = list;
		}
	}
}
