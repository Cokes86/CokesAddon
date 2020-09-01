package cokes86.addon.ability.list;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "세트", rank = Rank.S, species = Species.GOD, explain = {
		"패시브 - 전쟁의 여왕: 게임 중 플레이어가 사망할때마다 전쟁스택이 1씩 쌓입니다.",
		"  남아있는 생존자 대비 얻은 전쟁스택에 비례하여",
		"  자신이 상대방에게 주는 대미지가 증가합니다 (최대 $[max_damage] 증가)",
		"철괴 우클릭 - 토벌: 자신 기준 $[range]블럭 이내 모든 플레이어를 자신의 위치로 이동시킨 후",
		"  남아있는 생존자 대비 얻은 전쟁스택에 반비례하여 끌어당긴 플레이어의",
		"  주는 대미지가 $[debuff]간 감소하게 됩니다. (최대 $[debuff_max] 증가) $[cool]"})
public class Seth extends CokesAbility implements ActiveHandler {
	int max = getGame().getParticipants().size();
	int kill = 0;
	DecimalFormat df = new DecimalFormat("0.00");
	public static Config<Integer> max_damage = new Config<Integer>(Seth.class, "추가대미지", 9) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, cool = new Config<Integer>(Seth.class, "쿨타임", 60, 1) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, debuff = new Config<Integer>(Seth.class, "디버프시간", 5, 2) {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	}, range = new Config<Integer>(Seth.class, "범위", 7) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, debuff_max = new Config<Integer>(Seth.class, "토벌_최대치", 4) {
		@Override
		public boolean condition(Integer value) {
			return value>=0;
		}
	};

	Cooldown c = new Cooldown(cool.getValue());

	ActionbarChannel ac = newActionbarChannel();

	public Seth(Participant arg0) {
		super(arg0);
	}

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

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				int range = Seth.range.getValue();
				List<Player> ps = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate);
				if (ps.size() > 0) {
					new GrapTimer(ps);
					c.start();
					return true;
				} else {
					getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				}
			}
		}
		return false;
	}

	@SubscribeEvent(priority = 99)
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			kill += 1;

			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
					max -= 1;
				}
			}
			
			ac.update(df.format((double) kill * 100 / max) + "% (" + kill + "/" + max + ")");
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			final double damage = Math.max(9, kill*3/max);
			e.setDamage(e.getDamage() + damage);
		}
	}
	
	class GrapTimer extends AbilityTimer implements Listener {
		private final List<Player> grapped;
		private final Set<ActionbarChannel> channel = new HashSet<>();
		private final double damage;
		
		public GrapTimer(List<Player> grapped) {
			super(debuff.getValue());
			this.grapped = grapped;
			this.damage = debuff_max.getValue() - (kill/max*2);
			start();
		}
		
		public void onStart() {
			for (Player p : grapped) {
				p.teleport(getPlayer().getLocation());
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(p);
				channel.add(getGame().getParticipant(p).actionbar().newChannel());
			}
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int arg0) {
			channel.forEach(channel -> channel.update("신의 프레셔 : "+TimeUtil.parseTimeAsString(getFixedCount())));
		}
		
		@Override
		protected void onSilentEnd() {
			channel.forEach(channel -> channel.unregister());
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			if (grapped.contains(e.getDamager())) {
				e.setDamage(Math.max(0, e.getDamage() - damage));
			}
		}
	}
}
