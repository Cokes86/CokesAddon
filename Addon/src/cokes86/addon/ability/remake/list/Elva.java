package cokes86.addon.ability.remake.list;

import cokes86.addon.ability.remake.Remaking;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.function.Predicate;

@AbilityManifest(name = "엘바 [리메이크]", rank = Rank.A, species = Species.OTHERS, explain = {
		"최대 $[maxarrow]개의 마법의 화살을 소지할 수 있습니다.",
		"철괴를 주손에 들고 우클릭할 시, 인벤토리에 보유하고 있는 화살 1개당",
		"$[spawn]개의 마법화살을 생성합니다. 이는 최대치까지 반복합니다.",
		"철괴를 비주류손에 들 시 마법의 화살이 $[speed]틱당 1회씩 전방을 향해 발사됩니다.",
		"마법의 화살은 개당 $[damage]의 근접대미지를 줍니다.",
		"기존 화살을 사용할 경우 대미지가 $[reduce_damage]만큼 감소합니다.",
		"※능력 아이디어: Sato207"
})
public class Elva extends Remaking implements ActiveHandler {
	private static final Config<Integer> maxarrow = new Config<Integer>(Elva.class, "마법화살수", 200) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, speed = new Config<Integer>(Elva.class, "발사속도(틱)", 6) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, spawn = new Config<Integer>(Elva.class, "생성되는_마법화살수", 20) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	private static final Config<Double> damage = new Config<Double>(Elva.class, "마법화살대미지", 2.5) {
		@Override
		public boolean condition(Double value) {
			return value > 0.0;
		}
	}, reduce_damage = new Config<Double>(Elva.class, "화살_대미지_감소치", 1.0) {
		@Override
		public boolean condition(Double value) {
			return value > 0.0;
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
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};
	private final RGB color = RGB.of(1, 255, 102);
	private int arrow = maxarrow.getValue();
	private final ActionbarChannel ac = newActionbarChannel();
	AbilityTimer bow = new AbilityTimer() {
		
		protected void onStart() {
			ac.update("화살 수 : " + arrow + " / " + maxarrow.getValue());
		}

		@Override
		protected void run(int arg0) {
			Vector velocity = getPlayer().getLocation().getDirection().normalize();
			Location startArrow = getPlayer().getLocation().clone().add(velocity.multiply(.25)).add(0, getPlayer().getEyeHeight(), 0);

			ItemStack off = getPlayer().getInventory().getItemInOffHand();

			if (off.getType().equals(Material.IRON_INGOT) && arrow > 0) {
				new Bullet(getPlayer(), startArrow, velocity, color).start();
				arrow -= 1;
				SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
				ac.update("화살 수 : " + arrow + " / " + maxarrow.getValue());
			}		
		}

	}.setPeriod(TimeUnit.TICKS, speed.getValue());

	public Elva(Participant arg0) {
		super(arg0);
		bow.register();
	}

	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			bow.setBehavior(RestrictionBehavior.PAUSE_RESUME).start();
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter() != null && arrow.getShooter().equals(getPlayer())) {
				e.setDamage(e.getDamage() - reduce_damage.getValue());
			}
		}
	}
	 
	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK) {
			int required = maxarrow.getValue() - arrow;
			if (required % spawn.getValue() > 0) {
				int cost = required % spawn.getValue();
				int arrow_amount = 0;
				for (ItemStack item : getPlayer().getInventory().getContents()) {
					if (item.getType() == Material.ARROW) {
						arrow_amount += item.getAmount();
					}
				}
				
				ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, Math.max(arrow_amount, arrow_amount-cost));
				arrow += Math.max(arrow_amount, arrow_amount-cost)*spawn.getValue();
				getPlayer().sendMessage("마법화살 "+Math.max(arrow_amount, arrow_amount-cost)*spawn.getValue()+"개를 생성하였습니다.");
			} else {
				getPlayer().sendMessage("마법화살을 생성할 수 없습니다.");
			}
		}
		return false;
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
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 10); iterator.hasNext(); ) {
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

		public class ArrowEntity extends CustomEntity implements Deflectable {
			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			public void onDeflect(Participant arg0, Vector arg1) {
				Bullet.this.stop(true);
				new Bullet(arg0.getPlayer(), lastLocation, arg1, color).start();
			}
		}
	}
}
