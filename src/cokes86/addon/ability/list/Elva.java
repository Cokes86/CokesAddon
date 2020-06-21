package cokes86.addon.ability.list;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.AbstractGame.RestrictionBehavior;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "엘바", rank = Rank.A, species = Species.OTHERS, explain = { "활을 비주류 손에 들고 있을 경우",
		"마법의 화살이 전방으로 $[speed]틱 간격으로 자동으로 발사되며 최대 $[maxarrow]발까지 발사됩니다.",
		"마법의 화살은 $[damage]의 대미지를 주며, 화살을 다 소비할 시 재장전합니다.", "기존의 화살은 사용할 수 없습니다.", "마법의 화살은 대미지를 주거나, 블럭에 닿을 시 소멸합니다.",
		"※능력 아이디어: Sato207" })
public class Elva extends AbilityBase {

	Vector velocity = null;
	RGB color = new RGB(0, 255, 102);
	int arrow;
	ActionbarChannel ac = newActionbarChannel();
	double[] location;

	private static Config<Integer> maxarrow = new Config<Integer>(Elva.class, "마법화살수", 200) {

		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}

	};

	private static SettingObject<Integer> speed = new Config<Integer>(Elva.class, "발사속도(틱)", 4) {

		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}

	};

	private static SettingObject<Double> damage = new Config<Double>(Elva.class, "마법화살대미지", 3.0) {

		@Override
		public boolean Condition(Double value) {
			return value > 0.0;
		}

	};

	public Elva(Participant arg0) {
		super(arg0);
		arrow = maxarrow.getValue();
	}

	Timer bow = new Timer() {

		@Override
		protected void run(int arg0) {
			Location playerLocation = getPlayer().getLocation().clone();

			float playerYaw = playerLocation.getYaw(), playerPitch = playerLocation.getPitch();

			double radYaw = Math.toRadians(playerYaw), radPitch = Math.toRadians(playerPitch);

			double cosPitch = Math.cos(radPitch);

			double x = -Math.sin(radYaw) * cosPitch;
			double y = -Math.sin(radPitch);
			double z = Math.cos(radYaw) * cosPitch;

			velocity = new Vector(x, y, z);
			velocity.normalize();
			Location startArrow = playerLocation.add(velocity.multiply(.25)).add(0, getPlayer().getEyeHeight(), 0);

			ItemStack off = getPlayer().getInventory().getItemInOffHand();

			if (!reload.isRunning()) {
				if (off.getType().equals(Material.BOW) && arrow > 0) {
					new Bullet<>(getPlayer(), startArrow, velocity, color).start();
					arrow -= 1;
					SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
				}
				ac.update("화살 수 : " + arrow + " / " + maxarrow.getValue());

				if (arrow == 0)
					reload.start();
			}
		}

	}.setPeriod(TimeUnit.TICKS, speed.getValue());

	Timer reload = new Timer(10) {
		ProgressBar progress;

		protected void onStart() {
			progress = new ProgressBar(10, 10);
		}

		@Override
		protected void run(int arg0) {
			progress.step();
			ac.update("재장전 중 : " + progress.toString());
		}

		protected void onEnd() {
			onSilentEnd();
		}

		protected void onSilentEnd() {
			arrow = maxarrow.getValue();
		}

	}.setPeriod(TimeUnit.TICKS, 4);

	@SubscribeEvent(onlyRelevant = true)
	public void onAbilityRestrictionClear(AbilityRestrictionClearEvent e) {
		bow.setBehavior(RestrictionBehavior.PAUSE_RESUME).start();
	}

	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && e.getProjectile() instanceof Arrow) {
			e.setCancelled(true);
			getPlayer().updateInventory();
		}
	}
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			bow.setBehavior(RestrictionBehavior.PAUSE_RESUME).start();
			break;
		default:
		}
	}

	public class Bullet<Shooter extends Entity & ProjectileSource> extends Timer {

		private final Shooter shooter;
		private final CustomEntity entity;
		private final Vector forward;

		private final RGB color;

		private Bullet(Shooter shooter, Location startLocation, Vector arrowVelocity, RGB color) {
			super(8);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).setBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(2);
			this.color = color;
			this.lastLocation = startLocation;
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 40); iterator
					.hasNext();) {
				Location location = iterator.next();
				entity.setLocation(location);
				org.bukkit.block.Block block = location.getBlock();
				Material type = block.getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingDamageables(entity.getBoundingBox())) {
					if (!shooter.equals(damageable) && !damageable.isDead()) {
						damageable.damage(damage.getValue(), shooter);
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
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				Player deflectedPlayer = deflector.getPlayer();
				new Bullet<>(deflectedPlayer, lastLocation, newDirection, color).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			public Vector getDirection() {
				return Elva.Bullet.this.forward.clone();
			}

		}

	}
}
