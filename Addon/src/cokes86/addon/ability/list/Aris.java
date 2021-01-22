package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
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
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@AbilityManifest(name = "아리스", rank = Rank.B, species = Species.HUMAN, explain = {
		"§7패시브 - §c체인§r: 5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 $[max_count]만큼 상승합니다.",
		"  엑티브 지속시간, 쿨타임 동안은 §d사슬 카운터§f가 증가하지 않는다.",
		"§7철괴 우클릭 - §c사슬무덤§r: $[range]블럭 이내 모든 플레이어를 공중에 고정시킵니다. $[cool]",
		"  공중에는 누적된 §d사슬 카운터§f만큼 초로 환산되어 고정시키며,",
		"  (§d사슬 카운터§f/2 + 4)블럭만큼 위로 고정시킵니다.",
		"  공중에 고정되어있는 플레이어는 능력사용불능, 행동불능상태에 들어가며,",
		"  매 2초마다 1의 고정대미지를 입습니다. 그 이외의 대미지는 받지 않으며,",
		"  고정대미지는 체력이 1 이하인 상태에서는 받지 않습니다."})
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
	private final ActiveTimer activeTimer = new ActiveTimer();
	private final Cooldown cooldown = new Cooldown(cool.getValue(), CooldownDecrease._50);
	private final AbilityTimer passive = new AbilityTimer() {
		final int count = (int) (Wreck.isEnabled(getGame()) ? 5 * Wreck.calculateDecreasedAmount(50) : 5);

		@Override
		protected void run(int arg0) {
			if (!cooldown.isRunning() && !activeTimer.isDuration(false)) {
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
	};

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
		if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK) && !activeTimer.isDuration(true) && chain != 0) {
			final List<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
			if (players.isEmpty()) {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				return false;
			}
			activeTimer.start(players);
			return true;
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		Entity attacker = e.getDamager();
		if (attacker instanceof Projectile) {
			Projectile projectile = (Projectile) attacker;
			if (projectile.getShooter() instanceof Player) {
				attacker = (Entity) projectile.getShooter();
			}
		}

		if (attacker instanceof Player) {
			if (activeTimer.isDuration(false) && activeTimer.getGrabbedPlayer().contains(attacker)) {
				e.setCancelled(true);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			if (activeTimer.isDuration(false) && activeTimer.getGrabbedPlayer().contains(e.getEntity())) {
				e.setCancelled(true);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (activeTimer.isDuration(false) && activeTimer.getGrabbedPlayer().contains(e.getPlayer())) {
			e.getPlayer().setVelocity(new Vector(0,0,0));
		}
	}

	@SubscribeEvent
	public void onActiveSkill(AbilityPreActiveSkillEvent e) {
		if (activeTimer.isDuration(false) && activeTimer.getGrabbedPlayer().contains(e.getPlayer())) {
			e.setCancelled(true);
		}
	}
	
	class ActiveTimer {
		private int duration;
		private UpTimer up;
		private ArisGrabTimer grab;
		private List<Player> list;
		private List<ActionbarChannel> channels;

		public boolean start(List<Player> list) {
			list.forEach(player -> player.setGravity(false));
			this.duration = chain;
			this.up = new UpTimer();
			this.grab = new ArisGrabTimer();
			this.list = list;
			this.channels = new ArrayList<>();
			list.forEach(player -> {
				Participant p = getGame().getParticipant(player);
				channels.add(p.actionbar().newChannel());
			});
			channels.forEach(channel -> channel.update("붙잡힘!!"));
			return up.start();
		}
		
		public boolean stop(boolean silent) {
			return up.stop(silent) || grab.stop(silent);
		}

		public List<Player> getGrabbedPlayer() {
			return list;
		}
		
		public boolean isDuration(boolean message) {
			if (up != null && grab != null) {
				return up.isRunning() || (message ? grab.isDuration() : grab.isRunning());
			}
			return false;
		}

		public void onStop() {
			list.forEach(player -> player.setGravity(true));
			list.clear();
			channels.forEach(ActionbarChannel::unregister);
			channels.clear();
			chain = 0;
		}
		
		class UpTimer extends AbilityTimer {
			public UpTimer() {
				super(duration/2 + 4);
				this.setPeriod(TimeUnit.TICKS, 1);
			}
			
			@Override
			protected void run(int count) {
				for (Player player : list) {
					Location location = player.getLocation().clone();
					LocationIterator line = Line.iteratorBetween(location, location.add(0, 1, 0), 200);
					for (Location line_location : line.iterable()) {
						if(line_location.clone().add(0, 1.4, 0).getBlock().getType() == Material.AIR) {
							player.teleport(line_location);
						}
					}
				}

			}
			
			protected void onSilentEnd() {
				onStop();
			}
			
			protected void onEnd() {
				grab.start();
			}
		}
		
		class ArisGrabTimer extends Duration {

			public ArisGrabTimer() {
				super(chain*20, cooldown);
				setPeriod(TimeUnit.TICKS, 1);
			}
			
			Map<Player, Location> locations = new HashMap<>();
			
			protected void onStart() {
				list.forEach(player -> locations.put(player, player.getLocation().clone()));
			}

			@Override
			protected void onDurationProcess(int arg0) {
				list.forEach(player -> {
					Location l = player.getLocation().clone();
					player.teleport(l);
					if (arg0 % 40 == 0) {
						Healths.setHealth(player, player.getHealth() - 1);
					}
				});
			}
			
			protected void onDurationSilentEnd() {
				onStop();
			}
			
			protected void onDurationEnd() {
				onStop();
			}
		}
	}
}
