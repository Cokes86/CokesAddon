package cokes86.addon.ability.list;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil.Predicates;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "프리드", rank = Rank.A, species = Species.HUMAN, explain= {
		"프리드는 마나 100을 가지고 시작하며 5틱마다 1씩 회복합니다. (최대 100)",
		"철괴 우클릭시 일정한 마나를 소모하여,",
		"가장 가까운 플레이어에게 유도 마법구를 2초간 날립니다.",
		"플레이어가 해당 마법구를 맞을 시 효과가 나타납니다.",
		"마법은 발사할 때 마다 랜덤하게 바뀝니다.",
		"§c화상§f: 마나 30소모, 상대방에게 고정 2의 대미지를 주고 $[fireTick]틱의 화상효과 부여",
		"§8나약함§f: 마나 45소모, 상대방에게 고정 3의 대미지를 주고 상대방의 나약함 디버프를 3초 부여.",
		"§a폭발§f: 마나 80소모, 상대방에게 고정 4의 대미지를 주고 2F의 위력으로 폭발."
})
public class Freud extends AbilityBase implements ActiveHandler {
	private Magic magic;
	private int mana = 100;
	private ActionbarChannel ac = newActionbarChannel();
	
	private static Config<Integer> fireTick = new Config<Integer>(Freud.class, "화상시간(틱)", 50) {

		@Override
		public boolean Condition(Integer value) {
			return value>0;
		}
	};

	public Freud(Participant arg0) {
		super(arg0);
		magic = Magic.getRandomMagic();
	}
	
	Timer passiveTimer = new Timer() {

		@Override
		protected void run(int arg0) {
			if (mana != 100 && this.getCount() % (WRECK.isEnabled(getGame()) ? 2 : 5) == 0) {
				mana += 1;
			}
			
			ac.update("마나: "+mana+" 마법: "+magic.getName());
		}
		
	};
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			passiveTimer.setPeriod(TimeUnit.TICKS, 1).start();
		default:
		}
	}
	
	@SubscribeEvent
	public void onAbilityRestrictionClear(AbilityRestrictionClearEvent e) {
		passiveTimer.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT)) {
			if (arg1.equals(ClickType.RIGHT_CLICK) && mana >= magic.getMana()) {
				Predicate<Entity> predicate = (Predicate<Entity>) Predicates.STRICT(getPlayer());
				Player target = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
				if (target != null) {
					mana -= magic.getMana();
					new Bullet<>(getPlayer(), getPlayer().getLocation(), target, magic).start();
					magic = Magic.getRandomMagic();
					return true;
				}
				getPlayer().sendMessage("근처에 플레이어가 존재하지 않습니다.");
			}
		}
		return false;
	}
	
	public class Bullet<Shooter extends Entity & ProjectileSource> extends Timer {

		private final Shooter shooter;
		private final CustomEntity entity;
		private final Vector velocity;
		private final Magic magic;

		private Bullet(Shooter shooter, Location startLocation, Entity target, Magic magic) {
			super(40);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			
			Vector first = target.getLocation().clone().subtract(getPlayer().getLocation().clone()).toVector();
			
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX()+first.getX()/0.25, startLocation.getY()+first.getY()/0.25, startLocation.getZ()+first.getZ()/0.25).setBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.velocity = target.getLocation().clone().subtract(getPlayer().getLocation().clone()).toVector().normalize().multiply(0.045);
			this.magic = magic;
			this.lastLocation = startLocation;
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(velocity);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 40); iterator.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				for (Damageable damageable : LocationUtil.getConflictingDamageables(entity.getBoundingBox())) {
					if (!shooter.equals(damageable) && !damageable.isDead()) {
						magic.onDamaged(damageable);
						stop(false);
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, magic.getRGB());
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
				new Bullet<>(deflectedPlayer, lastLocation, Bullet.this.shooter, magic).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			public Vector getDirection() {
				return Freud.Bullet.this.velocity.clone();
			}

		}

	}
	
	enum Magic {
		FIRE("§c화상§f", 30, new RGB(209,0,0)) {
			protected void onDamaged(Damageable target) {
				target.damage(2);
				target.setFireTicks(fireTick.getValue());
			}
		},
		WEAKNESS("§8나약함§f", 45, new RGB(85,85,85)){
			protected void onDamaged(Damageable target) {
				target.damage(3);
				PotionEffects.WEAKNESS.addPotionEffect((LivingEntity) target, 3*20, 0, false);
			}
		},
		EXPLOSION("§a폭발§f", 80, new RGB(102,153,0)){
			protected void onDamaged(Damageable target) {
				target.damage(4);
				target.getWorld().createExplosion(target.getLocation().clone().add(0, -0.25, 0), 2F);
			}
		};
		
		Magic(String name, int mana, RGB magicColor) {
			this.name = name;
			this.mana = mana;
			this.magicColor = magicColor;
		}
		
		int mana;
		RGB magicColor;
		String name;
		protected abstract void onDamaged(Damageable damageable);
		
		public int getMana() {
			return mana;
		}
		
		public RGB getRGB() {
			return magicColor;
		}
		
		public String getName() {
			return name;
		}
		
		public static Magic getRandomMagic() {
			Random r = new Random();
			int a = r.nextInt(3);
			switch(a) {
			case 0: return Magic.FIRE;
			case 1: return Magic.WEAKNESS;
			case 2: return Magic.EXPLOSION;
			default: return null;
			}
		}
	}

}
