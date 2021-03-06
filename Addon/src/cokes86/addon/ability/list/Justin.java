package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.triplemix.TripleMix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Function;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@AbilityManifest(name = "져스틴", rank= AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
		"$(EXPLAIN)",
		"§7사망 위기 §8- §8§l광기§f: 두 개의 인격이 융합되어 불안정해졌습니다.",
		"  체력을 전부 회복 후 빨라지며, 회복 효과를 받지 않습니다.",
		"  매 초 고정 1 대미지를 입으며 주는 대미지가 $[LAST_ADD_DAMAGE] 상승하고,",
		"  플레이어를 죽일 때마다 체력을 $[LAST_HEAL] 회복합니다.",
		"  다른 져스틴이 자신을 공격하는 경우 25%의 대미지만 받습니다."
})
public class Justin extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> COOL = new Config<Integer>(Justin.class, "쿨타임", 120, Config.Condition.COOLDOWN) {
	}, TELEPORT_RANGE = new Config<Integer>(Justin.class, "위험.이동_반경", 10) {
	}, DANGER_COST = new Config<Integer>(Justin.class, "위험.코스트", 4, Config.Condition.NUMBER) {
	}, DANGER_DELAY = new Config<>(Justin.class, "위험.딜레이", 2, Config.Condition.TIME),
			LAST_HEAL = new Config<>(Justin.class, "광기.회복량", 2, aInt -> aInt > 0);
	private static final Config<Double> DANGER_ADD_DAMAGE = new Config<>(Justin.class, "위험.추가대미지", 3.0, aDouble -> aDouble > 0),
	LAST_ADD_DAMAGE = new Config<>(Justin.class, "광기.추가대미지", 4.0, aDouble -> aDouble > 0);

	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner("\n");
			String[] explain = new String[]{
					"§7패시브(일반) §8- §c진실§f: 받은 피해의 절반만큼 잔혹함 수치가 쌓입니다.",
					"§7철괴 우클릭(일반) §8- §c누가 진짜일까§f: 잔혹함 수치만큼 §4위험 상태§f 인격이 됩니다.",
					"  돌입할 때 체력 $[DANGER_COST] 소모하고 주변 플레이어를 밀쳐냅니다. $[COOL]",
					"  돌입할 때 소모할 체력이 부족할 경우 발동하지 않습니다.",
					"  돌입을 시도할 때 $[DANGER_DELAY]의 딜레이가 존재합니다.",
					"  이 딜레이동안 움직일 수 없고, 공격을 할 수 없으며,",
					"  공격을 시도하거나 공격에 맞을 경우 실패합니다. 이때 쿨타임은 50% 감소합니다."
			};
			if (dangerTimer.isRunning()) {
				explain = new String[] {
						"§7패시브(§4위험§f) §8- §c놀아볼까?§f: 회복 효과를 받지 않습니다.",
						"  공격 시 $[DANGER_ADD_DAMAGE]의 추가대미지를 주고 대미지의 10%만큼 회복합니다.",
						"§7철괴 우클릭(§4위험§f) §8- §c위험할 뻔했어§f: 능력의 쿨타임이 25% 증가하고,",
						"  주변 $[TELEPORT_RANGE]칸 내 랜덤한 안전한 위치로 이동합니다.",
						"  이후 자신은 일반 상태 인격으로 돌아갑니다.",
				};
			}
			for (final Iterator<String> iterator = getExplanation(explain); iterator.hasNext();) {
				joiner.add(iterator.next());
			}
			return joiner.toString();
		}
	};

	private boolean isJustin(Player player) {
		if (!getGame().isParticipating(player)) return false;

		AbstractGame.Participant participant = getGame().getParticipant(player);
		if (!participant.hasAbility()) return false;

		AbilityBase abilityBase = participant.getAbility();
		if (abilityBase instanceof Mix) {
			Mix mix = (Mix) abilityBase;
			return mix.getFirst() instanceof Justin || mix.getSecond() instanceof Justin;
		} else if (abilityBase instanceof TripleMix) {
			TripleMix mix = (TripleMix) abilityBase;
			return mix.getFirst() instanceof Justin || mix.getSecond() instanceof Justin || mix.getThird() instanceof Justin;
		}
		return abilityBase instanceof Justin;
	}

	public static Iterator<String> getExplanation(String[] explanation) {
		boolean[] needsReplace = new boolean[explanation.length];
		for (int i = 0; i < explanation.length; i++) {
			needsReplace[i] = explanation[i].contains("$");
		}

		final Function<String, String> valueProvider = string -> {
			try {
				if (string != null) {
					final Field field = Justin.class.getDeclaredField(string);
					if (Modifier.isStatic(field.getModifiers())) {
						try {
							return String.valueOf(ReflectionUtil.setAccessible(field).get(null));
						} catch (IllegalAccessException ignored) {
						}
					}
				}
			} catch (NoSuchFieldException ignored) {
			}
			return "?";
		};

		return new Iterator<String>() {
			private int cursor = 0;
			@Override
			public boolean hasNext() {
				return cursor < explanation.length;
			}

			@Override
			public String next() {
				final int index = cursor;
				cursor++;
				final String line = explanation[index];
				if (needsReplace[index]) {
					return ROUND_BRACKET.replaceAll(SQUARE_BRACKET.replaceAll(line, valueProvider), valueProvider);
				} else {
					return line;
				}
			}
		};
	}
	
	private int cruel = 0;
	private final Cooldown cool = new Cooldown(COOL.getValue(), CooldownDecrease._50);
	private final AbilityTimer dangerTimer = new AbilityTimer() {
		double t = 0;
		@Override
		protected void onStart() {
			channel.update("[§4위험§f] | 잔혹함: "+cruel);
		}

		@Override
		protected void run(int count) {
			if (count % 20 == 0) cruel--;
			if (cruel == 0) {
				stop(false);
				return;
			}
			channel.update("[§4위험§f] | 잔혹함: "+cruel);
			
			t += 0.1;
			if (t >= 2.00) {
				t = 0.00;
			}

			double radian = Math.toRadians(t * 180*3);
			double sin = FastMath.sin(radian), cos = FastMath.cos(radian);
			double x = getPlayer().getLocation().clone().getX() + cos,
					y = getPlayer().getLocation().clone().getY() + t,
					z = getPlayer().getLocation().clone().getZ() + sin;
			Location location = new Location(getPlayer().getWorld(), x, y, z);
			ParticleLib.REDSTONE.spawnParticle(location, RGB.of(255, 1, 1));
		}

		@Override
		protected void onEnd() {
			channel.update("[일반] | 잔혹함: "+cruel);
			cool.start();
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	private final AbilityTimer lastTimer = new AbilityTimer() {
		double t = 0;
		@Override
		protected void run(int count) {
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 50, 3, true);
			if (count % 20 == 0) Healths.setHealth(getPlayer(), getPlayer().getHealth() -1);
			channel.update("[§8§l광기§f]");

			t += 0.1;
			if (t >= 2.00) {
				t = 0.00;
			}

			double radian = Math.toRadians(t * 180*3);
			double sin = FastMath.sin(radian), cos = FastMath.cos(radian);
			double x = getPlayer().getLocation().clone().getX() + cos,
					y = getPlayer().getLocation().clone().getY() + t,
					z = getPlayer().getLocation().clone().getZ() + sin;
			Location location = new Location(getPlayer().getWorld(), x, y, z);
			ParticleLib.REDSTONE.spawnParticle(location, RGB.of(255, 255, 255));
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	private final ChangeTimer changeTimer = new ChangeTimer();
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
	private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();
	private double lastClick = 0;

	public Justin(AbstractGame.Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		double current = System.currentTimeMillis();
		if (!cool.isCooldown() && current - lastClick >= 250 && material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !lastTimer.isRunning()) {
			final Location playerLocation = getPlayer().getLocation().clone();
			if (dangerTimer.isRunning()) {
				dangerTimer.stop(false);
				cool.setCount((int) (cool.getCooldown() * 1.25));
				randomTeleport();
			} else if (cruel > 0 && getPlayer().getHealth() > DANGER_COST.getValue() && !dangerTimer.isRunning() && !changeTimer.isRunning()){
				List<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5,5,predicate);
				for (Player entity : players) {
					entity.setVelocity(entity.getLocation().toVector().subtract(playerLocation.toVector()).normalize().multiply(2).setY(0));
				}
				addHealth(-DANGER_COST.getValue());
				return changeTimer.start();
			}
			lastClick = current;
		}
		return false;
	}

	public boolean randomTeleport() {
		final Location playerLocation = getPlayer().getLocation().clone();
		final Random random = new Random();
		double radian = Math.toRadians(random.nextDouble()*360);
		double sin = FastMath.sin(radian), cos = FastMath.cos(radian);
		double x = playerLocation.getX()+(cos * random.nextDouble() * TELEPORT_RANGE.getValue());
		double z = playerLocation.getZ()+(sin * random.nextDouble() * TELEPORT_RANGE.getValue());

		Location l = new Location(getPlayer().getWorld(), x, 100, z);
		l.setY(LocationUtil.getFloorYAt(getPlayer().getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.1);

		if (!getPlayer().getWorld().getWorldBorder().isInside(l)) {
			return randomTeleport();
		}
		return getPlayer().teleport(l);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			channel.update("[일반] | 잔혹함: "+cruel);
		}
	}

	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && (dangerTimer.isRunning()) || lastTimer.isRunning()) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onPlayerSetHealthEvent(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer()) && lastTimer.isRunning() && getPlayer().getHealth() < e.getHealth()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (Objects.equals(e.getEntity().getKiller(), getPlayer())) {
			addHealth(LAST_HEAL.getValue());
		}
		else if (e.getEntity().equals(getPlayer()) && lastTimer.isRunning()) {
			lastTimer.stop(true);
		}
	}
	
	public void addHealth(double add) {
		final double maxHealth = AttributeUtil.getMaxHealth(getPlayer());
		getPlayer().setHealth(Math.max(0, Math.min(getPlayer().getHealth() + add, maxHealth)));
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity().equals(getPlayer())) {
			if (!lastTimer.isRunning() && !dangerTimer.isRunning()) {
				cruel += event.getFinalDamage()/2;
				channel.update("[일반] | 잔혹함: "+cruel);
			}
		}
	}

	@SubscribeEvent(priority = 100)
	public void onEntityDamage2(EntityDamageEvent event) {
		if (!lastTimer.isRunning() && getPlayer().getHealth() - event.getFinalDamage() <= 0) {
			event.setCancelled(true);
			Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));
			lastTimer.start();
			dangerTimer.stop(true);
			SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(getPlayer().getLocation());
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		if (attacker instanceof Projectile) {
			Projectile projectile = (Projectile) attacker;
			if (projectile.getShooter() instanceof Entity) {
				attacker = (Entity) projectile.getShooter();
			}
		}

		if (attacker instanceof Player && isJustin((Player) attacker) && event.getEntity().equals(getPlayer()) && lastTimer.isRunning()) {
			event.setDamage(event.getDamage()*0.25);
		}

		if (attacker.equals(getPlayer())) {
			if (dangerTimer.isRunning()) {
				event.setDamage(event.getDamage()+ DANGER_ADD_DAMAGE.getValue());
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getFinalDamage()*0.1);
			} else if (lastTimer.isRunning()) {
				event.setDamage(event.getDamage()+ LAST_ADD_DAMAGE.getValue());
			}
		}
		onEntityDamage(event);
	}

	private class ChangeTimer extends AbilityTimer implements Listener {
		public ChangeTimer() {
			super(DANGER_DELAY.getValue());
		}

		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			SoundLib.ENTITY_ENDERMAN_STARE.playSound(getPlayer().getLocation());
		}

		public void onEnd() {
			dangerTimer.start();
			HandlerList.unregisterAll(this);
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		public void onPlayerMove(PlayerMoveEvent e) {
			if (e.getPlayer().equals(getPlayer())) {
				e.setTo(e.getFrom());
			}
		}

		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Entity attacker = e.getDamager();
			if (attacker instanceof Projectile) {
				Projectile projectile = (Projectile) attacker;
				if (projectile.getShooter() instanceof Entity) {
					attacker = (Entity) projectile.getShooter();
				}
			}

			if (e.getEntity().equals(getPlayer()) || attacker.equals(getPlayer())) {
				stop(true);
				cool.start();
				cool.setCount(cool.getCount()/2);
			}
		}
	}
}
