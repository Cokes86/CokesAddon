package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@AbilityManifest(name = "세트", rank = Rank.S, species = Species.GOD, explain = {
		"§7패시브 §8- §c전쟁의 여왕§r: 게임 중 플레이어가 사망할때마다 전쟁스택이 1씩 쌓입니다.",
		"  남아있는 생존자 대비 얻은 전쟁스택에 비례하여",
		"  자신이 상대방에게 주는 대미지가 증가합니다 (최대 $[MAX_DAMAGE] 증가)",
		"§7철괴 우클릭 §8- §c프레셔§r: 자신 기준 $[RANGE]블럭 이내 모든 플레이어를 자신의 위치로 이동시킨 후",
		"  남아있는 생존자 대비 얻은 전쟁스택에 반비례하여 끌어당긴 플레이어의",
		"  주는 대미지가 $[DEBUFF]간 감소하게 됩니다. (최대 $[DEBUFF_MAX] 감소)",
	    "  이 감소수치는 0 아래로 감소하지 않습니다. $[COOL]",
		"  대미지가 감소하고 있는 동안 프레셔를 통해 다시 끌려오지 않습니다."})
public class Seth extends CokesAbility implements ActiveHandler {
	private int max = getGame().getParticipants().size();
	private int kill = 0;
	DecimalFormat df = new DecimalFormat("0.00");
	public static final Config<Integer> MAX_DAMAGE = new Config<Integer>(Seth.class, "추가대미지", 9) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, COOL = new Config<Integer>(Seth.class, "쿨타임", 60, Config.Condition.COOLDOWN) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, DEBUFF = new Config<Integer>(Seth.class, "디버프시간", 5, Config.Condition.TIME) {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	}, RANGE = new Config<Integer>(Seth.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, DEBUFF_MAX = new Config<Integer>(Seth.class, "토벌_최대치", 4) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
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
		}
		return true;
	};

	private final Cooldown cooldown = new Cooldown(COOL.getValue());
	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public Seth(Participant arg0) {
		super(arg0);
	}

	public boolean isPressure(Player player) {
		for (AbstractGame.GameTimer timer : getGame().getRunningTimers()) {
			if (timer instanceof SethGrabTimer) {
				SethGrabTimer sethGrabTimer = (SethGrabTimer) timer;
				if (sethGrabTimer.getGrabbed().contains(player)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			final int range = Seth.RANGE.getValue();
			final List<Player> list = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate);
			if (list.size() > 0) {
				new SethGrabTimer(list);
				cooldown.start();
				return true;
			} else {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
			}
		}
		return false;
	}

	@SubscribeEvent(priority = 99)
	private void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			kill += 1;

			if (getGame() instanceof DeathManager.Handler) {
				final DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
					max -= 1;
				}
			}

			actionbarChannel.update(df.format((double) kill * 100 / max) + "% (" + kill + "/" + max + ")");
		}
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			final double damage = Math.min(MAX_DAMAGE.getValue(), kill * 3 / max);
			e.setDamage(e.getDamage() + damage);
		}
	}

	private class SethGrabTimer extends AbilityTimer implements Listener {
		private final ArrayList<Player> grabbed = new ArrayList<>();
		private final Set<ActionbarChannel> channel = new HashSet<>();
		private final double damage;

		public SethGrabTimer(List<Player> grabbed) {
			super(DEBUFF.getValue());
			grabbed.forEach(player -> {
				if (!isPressure(player)) {
					this.grabbed.add(player);
				}
			});
			this.damage = DEBUFF_MAX.getValue() - (kill / ((double) max) * 2);
			start();
		}

		public List<Player> getGrabbed() {
			return grabbed;
		}

		public void onStart() {
			for (final Player player : grabbed) {
				player.teleport(getPlayer().getLocation());
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(player);
				channel.add(getGame().getParticipant(player).actionbar().newChannel());
			}
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int arg0) {
			channel.forEach(channel -> channel.update("§c신의 프레셔 §f: " + TimeUtil.parseTimeAsString(getFixedCount())));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			channel.forEach(ActionbarChannel::unregister);
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
			if (grabbed.contains(e.getEntity())) {
				e.setDamage(Math.max(0, e.getDamage() - damage));
			}
		}
	}
}
