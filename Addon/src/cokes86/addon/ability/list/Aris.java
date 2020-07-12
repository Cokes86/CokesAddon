package cokes86.addon.ability.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cokes86.addon.utils.LocationPlusUtil;
import daybreak.abilitywar.utils.base.collect.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.DamageUtil;

@AbilityManifest(name = "아리스", rank = Rank.B, species = Species.HUMAN, explain = {
		"5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 10만큼 상승합니다.",
		"철괴 우클릭시 $[range]블럭 이내 모든 플레이어를 공중에 고정시킵니다. $[cool]",
		"공중에는 누적된 §d사슬 카운터§f만큼 초로 환산되어 고정시키며,",
		"(§d사슬 카운터§f/2 + 4)블럭만큼 위로 고정시킵니다.",
		"공중에 고정되어있는 플레이어는 그동안 움직일 수 없으며,",
		"매 2초마다 1의 고정대미지를 입습니다. 그 이외의 대미지는 받지 않습니다.",
		"지속시간과 쿨타임동안 §d사슬 카운터§f는 증가하지 않습니다." })
public class Aris extends AbilityBase implements ActiveHandler {
	private static final Config<Integer> range = new Config<Integer>(Aris.class, "범위", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Aris.class, "쿨타임", 30,1) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	private final Map<Player, Pair<Location, ActionbarChannel>> holding = new HashMap<>();

	int chain = 0;
	ActionbarChannel ac = newActionbarChannel();

	Timer passive = new Timer() {

		@Override
		protected void run(int arg0) {
			if (!c.isRunning()) {
				if (arg0 % (WRECK.isEnabled(getGame()) ? 2 : 5) == 0) {
					chain++;
					if (chain >= 10)
						chain = 10;
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

	DurationTimer d = new ChainTimer(null);
	CooldownTimer c = new CooldownTimer(cool.getValue());

	public Aris(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if ((!d.isDuration() || d == null) && chain != 0) {
				ArrayList<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), LocationPlusUtil.STRICT(getParticipant()));
				if (players.isEmpty()) {
					getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
					return false;
				}
				this.d = new ChainTimer(players);
				d.start();
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
		if (e.getEntity() instanceof Player) {
			Player t = (Player) e.getEntity();
			if (holding.containsKey(t) && e.getDamager().equals(getPlayer())) {
				return;
			}
		}
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (holding.containsKey(e.getPlayer())) e.setCancelled(true);
	}
	
	class ChainTimer extends DurationTimer {
		private final List<Player> players;

		public ChainTimer(List<Player> players) {
			super(Aris.this.chain*20);
			this.setPeriod(TimeUnit.TICKS, 1);
			this.players = players;
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
				if (seconds % 40 == 0
						&& DamageUtil.canDamage(getPlayer(), player, DamageCause.ENTITY_ATTACK, 1)) {
					player.setHealth(Math.max(0.0, player.getHealth() - 1));
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
	}
}