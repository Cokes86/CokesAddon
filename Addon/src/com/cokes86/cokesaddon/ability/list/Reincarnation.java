package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.List;

@AbilityManifest(name = "리인카네이션", rank = Rank.L, species = Species.OTHERS, explain = {
		"자신이 죽을 위기에 처했을 때, 이를 무시하고 체력이 1로 고정됩니다. $[cooldown]",
		"지속시간 $[duration]동안 상대방에게 주는 대미지가 $[damage]% 감소합니다.",
		"지속시간이 종료 시 상대방을 $[hit]번 공격에 성공했을 경우 자신의 체력이 $[respawn] 되어 부활하고,",
		"이후 추가 타격마다 전체 체력의 $[heal]%씩 누적되어 추가적으로 회복합니다.",
		"하지만 타격 횟수를 채우지 못하였을 경우, 사망하게 됩니다.",
		"[아이디어 제공자 §bSato207§f]"
})
public class Reincarnation extends CokesAbility {
	public static final Config<Integer> duration = Config.of(Reincarnation.class, "지속시간", 25, Config.Condition.TIME);
	public static final Config<Integer> cooldown = Config.of(Reincarnation.class, "쿨타임", 600, Config.Condition.COOLDOWN);
	public static final Config<Integer> damage = Config.of(Reincarnation.class, "감소대미지(%)", 50, FunctionalInterfaceUnit.positive());
	public static final Config<Integer> hit = Config.of(Reincarnation.class, "타격횟수", 5, FunctionalInterfaceUnit.positive());
	public static final Config<Integer> heal = Config.of(Reincarnation.class, "회복수치량(%)", 5, FunctionalInterfaceUnit.positive());
	public static final Config<Integer> respawn = Config.of(Reincarnation.class, "회복량", 2, FunctionalInterfaceUnit.positive(),
			FunctionalInterfaceUnit.addJosa(Josa.이가));
	private final ActionbarChannel ac = newActionbarChannel();
	private int hitted = 0;
	private final Cooldown cool = new Cooldown(cooldown.getValue());
	private final AbilityTimer reincarnation = new AbilityTimer(duration.getValue() * 20) {

		protected void onStart() {
			List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, null);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(nearby);
		}

		@Override
		protected void run(int arg0) {
			getPlayer().setHealth(1);
			ac.update("지속 시간: " + TimeUtil.parseTimeAsString(getFixedCount()) + " 히트횟수: "
					+ (hitted >= hit.getValue() ? "§a" + hitted : hitted) + "/ " + hit.getValue());

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
		protected void onEnd() {
			if (hitted >= hit.getValue()) {
				double max_Health = AttributeUtil.getMaxHealth(getPlayer());
				double return_heal = Math.min(max_Health, respawn.getValue() + max_Health * (hitted - hit.getValue()) * heal.getValue() / 100.0);
				getPlayer().setHealth(return_heal);
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
			if (reincarnation.isRunning())
				e.setCancelled(true);
			else if (!reincarnation.isRunning() && getPlayer().getHealth() - e.getFinalDamage() <= 0 && !cool.isRunning() && !e.isCancelled()) {
				e.setDamage(0);
				getPlayer().setHealth(1);
				reincarnation.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}

		Entity damager = e.getDamager();
		if (damager instanceof Projectile) {
			Projectile projectile = (Projectile) damager;
			if (projectile.getShooter() instanceof Entity) {
				damager = (Entity) projectile.getShooter();
			}
		}

		if (damager != null && e.getEntity() instanceof Player && damager.equals(getPlayer())) {
			Player target = (Player) e.getEntity();
			if (reincarnation.isRunning() && getGame().isParticipating(target) && !e.isCancelled()) {
				e.setDamage(e.getDamage() * (damage.getValue() / 100.0D));
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
		if (e.getDamager() == null) return;
		Entity damager = e.getDamager();
		if (damager instanceof Projectile) {
			Projectile projectile = (Projectile) damager;
			if (projectile.getShooter() instanceof Entity) {
				damager = (Entity) projectile.getShooter();
			}
		}

		if (e.getEntity() instanceof Player && damager.equals(getPlayer())) {
			Player target = (Player) e.getEntity();
			if (reincarnation.isRunning() && getGame().isParticipating(target) && !e.isCancelled()) {
				hitted += 1;
				if (hitted == hit.getValue()) {
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
				}
			}
		}
	}
}
