package cokes86.addon.ability.list;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "프리드", rank = Rank.A, species = Species.HUMAN, explain= {
		"프리드는 마나 100을 가지고 시작하며 5틱마다 1씩 회복합니다. (최대 100)",
		"철괴 우클릭시 일정한 마나를 소모하여, 가장 가까운 플레이어에게 유도 마법구를 2초간 날립니다.",
		"플레이어가 해당 마법구를 맞을 시 효과가 나타납니다.",
		"마법은 발사할 때 마다 랜덤하게 바뀝니다.",
		"§c화상§f: 마나 $[mana_burn]소모, 상대방에게 고정 $[damage_burn]의 대미지를 주고 $[fireTick]틱의 화상효과 부여",
		"§8나약함§f: 마나 $[mana_weakness]소모, 상대방에게 고정 $[damage_weakness]의 대미지를 주고 상대방의 나약함 디버프를 $[weakness_duration]초 부여.",
		"§a폭발§f: 마나 $[mana_explosion]소모, 상대방에게 고정 $[damage_explosion]의 대미지를 주고 $[fuse]의 위력으로 폭발."
})
public class Freud extends AbilityBase implements ActiveHandler {
	private Magic magic;
	private int mana = 100;
	private final ActionbarChannel ac = newActionbarChannel();
	private final Set<UUID> explosion = new HashSet<>();
	
	private static final Config<Integer> mana_burn = new Config<Integer>(Freud.class, "마나소모량.화상", 30) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, mana_weakness = new Config<Integer>(Freud.class, "마나소모량.나약함", 45) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, mana_explosion = new Config<Integer>(Freud.class, "마나소모량.폭발", 80) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, damage_burn = new Config<Integer>(Freud.class, "고정대미지.화상", 2) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, damage_weakness = new Config<Integer>(Freud.class, "고정대미지.나약함", 3) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, damage_explosion = new Config<Integer>(Freud.class, "고정대미지.폭발", 2) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, fireTick = new Config<Integer>(Freud.class, "화상시간(틱)", 50) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	}, weakness_duration = new Config<Integer>(Freud.class, "나약함_지속시간(초)", 3) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	};
	private static final Config<Float> fuse = new Config<Float>(Freud.class, "폭발위력", 0.4f) {
		@Override
		public boolean condition(Float value) {
			return value > 0;
		}
	};

	public Freud(Participant arg0) {
		super(arg0);
		passiveTimer.register();
		magic = Magic.getRandomMagic();
	}

	AbilityTimer passiveTimer = new AbilityTimer() {

		@Override
		protected void run(int arg0) {
			if (mana != 100 && this.getCount() % (WRECK.isEnabled(getGame()) ? 2 : 5) == 0) {
				mana += 1;
			}
			
			ac.update("마나: "+mana+" 마법: "+magic.getName());
		}
		
	};
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
			if (explosion.contains(e.getEntity().getUniqueId())) {
				e.getEntity().getVelocity().setY(0);
				explosion.remove(e.getEntity().getUniqueId());
			}
		}
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
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passiveTimer.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && mana >= magic.getMana()) {
			Player target = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
			if (target != null) {
				mana -= magic.getMana();
				new Bullet(getPlayer(), getPlayer().getLocation(), target, magic).start();
				magic = Magic.getRandomMagic();
				return true;
			}
			getPlayer().sendMessage("근처에 플레이어가 존재하지 않습니다.");
		}
		return false;
	}
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private Vector velocity;
		private final Magic magic;
		private final Entity target;

		private Bullet(LivingEntity shooter, Location startLocation, Entity target, Magic magic) {
			super(40);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			
			Vector first = target.getLocation().clone().subtract(getPlayer().getLocation().clone()).toVector();
			
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX()+first.getX()/0.25, startLocation.getY()+first.getY()/0.25, startLocation.getZ()+first.getZ()/0.25).setBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.velocity = target.getLocation().clone().subtract(getPlayer().getLocation().clone()).toVector().normalize().multiply(0.65);
			this.magic = magic;
			this.lastLocation = startLocation;
			this.target = target;
			
			if (magic == Magic.EXPLOSION) {
				explosion.add(target.getUniqueId());
			}
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			this.velocity = target.getLocation().clone().subtract(lastLocation.clone()).toVector().normalize().multiply(0.65);
			Location newLocation = lastLocation.clone().add(velocity);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 40); iterator.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				Material type = location.getBlock().getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class,entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable) && !damageable.isDead()) {
						magic.onDamaged(damageable, getPlayer());
						if (magic.equals(Magic.EXPLOSION)) {
							damageable.getVelocity().setY(0);
						}
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
				new Bullet(deflectedPlayer, lastLocation, Bullet.this.shooter, magic).start();
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
		FIRE("§c화상§f", mana_burn.getValue(), RGB.of(209,1,1)) {
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_burn.getValue());
				target.damage(damage_burn.getValue());
				target.setFireTicks(fireTick.getValue());
			}
		},
		WEAKNESS("§8나약함§f", mana_weakness.getValue(), RGB.of(85,85,85)){
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_weakness.getValue());
				PotionEffects.WEAKNESS.addPotionEffect((LivingEntity) target, weakness_duration.getValue()*20, 0, false);
			}
		},
		EXPLOSION("§a폭발§f", mana_explosion.getValue(), RGB.of(102,153,1)){
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_explosion.getValue());
				target.getWorld().createExplosion(target.getLocation().clone().add(0, -0.3, 0), fuse.getValue());
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
		protected abstract void onDamaged(Damageable damageable, Player owner);
		
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
