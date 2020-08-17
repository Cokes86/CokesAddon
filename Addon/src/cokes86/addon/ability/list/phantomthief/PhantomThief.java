package cokes86.addon.ability.list.phantomthief;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

@SuppressWarnings(value = { "unused" })
@AbilityManifest(name = "팬텀 시프", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"$(Explain)" })
@NotAvailable(AbstractTripleMix.class)
public class PhantomThief extends CokesAbility implements ActiveHandler, TargetHandler {
	private AbilityBase newAbility = null;
	private final Object Explain = new Object() {
		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner("\n");
			return returnAbilityExplain(joiner, newAbility);
		}
	};

	public static PhantomMathod mathod;

	{
		PhantomMathod mathod1;
		try {
			mathod1 = Class.forName("cokes86.addon.ability.list.phantomthief." + ServerVersion.getName())
					.asSubclass(PhantomMathod.class).getConstructor(PhantomThief.class).newInstance(this);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| ClassNotFoundException e) {
			e.printStackTrace();
			mathod1 = null;
		}
		mathod = mathod1;
	}

	private static final Config<Integer> cooldown = new Config<Integer>(PhantomThief.class, "쿨타임", 90, 1) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, duration = new Config<Integer>(PhantomThief.class, "팬텀모드_지속시간", 15, 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, glowing = new Config<Integer>(PhantomThief.class, "발광모드_지속시간", 10, 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, change = new Config<Integer>(PhantomThief.class, "능력변환_대기시간", 30, 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};

	final Cooldown c = new Cooldown(cooldown.getValue());
	final Duration phantom_1 = new Phantom1(), phantom_2 = new Phantom2();
	AbstractGame.Participant target;

	public static boolean initPhantomThief() {
		try {
			Class.forName("cokes86.addon.ability.list.phantomthief." + ServerVersion.getName())
					.asSubclass(PhantomMathod.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public PhantomThief(AbstractGame.Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (newAbility != null) {
			if (newAbility instanceof ActiveHandler) {
				return ((ActiveHandler) newAbility).ActiveSkill(material, clickType);
			}
		} else {
			if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.LEFT_CLICK) && !phantom_1.isRunning() && !phantom_2.isRunning() && !c.isCooldown()) {
				Player p = getFarthestEntity(getPlayer().getLocation(), predicate);

				if (p != null && getGame().isParticipating(p)) {
					this.target = getGame().getParticipant(p);
					Location location = p.getLocation().clone();
					float playerYaw = location.getYaw();

					double radYaw = Math.toRadians(playerYaw + 180);

					double x = -Math.sin(radYaw);
					double z = Math.cos(radYaw);
					Vector velocity = new Vector(x, 0, z);
					velocity.normalize().multiply(10);

					Location after = p.getLocation().add(velocity);
					after.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(after.getWorld()), location.getY(), after.getBlockX(), after.getBlockZ()) + 0.1);
					getPlayer().teleport(after);

					getPlayer().sendMessage("§e" + p.getName() + "§f님이 목표입니다. 해당 플레이어에게 다가가 철괴로 우클릭하세요.");

					return phantom_1.start();
				} else {
					getPlayer().sendMessage("조건에 맞는 가장 먼 플레이어가 존재하지 않습니다.");
				}
			}
		}
		return false;
	}

	@Override
	public void TargetSkill(Material material, LivingEntity livingEntity) {
		if (newAbility != null) {
			if (newAbility instanceof TargetHandler) {
				((TargetHandler) newAbility).TargetSkill(material, livingEntity);
			}
		} else {
			if (material.equals(Material.IRON_INGOT) && livingEntity.equals(target.getPlayer()) && phantom_1.isRunning()) {
				phantom_1.stop(true);
				phantom_2.start();
				getPlayer().sendMessage("능력 훔치기를 시도합니다!");
				NMS.sendTitle(target.getPlayer(), "경  고", "팬텀시프가 당신의 능력을 훔칠려 합니다.", 10, 30, 10);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && phantom_1.isRunning()) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		if (e.getDamager().equals(getPlayer()) && phantom_1.isRunning()) {
			e.setCancelled(true);
		}

		if (e.getEntity().equals(getPlayer()) && phantom_2.isRunning()) {
			if (e.getDamager().equals(target.getPlayer()) || (e.getDamager() instanceof Projectile
					&& Objects.equals(((Projectile) e.getDamager()).getShooter(), target.getPlayer()))) {
				NMS.clearTitle(target.getPlayer());
				target = null;
				phantom_2.stop(true);
				getPlayer().removePotionEffect(PotionEffectType.GLOWING);
				Stun.apply(getParticipant(), TimeUnit.SECONDS, 1);
				Bukkit.broadcastMessage(getPlayer().getName() + "님의 능력은 §e팬텀 시프§f입니다.");
				c.start();
				c.setCount(c.getCount() / 2);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (!phantom_1.isRunning())
			return;
		mathod.onPlayerJoin(e);
	}

	@SubscribeEvent
	public void onPlayerQuit(PlayerQuitEvent e) {
		mathod.onPlayerQuit(e);
	}

	private static String returnAbilityExplain(StringJoiner joiner, AbilityBase ability) {
		if (ability != null) {
			joiner.add("§a--------------------------------");
			joiner.add("훔친 능력 §a| §b" + ability.getName() + " " + ability.getRank().getRankName() + " "
					+ ability.getSpecies().getSpeciesName());
			joiner.add("§a--------------------------------");
			Iterator<String> explain = ability.getExplanation();
			while (explain.hasNext()) {
				joiner.add("§r" + explain.next());
			}
		} else {
			for (String str : new String[] {
					"철괴 좌클릭 시 가장 멀리있는 플레이어의 등 뒤 10칸으로 워프 후, 팬텀 모드가 " + duration.toString() + " 지속됩니다. "
							+ cooldown.toString(),
					"팬텀 모드 동안에는 투명화, 갑옷 삭제효과를 받는 대신, 공격할 수 없고 공격받을 수 없습니다.", "팬텀 모드 동안 대상에게 철괴로 우클릭시 팬텀 모드가 즉시 종료되고,",
					"발광효과를 " + glowing.toString() + "동안 받습니다. 이때 대상은 이 사실을 알 수 있습니다.",
					"발광효과동안 대상에게 공격을 받지 않았을 경우 해당 플레이어의 능력을 훔치고,", "대상은 " + change.toString() + " 뒤 팬텀시프로 능력이 바뀝니다.",
					"반대로 공격을 받았을 경우 1초간 스턴상태가 되며 모두에게 자신의 능력이 공개됩니다.", "타게팅에 실패하거나 공격을 받아 스턴상태가 되었을 경우 쿨타임이 반으로 감소합니다.",
					"※능력 아이디어: RainStar_" }) {
				joiner.add(str);
			}
		}
		return joiner.toString();
	}

	private Player getFarthestEntity(Location center, Predicate<Entity> predicate) {
		double distance = Double.MIN_VALUE;
		Player current = null;

		Location centerLocation = center.clone();
		if (center.getWorld() == null)
			return null;
		for (Entity e : center.getWorld().getEntities()) {
			if (e instanceof Player) {
				Player entity = (Player) e;
				double compare = centerLocation.distanceSquared(entity.getLocation());
				if (compare > distance && (predicate == null || predicate.test(entity))) {
					distance = compare;
					current = entity;
				}
			}
		}

		return current;
	}

	Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer()))
			return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(entity.getUniqueId()))
					return false;
			}
			if (getGame() instanceof Teamable) {
				Teamable game = (Teamable) getGame();
				return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			if (!target.hasAbility()) return false;
			if (target.getAbility() instanceof Mix) {
				Mix mix = (Mix) target.getAbility();
				if (!mix.hasAbility())
					return false;
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return false;
	};

	class Phantom1 extends Duration {
		public Phantom1() {
			super(duration.getValue(), c);
		}

		public void onDurationStart() {
			mathod.hide();
		}

		@Override
		protected void onDurationProcess(int i) {
		}

		public void onDurationSilentEnd() {
			mathod.show();
		}

		public void onDurationEnd() {
			onDurationSilentEnd();
		}
	}

	class Phantom2 extends Duration {
		public Phantom2() {
			super(glowing.getValue());
		}

		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
		}

		protected void onDurationSilentEnd() {
			getPlayer().removePotionEffect(PotionEffectType.GLOWING);
		}

		protected void onDurationEnd() {
			NMS.clearTitle(target.getPlayer());
			getPlayer().removePotionEffect(PotionEffectType.GLOWING);
			try {
				if (target != null && target.hasAbility()) {
					if (target.getAbility() instanceof Mix && getParticipant().getAbility() instanceof Mix) {
						Mix targetMix = (Mix) target.getAbility();
						Mix mix = (Mix) getParticipant().getAbility();
						if (targetMix.hasAbility() && mix.hasAbility()) {
							boolean first = false;
							Class<? extends AbilityBase> continued;

							if (targetMix.getSynergy() != null) {
								Synergy synergy = targetMix.getSynergy();
								Pair<AbilityFactory.AbilityRegistration, AbilityFactory.AbilityRegistration> pair = SynergyFactory
										.getSynergyBase(synergy.getRegistration());
								if (mix.getFirst() != null && mix.getFirst().getClass().equals(PhantomThief.class)) {
									first = true;
									continued = pair.getRight().getAbilityClass();
									newAbility = create(pair.getLeft().getAbilityClass(), getParticipant());
								} else {
									continued = pair.getLeft().getAbilityClass();
									newAbility = create(pair.getRight().getAbilityClass(), getParticipant());
								}
							} else {
								if (mix.getFirst() != null && mix.getFirst().getClass().equals(PhantomThief.class)) {
									first = true;
									continued = targetMix.getSecond().getClass();
									newAbility = create(targetMix.getFirst().getClass(), getParticipant());
								} else {
									continued = targetMix.getFirst().getClass();
									newAbility = create(targetMix.getSecond().getClass(), getParticipant());
								}
							}
							new PhantomThiefTimer(target, first, continued);
						}
					} else {
						newAbility = create(target.getAbility().getClass(), getParticipant());
						new PhantomThiefTimer(target);
					}
					newAbility.setRestricted(false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class PhantomThiefTimer extends AbilityTimer {
		final AbstractGame.Participant target;
		final AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac;
		final boolean mix;
		private boolean first;
		private Class<? extends AbilityBase> continued;

		public PhantomThiefTimer(AbstractGame.Participant target) {
			super(change.getValue());
			this.target = target;
			this.ac = target.actionbar().newChannel();
			this.mix = false;
			this.start();
		}

		public PhantomThiefTimer(AbstractGame.Participant target, boolean first,
				Class<? extends AbilityBase> continued) {
			super(change.getValue());
			this.target = target;
			this.ac = target.actionbar().newChannel();
			this.mix = true;
			this.first = first;
			this.continued = continued;
			this.start();
		}

		public AbstractGame.Participant getParticipant() {
			return target;
		}

		protected void onStart() {
			try {
				if (mix) {
					Mix mix = (Mix) target.getAbility();
					if (first) {
						mix.setAbility(NullAbility.class, continued);
					} else {
						mix.setAbility(continued, NullAbility.class);
					}
				} else {
					target.setAbility(NullAbility.class);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void run(int arg0) {
			ac.update("팬텀 시프가 되기까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 전");
		}

		@Override
		protected void onEnd() {
			try {
				if (mix) {
					Mix mix = (Mix) target.getAbility();
					if (first) {
						mix.setAbility(PhantomThief.class, continued);
					} else {
						mix.setAbility(continued, PhantomThief.class);
					}
				} else {
					target.setAbility(PhantomThief.class);
				}
				target.getPlayer().sendMessage("당신의 능력이 팬텀 시프가 되었습니다 /aw check");
				ac.unregister();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		protected void onSilentEnd() {
			ac.unregister();
		}
	}

	@AbilityManifest(name = "사라짐", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL, explain = {
			"이런! 능력이 사라지셨네!" })
	public static class NullAbility extends AbilityBase {

		public NullAbility(AbstractGame.Participant arg0) {
			super(arg0);
		}

		@SubscribeEvent
		public void onParticipantDeath(ParticipantDeathEvent e) {
			if (e.getParticipant().equals(getParticipant())) {
				for (AbstractGame.GameTimer timer : getGame().getRunningTimers()) {
					if (timer instanceof PhantomThiefTimer) {
						PhantomThiefTimer ptTimer = (PhantomThiefTimer) timer;
						if (ptTimer.getParticipant().equals(getParticipant())) {
							ptTimer.stop(true);
						}
					}
				}
			}
		}
	}
}