package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.function.Predicate;

@AbilityManifest(name = "엘바", rank = Rank.B, species = Species.OTHERS, explain = {
		"§7활 들고 F키 §8- §a마법의 화살§f: 활이 비주류 손에 있는 한 마법의 화살이 전방으로 발사됩니다.",
		"  이는 $[speed]틱의 간격으로 자동으로 발사되며, $[damage]의 대미지를 줍니다.",
		"  한번에 최대 $[maxarrow]발까지 발사되며, 다 소비할 경우 자동으로 재장전합니다.",
		"  마법의 화살은 대미지를 주거나 블럭에 닿을 시 소멸하며, 플렉터가 튕겨낼 수 없습니다.",
		"  일반 화살은 사용할 수 없습니다.",
		"[아이디어 제공자 §bSato207§f]"
})
public class Elva extends CokesAbility {
	private static final Config<Integer> maxarrow = new Config<>(Elva.class, "마법화살수", 200, PredicateUnit.positive());
	private static final Config<Integer> speed = new Config<>(Elva.class, "발사속도(틱)", 4, PredicateUnit.positive());
	private static final Config<Double> damage = new Config<>(Elva.class, "마법화살대미지", 2.5, PredicateUnit.positive());

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
	private final RGB color = RGB.of(1, 255, 102);
	private int arrow;
	private final ActionbarChannel ac = newActionbarChannel();
	private final AbilityTimer reload = new AbilityTimer(20) {
		ProgressBar progress;

		protected void onStart() {
			progress = new ProgressBar(20, 20);
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
	private final AbilityTimer bow = new AbilityTimer() {

		@Override
		protected void run(int arg0) {
			Vector velocity = getForwardVector(getPlayer().getLocation().clone());
			Location startArrow = getPlayer().getLocation().clone().add(velocity.multiply(.25)).add(0, getPlayer().getEyeHeight(), 0);

			ItemStack off = getPlayer().getInventory().getItemInOffHand();

			if (!reload.isRunning()) {
				if (off.getType().equals(Material.BOW) && arrow > 0) {
					new Bullet(getPlayer(), startArrow, velocity, color).start();
					arrow -= 1;
					SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
				}
				ac.update("화살 수 : " + arrow + " / " + maxarrow.getValue());

				if (arrow == 0)
					reload.start();
			}
		}

	}.setPeriod(TimeUnit.TICKS, speed.getValue());

	public Elva(Participant arg0) {
		super(arg0);
		bow.register();
		reload.register();
		arrow = maxarrow.getValue();
	}

	public static Vector getForwardVector(Location location) {
		float yaw = location.getYaw(), pitch = location.getPitch();

		double radYaw = Math.toRadians(yaw), radPitch = Math.toRadians(pitch);

		double cosPitch = FastMath.cos(radPitch);

		double x = -FastMath.sin(radYaw) * cosPitch;
		double y = -FastMath.sin(radPitch);
		double z = FastMath.cos(radYaw) * cosPitch;

		Vector velocity = new Vector(x, y, z);
		return velocity.normalize();
	}

	@SuppressWarnings("deprecation")
	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && NMS.isArrow(e.getProjectile())) {
			e.setCancelled(true);
			getPlayer().updateInventory();
		}
	}

	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			bow.setBehavior(RestrictionBehavior.PAUSE_RESUME).start();
		}
	}

	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;

		private final RGB color;
		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, RGB color) {
			super(8);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(2);
			this.color = color;
			this.lastLocation = startLocation;
		}

		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 7); iterator.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				Material type = location.getBlock().getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
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

		public class ArrowEntity extends CustomEntity {
			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}
		}
	}
}
