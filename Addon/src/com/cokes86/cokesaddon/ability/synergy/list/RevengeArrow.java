package com.cokes86.cokesaddon.ability.synergy.list;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.function.Predicate;

@AbilityManifest(name = "리벤지 애로우", rank = Rank.A, species = Species.OTHERS, explain = {
		"공격을 받을 시 그의 $[multiply]배에 해당하는 대미지를 가진",
		"추가 화살이 자동으로 사출되어 상대방의 위치로 직선으로 나가 공격합니다.",
		"해당 추가화살은 인벤토리의 화살 1개를 소비하여 발사합니다.",
		"추가 화살은 블럭에 닿거나 플레이어가 공격을 받을 시 사라집니다."})
public class RevengeArrow extends CokesSynergy {
	private static final Config<Double> multiply = Config.of(RevengeArrow.class, "배율", 2.2, FunctionalInterfaces.positive());
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

	public RevengeArrow(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			Player damager = null;
			if (e.getDamager() instanceof Player)
				damager = (Player) e.getDamager();
			else if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) {
					damager = (Player) projectile.getShooter();
				}
			}

			if (damager != null) {
				if (getPlayer().getInventory().contains(Material.ARROW)) {
					ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
					Vector vector = damager.getLocation().clone().subtract(getPlayer().getLocation().clone()).toVector()
							.normalize();
					new RevengeArrowBullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0).add(vector.multiply(.25)), vector,
							RGB.of(100, 100, 100), e.getDamage() * multiply.getValue()).start();
				}
			}
		}
	}

	public class RevengeArrowBullet extends AbilityTimer {

		private final Player shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final double damage;

		private final RGB color;
		private Location lastLocation;

		private RevengeArrowBullet(Player shooter, Location startLocation, Vector arrowVelocity, RGB color, double damage) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.normalize().multiply(2);
			this.color = color;
			this.lastLocation = startLocation;
			this.damage = damage;
		}

		public double getDamage() {
			return damage;
		}

		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 40); iterator
					.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				org.bukkit.block.Block block = location.getBlock();
				Material type = block.getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable) && !damageable.isDead()) {
						if (damageable instanceof Player) ((Player) damageable).setNoDamageTicks(0);
						Damages.damageArrow(damageable, getPlayer(), (float) damage);
						stop(false);
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				RevengeArrowBullet.this.stop(false);
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				Player deflectedPlayer = deflector.getPlayer();
				new RevengeArrowBullet(deflectedPlayer, lastLocation, newDirection, color, RevengeArrowBullet.this.getDamage()).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			public Vector getDirection() {
				return RevengeArrowBullet.this.forward.clone();
			}
		}
	}
}
