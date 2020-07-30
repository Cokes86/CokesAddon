package cokes86.addon.ability.synergy;

import cokes86.addon.configuration.synergy.Config;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Predicate;

@AbilityManifest(name = "공중 분해", rank = Rank.A, species = Species.HUMAN, explain = {
		"5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 8만큼 상승합니다.",
		"철괴 우클릭시 §d사슬 카운터§f를 전부 소모하여 플레이어를 (§d사슬 카운터§f/2+5)블럭만큼 공중에 고정시킨 후",
		"3틱마다 고정된 플레이어 중 한명에게 이동하고 누적된 (§d사슬 카운터§f*2)만큼의 고정대미지를 준 후 떨어트립니다. $[cool]",
		"능력 사용 이후 1회에 한정해 낙하대미지를 받지 않습니다."
})
public class AirDisintegration extends Synergy implements ActiveHandler {
	public static Config<Integer> range = new Config<Integer>(AirDisintegration.class, "범위", 7) {
		@Override
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	}, cool = new Config<Integer>(AirDisintegration.class, "쿨타임", 15, 1) {
		@Override
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	};

	int chain = 0;
	ActionbarChannel ac = newActionbarChannel();
	private final Predicate<Entity> STRICT_PREDICATE = entity -> {
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
	private LinkedList<LivingEntity> entities = null;
	boolean falling = false;
	Cooldown cooldown = new Cooldown(cool.getValue());

	AbilityTimer passive = new AbilityTimer() {

		@Override
		protected void run(int arg0) {
			if (!cooldown.isRunning()) {
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
	
	private final AbilityTimer skill = new AbilityTimer() {
		final Map<LivingEntity, Location> stun = new HashMap<>();
		
		public void onStart() {
			passive.stop(false);
			for (LivingEntity entity : entities) {
				stun.put(entity, entity.getLocation().clone().add(0, chain/2.0 + 5, 0));
			}
		}

		@Override
		public void run(int count) {
			if (entities != null) {
				if (!entities.isEmpty()) {
					for (LivingEntity entity : entities) {
						entity.teleport(stun.get(entity));
					}
					if (count % 3 == 0) {
						LivingEntity e = entities.remove();
						getPlayer().teleport(e);
						e.damage(chain*2, getPlayer());
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
						SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
						stun.remove(e);
					}
				} else {
					falling = true;
					chain = 0;
					cooldown.start();
					passive.start();
					stop(false);
				}
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	public AirDisintegration(Participant participant) {
		super(participant);
		passive.register();
		skill.register();
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && chain > 0 && !cooldown.isCooldown()) {
			this.entities = new LinkedList<>(LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), range.getValue(), range.getValue(), STRICT_PREDICATE));
			if (entities.size() > 0) {
				skill.start();
				return true;
			} else {
				getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&f" + range.getValue() + "칸 이내에 &a엔티티&f가 존재하지 않습니다."));
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause().equals(DamageCause.FALL) && falling && e.getEntity().equals(getPlayer())) {
			falling = false;
			e.setCancelled(true);
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		} else if (e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
			if (entities != null && entities.contains(entity)) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
			if (entities != null && entities.contains(entity) && !e.getDamager().equals(getPlayer())) {
				e.setCancelled(true);
			}
		}
	}
}
