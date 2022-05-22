package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.function.Predicate;

@AbilityManifest(name = "장미의 유혹", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §c가시돋움§f: 상대방을 공격할 시 가시 카운터를 1씩 올리고,",
		"  (가시 카운터*0.125)의 추가대미지를 줍니다. (최대 5대미지)",
		"  또한 모든 받는 대미지가 (가시 카운터)%만큼 증가합니다. (최대 50%)",
		"§7쿨타임 패시브 §8- §c유혹§f: 쿨타임이 아닐 때 자신이 죽을 위기에 처할 시",
		"  모든 가시 카운터를 소비하고 반경 10블럭 이내 모든 플레이어에게 (가시 카운터*0.5)초의",
		"  블라인드 효과를 준 후 지속시간동안 자신은 무적상태가 되며,",
		"  0.5초마다 채력을 1씩 회복합니다. $[cool]"
})
public class LureOfRoses extends CokesSynergy {
	private static final Config<Integer> cool = Config.of(LureOfRoses.class, "쿨타임", 300, Config.Condition.COOLDOWN);

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				return !game.getDeathManager().isExcluded(entity.getUniqueId());
			}
		}
		return true;
	};
	private int counter = 0;
	private final Cooldown cooldown = new Cooldown(cool.getValue());
	private final InvTimer duration = new InvTimer();

	public LureOfRoses(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	public void onCEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 + Math.min(counter, 50.0) / 100));
			if (duration.isRunning()) {
				e.setCancelled(true);
			} else if (!cooldown.isRunning() && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				e.setDamage(0);
				getPlayer().setHealth(1);
				duration.start(counter);
				counter = 0;
			}
		}

		if (e.getDamager() == null) return;
		Entity attacker = e.getDamager();
		if (attacker instanceof Projectile) {
			Projectile entity = (Projectile) attacker;
			if (entity.getShooter() instanceof Entity) {
				attacker = (Entity) entity.getShooter();
			}
		}
		if (attacker.equals(getPlayer()) && e.getEntity() instanceof Player && getGame().getParticipant((Player) e.getEntity()) != null) {
			counter += 1;
			e.setDamage(e.getDamage() + Math.min(counter * 0.125, 5));
		}
	}

	class InvTimer {
		Duration duration;

		public boolean start(int count) {
			duration = new Duration(count, cooldown) {
				@Override
				protected void onDurationProcess(int arg0) {
					for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
						PotionEffects.BLINDNESS.addPotionEffect(p, 50, 0, true);
					}
					getPlayer().setHealth(Math.min(AttributeUtil.getMaxHealth(getPlayer()), getPlayer().getHealth() + 1));
				}

			}.setPeriod(TimeUnit.TICKS, 10);
			return duration.start();
		}

		public boolean isDuration() {
			return duration != null && duration.isDuration();
		}

		public boolean isRunning() {
			return duration != null && duration.isRunning();
		}
	}
}
