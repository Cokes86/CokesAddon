package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.timer.InvincibilityTimer;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.List;

@AbilityManifest(name = "리인카네이션", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §5환생§f: 치명적인 대미지를 입었을 시, 이를 무시하고 체력이 1로 고정됩니다.",
		"  $[DURATION]동안 상대에게 주는 대미지가 0으로 바뀌는 대신",
		"  $[HIT_PREDICATE]번 이상 공격에 성공했을 경우 §b부활합니다.",
		"  §7부활 체력: $[RESPAWN_HEALTH] + 최대 체력의 $[RESPAWN_PERCENTAGE]% × 초과 타격횟수",
		"[아이디어 제공자 §bSato207§f]"
})
public class Reincarnation extends CokesAbility {
	public static final Config<Integer> DURATION = Config.of(Reincarnation.class, "duration", 20, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
			"# 환생 지속시간",
			"# 기본값: 20 (초)");
	public static final Config<Integer> COOLDOWN = Config.of(Reincarnation.class, "cooldown", 600, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
			"# 환생 쿨타임",
			"# 기본값: 600 (초)");
	public static final Config<Integer> HIT_PREDICATE = Config.of(Reincarnation.class, "hit-predicate", 5, FunctionalInterfaces.positive(),
			"# 환생 중 공격 횟수 조건",
			"# 기본값: 5 (회)");
	public static final Config<Double> RESPAWN_PRECENTAGE = Config.of(Reincarnation.class, "respawn-health-percentage", 5d, FunctionalInterfaces.positive(),
			"# 환생 성공 시 회복하는 최대 체력의 비율",
			"# 기본값: 5.0 (%)");
	public static final Config<Double> RESPAWN_HEALTH = Config.of(Reincarnation.class, "respawn-health", 2d, FunctionalInterfaces.positive(),
			"# 환생 성공 시 회복하는 고정 체력",
			"# 기본값: 2");
	private final ActionbarChannel ac = newActionbarChannel();
	private int hitted = 0;
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final AbilityTimer reincarnation = new InvincibilityTimer(getParticipant(), DURATION.getValue() * 20) {

		public void onInvincibilityStart() {
			List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, null);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(nearby);
		}

		@Override
		public void onInvincibilityRun(int arg0) {
			getPlayer().setHealth(1);
			ac.update((hitted >= HIT_PREDICATE.getValue() ? "§a" + hitted : hitted) + "/ " + HIT_PREDICATE);

			if (arg0 % 5 == 0) {
				for (Location l : Circle.iteratorOf(getPlayer().getLocation(), 3, 3 * 6).iterable()) {
					l.setY(LocationUtil.getFloorYAt(getPlayer().getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.1);
					ParticleLib.REDSTONE.spawnParticle(l, new RGB(140, 2, 120));

					l.setY(LocationUtil.getFloorYAt(getPlayer().getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.8);
					ParticleLib.REDSTONE.spawnParticle(l, new RGB(140, 2, 120));
				}
			}
		}

		@Override
		public void onInvincibilityEnd() {
			if (hitted >= HIT_PREDICATE.getValue()) {
				double max_Health = AttributeUtil.getMaxHealth(getPlayer());
				double return_heal = Math.min(max_Health, RESPAWN_HEALTH.getValue() + max_Health * (hitted - HIT_PREDICATE.getValue()) * RESPAWN_PRECENTAGE.getValue() / 100.0);
				getPlayer().setHealth(return_heal);
				SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
			} else {
				getPlayer().setHealth(0);
			}
			hitted = 0;
			cool.start();
			ac.update(null);
		}
	};

	public Reincarnation(Participant arg0) {
		super(arg0);
		reincarnation.register();
	}

	@SubscribeEvent(priority = 6)
	public void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (reincarnation.isRunning()) e.setCancelled(true);
			else if (!reincarnation.isRunning() && e.getHealth() <= 0 && !cool.isRunning() && !e.isCancelled()) {
				e.setCancelled(true);
				getPlayer().setHealth(1);
				reincarnation.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (!reincarnation.isRunning() && getPlayer().getHealth() - e.getFinalDamage() <= 0 && !cool.isRunning() && !e.isCancelled()) {
				e.setDamage(0);
				getPlayer().setHealth(1);
				reincarnation.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}

		Entity damager = CokesUtil.getDamager(e.getDamager());
		if (damager != null && e.getEntity() instanceof Player && damager.equals(getPlayer())) {
			Player target = (Player) e.getEntity();
			if (reincarnation.isRunning() && getGame().isParticipating(target) && !e.isCancelled()) {
				e.setDamage(0);
			}
		}
	}

	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && reincarnation.isRunning()) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent(priority = 999, eventPriority = EventPriority.MONITOR)
	public void onEntityDamage2(CEntityDamageEvent e) {
		Entity damager = CokesUtil.getDamager(e.getDamager());

		if (damager != null && e.getEntity() instanceof Player && damager.equals(getPlayer())) {
			Player target = (Player) e.getEntity();
			if (reincarnation.isRunning() && getGame().isParticipating(target) && !e.isCancelled()) {
				hitted += 1;
				if (hitted == HIT_PREDICATE.getValue()) {
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
				} else if (hitted == HIT_PREDICATE.getValue()/5 || hitted == HIT_PREDICATE.getValue()*2/5 || hitted == HIT_PREDICATE.getValue()*3/5 || hitted == HIT_PREDICATE.getValue()*4/5) {
					SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
				}
			}
		}
	}
}
