package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@AbilityManifest(name = "프리드", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b마나§f: 프리드는 고유 자원인 §b마나§f가 존재합니다.",
		"  $[MANA_REGAIN_TIME]틱마다 마나가 1씩 회복하며, 최대 100까지 증가합니다.",
		"§7철괴 우클릭 §8- §c엘리멘탈 서클§f: 특수한 효과를 가진 유도 발사체를",
		"  가장 가까운 플레이어에게 $[ELEMENTAL_CIRCLE_DURATION]간 발사됩니다.",
		"  대미지와 효과는 발사 후 무작위로 바뀝니다.",
		"    §c화상§f: $[damage_burn]의 관통 대미지를 주고 $[fireTick]틱의 화상효과 부여. $[mana_burn]",
		"    §8나약함§f: $[damage_weakness]의 관통 대미지를 주고 상대방의 나약함 디버프를 $[weakness_duration]초 부여. $[mana_weakness]",
		"    §a폭발§f: $[damage_explosion]의 관통 대미지를 주고 $[fuse]의 위력으로 폭발. $[mana_explosion]"
})
public class Freud extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> mana_burn = Config.of(Freud.class, "fire-magic-cost", 30, FunctionalInterfaces.positive(),
			FunctionalInterfaces.prefix("§c소모 §7: §b"));
	private static final Config<Integer> mana_weakness = Config.of(Freud.class, "weakness-magic-cost", 45, FunctionalInterfaces.positive(),
			FunctionalInterfaces.prefix("§c소모 §7: §b"));
	private static final Config<Integer> mana_explosion = Config.of(Freud.class, "explosion-magic-cost", 80, FunctionalInterfaces.positive(),
			FunctionalInterfaces.prefix("§c소모 §7: §b"));
	private static final Config<Integer> damage_burn = Config.of(Freud.class, "fire-damage", 2, FunctionalInterfaces.positive());
	private static final Config<Integer> damage_weakness = Config.of(Freud.class, "weakness-damage", 3, FunctionalInterfaces.positive());
	private static final Config<Integer> damage_explosion = Config.of(Freud.class, "explosion-damage", 2, FunctionalInterfaces.positive());
	private static final Config<Integer> fireTick = Config.of(Freud.class, "fire-duration", 50, FunctionalInterfaces.positive());
	private static final Config<Integer> weakness_duration = Config.of(Freud.class, "weakness-duration", 3, FunctionalInterfaces.positive());
	private static final Config<Double> fuse = Config.of(Freud.class, "explosion-fuse", 0.4, FunctionalInterfaces.positive());
	private static final Config<Integer> MANA_REGAIN_TIME = Config.of(Freud.class, "mana-regain-period", 5, FunctionalInterfaces.positive());
	private static final Config<Integer> ELEMENTAL_CIRCLE_DURATION = Config.of(Freud.class, "elemental-circle-duration", 2, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);

	private final Set<UUID> explosion = new HashSet<>();

	private final Predicate<Entity> predicate = entity -> {
		if (entity == null || entity.equals(getPlayer())) return false;
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
	private Magic magic;
	private int mana = 100;
	private final AbilityTimer passiveTimer = new AbilityTimer() {
		BossBar bar;
		final String mana_info = "§b마나";
		final String mana_using_info = "§b§l마나";

		@Override
		protected void onStart() {
			bar = Bukkit.createBossBar(mana >= magic.getMana() ? mana_using_info : mana_info, magic.getBarColor(), BarStyle.SEGMENTED_10);
			bar.addPlayer(getPlayer());
			bar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			bar.removeAll();
		}

		@Override
		protected void onSilentEnd() {
			bar.removeAll();
		}

		@Override
		protected void run(int arg0) {
			if (mana != 100 && this.getCount() % (MANA_REGAIN_TIME.getValue()/(Wreck.isEnabled(getGame()) ? 2 : 1)) == 0) {
				mana += 1;
			}
			bar.setColor(magic.getBarColor());
			bar.setProgress(mana / 100.0);
			bar.setTitle(mana >= magic.getMana() ? mana_using_info : mana_info);
		}
	};

	public Freud(Participant arg0) {
		super(arg0);
		passiveTimer.register();
		magic = Magic.getRandomMagic();
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
			if (explosion.contains(e.getEntity().getUniqueId())) {
				e.getEntity().getVelocity().setY(0);
				explosion.remove(e.getEntity().getUniqueId());
			}
		}
	}

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
				new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0,1.5,0), target, magic).start();
				magic = Magic.getRandomMagic();
				return true;
			}
			getPlayer().sendMessage("근처에 플레이어가 존재하지 않습니다.");
		}
		return false;
	}

	enum Magic {
		FIRE("§c화상§f", mana_burn.getValue(), RGB.of(209, 1, 1), BarColor.RED) {
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_burn.getValue());
				target.setFireTicks(fireTick.getValue());
			}
		},
		WEAKNESS("§8나약함§f", mana_weakness.getValue(), RGB.of(85, 85, 85), BarColor.WHITE) {
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_weakness.getValue());
				PotionEffects.WEAKNESS.addPotionEffect((LivingEntity) target, weakness_duration.getValue() * 20, 0, false);
			}
		},
		EXPLOSION("§a폭발§f", mana_explosion.getValue(), RGB.of(102, 153, 1), BarColor.GREEN) {
			protected void onDamaged(Damageable target, Player owner) {
				Damages.damageFixed(target, owner, damage_explosion.getValue());
				target.getWorld().createExplosion(target.getLocation().clone().add(0, -0.3, 0), fuse.getValue().floatValue());
			}
		};

		private final int mana;
		private final RGB magicColor;
		private final String name;
		private final BarColor bar;

		Magic(String name, int mana, RGB magicColor, BarColor bar) {
			this.name = name;
			this.mana = mana;
			this.magicColor = magicColor;
			this.bar = bar;
		}

		public static Magic getRandomMagic() {
			final Random random = new Random();
			return random.pick(values());
		}

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

		public BarColor getBarColor() {
			return bar;
		}
	}

	public class Bullet extends AbilityTimer implements Listener {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Magic magic;
		private final Entity target;
		private Vector velocity;
		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Entity target, Magic magic) {
			super(ELEMENTAL_CIRCLE_DURATION.getValue()*20);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;

			Vector first = target.getLocation().clone().add(0, 1.5,0).subtract(getPlayer().getLocation().clone().add(0, 1.5,0)).toVector();

			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX() + first.getX() / 0.25, startLocation.getY() + first.getY() / 0.25, startLocation.getZ() + first.getZ() / 0.25).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.velocity = target.getLocation().clone().add(0, 1.5,0).subtract(getPlayer().getLocation().clone().add(0, 1.5,0)).toVector().normalize().multiply(0.65);
			this.magic = magic;
			this.lastLocation = startLocation;
			this.target = target;

			if (magic == Magic.EXPLOSION) {
				explosion.add(target.getUniqueId());
			}
		}

		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int i) {
			this.velocity = target.getLocation().clone().add(0, 1.5,0).subtract(lastLocation.clone()).toVector().normalize().multiply(0.65);
			Location newLocation = lastLocation.clone().add(velocity);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 40); iterator.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				Material type = location.getBlock().getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
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
			HandlerList.unregisterAll(this);
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			if (this.isRunning() && e.getEntity().equals(target)) {
				stop(true);
			}
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
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

}
