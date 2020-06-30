package cokes86.addon.ability.list;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.LocationPlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil.Predicates;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "팬텀 시프", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 좌클릭시 가장 멀리있는 플레이어의 등 뒤 10칸으로 워프 후, 팬텀 모드가 10초 지속됩니다.",
		"팬텀 모드 동안에는 투명화, 갑옷 삭제효과를 받는 대신, 공격할 수 없고 공격받을 수 없습니다.", "팬텀 모드 동안 대상에게 철괴로 우클릭시 팬텀 모드가 즉시 종료되고,",
		"발광효과를 15초동안 받습니다. 이때 대상은 이 사실을 알 수 있습니다.", "발광효과동안 대상에게 공격을 받지 않았을 경우 해당 플레이어의 능력을 훔치고,",
		"대상은 30초 뒤 팬텀시프로 능력이 바뀝니다.", "반대로 공격을 받았을 경우 1초간 스턴상태가 되며 모두에게 자신의 능력이 공개됩니다.", "※능력 아이디어: RainStar_" })
public class PhantomThief extends AbilityBase implements ActiveHandler, TargetHandler {
	Player target;
	ItemStack[] armor = new ItemStack[4];

	public static Config<Integer> cool = new Config<Integer>(PhantomThief.class, "쿨타임", 90, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};

	CooldownTimer c = new CooldownTimer(cool.getValue());

	public PhantomThief(Participant participant) {
		super(participant);
	}

	DurationTimer phantom_1 = new DurationTimer(15) {
		public void onDurationStart() {
			armor[0] = getPlayer().getInventory().getHelmet();
			armor[1] = getPlayer().getInventory().getChestplate();
			armor[2] = getPlayer().getInventory().getLeggings();
			armor[3] = getPlayer().getInventory().getBoots();

			getPlayer().getInventory().setHelmet(new ItemStack(Material.AIR));
			getPlayer().getInventory().setChestplate(new ItemStack(Material.AIR));
			getPlayer().getInventory().setLeggings(new ItemStack(Material.AIR));
			getPlayer().getInventory().setBoots(new ItemStack(Material.AIR));
		}

		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
		}

		public void onDurationSilentEnd() {
			getPlayer().getInventory().setHelmet(armor[0]);
			getPlayer().getInventory().setChestplate(armor[1]);
			getPlayer().getInventory().setLeggings(armor[2]);
			getPlayer().getInventory().setBoots(armor[3]);
			armor = new ItemStack[4];
			getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
		}

		public void onDurationEnd() {
			onSilentEnd();
			c.start();
		}
	};

	DurationTimer phantom_2 = new DurationTimer(10) {

		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
			switch (arg0) {
			case 10:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.C));
				break;
			case 9:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.D));
				break;
			case 8:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.E));
				break;
			case 7:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.F));
				break;
			case 6:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.G));
				break;
			case 5:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.A));
				break;
			case 4:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.B));
				break;
			case 3:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.C));
				break;
			case 2:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.D));
				break;
			case 1:
				SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Note.Tone.E));
				break;
			}
		}

		protected void onDurationSilentEnd() {
			getPlayer().removePotionEffect(PotionEffectType.GLOWING);
		}

		protected void onDurationEnd() {
			getPlayer().removePotionEffect(PotionEffectType.GLOWING);
			try {
				Participant targ = getGame().getParticipant(target);
				if (targ.hasAbility()) {
					if (targ.getAbility().getClass() == Mix.class) {
						Mix mix = (Mix) getParticipant().getAbility();
						int a;
						if (mix.getFirst().getClass() == PhantomThief.class) {
							mix.setAbility(((Mix) targ.getAbility()).getFirst().getClass(), mix.getSecond().getClass());
							getPlayer().sendMessage("능력을 훔쳤습니다. => " + ((Mix) targ.getAbility()).getFirst().getName());
							((Mix) targ.getAbility()).getFirst().setRestricted(true);
							a = 0;
						} else {
							mix.setAbility(mix.getFirst().getClass(), ((Mix) targ.getAbility()).getSecond().getClass());
							getPlayer().sendMessage("능력을 훔쳤습니다. => " + ((Mix) targ.getAbility()).getSecond().getName());
							((Mix) targ.getAbility()).getSecond().setRestricted(true);
							a = 1;
						}
						target.sendMessage("팬텀시프가 당신의 능력을 훔쳤습니다. 30초뒤 자신의 능력 중 하나가 팬텀시프로 바뀝니다.");

						new Timer(30) {
							ActionbarChannel ac;

							protected void onStart() {
								ac = targ.actionbar().newChannel();
							}

							@Override
							protected void run(int arg0) {
								ac.update("팬텀시프가 되기까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 전");
							}

							protected void onEnd() {
								try {
									if (a == 0) {
										((Mix) targ.getAbility()).setAbility(PhantomThief.class,
												((Mix) targ.getAbility()).getSecond().getClass());
									} else {
										((Mix) targ.getAbility()).setAbility(
												((Mix) targ.getAbility()).getFirst().getClass(), PhantomThief.class);
									}
									ac.update(null);
									target.sendMessage("당신의 능력이 팬텀시프가 되었습니다 /aw check");
									ac.unregister();
								} catch (IllegalAccessException | InvocationTargetException
										| InstantiationException e) {
									e.printStackTrace();
								}
							}

						}.start();
					} else {
						getParticipant().removeAbility();
						getParticipant().setAbility(targ.getAbility().getClass());
						getPlayer().sendMessage("능력을 훔쳤습니다. => " + targ.getAbility().getName());

						target.sendMessage("팬텀시프가 당신의 능력을 훔쳤습니다. 30초뒤 자신의 능력이 팬텀시프로 바뀝니다.");
						new Timer(30) {
							ActionbarChannel ac;

							protected void onStart() {
								ac = targ.actionbar().newChannel();
							}

							@Override
							protected void run(int arg0) {
								ac.update("팬텀시프가 되기까지 " + TimeUtil.parseTimeAsString(getFixedCount()) + " 전");
							}

							protected void onEnd() {
								try {
									targ.setAbility(PhantomThief.class);
									ac.update(null);
									target.sendMessage("당신의 능력이 팬텀시프가 되었습니다 /aw check");
									ac.unregister();
								} catch (IllegalAccessException | InvocationTargetException
										| InstantiationException e) {
									e.printStackTrace();
								}
							}

						}.start();
					}
				} else {
					getPlayer().sendMessage("이런! 상대방이 능력이 없네요. 다시 시도해봐요~!");
				}
			} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				e.printStackTrace();
			}
		}
	};

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
			if (e.getDamager().equals(target) || (e.getDamager() instanceof Projectile
					&& ((Projectile) e.getDamager()).getShooter().equals(target))) {
				target = null;
				phantom_2.stop(true);
				getPlayer().removePotionEffect(PotionEffectType.GLOWING);
				Stun.apply(getParticipant(), TimeUnit.SECONDS, 1);
				Bukkit.broadcastMessage(getPlayer().getName() + "님의 능력은 §e팬텀시프§f입니다.");
				c.start();
				c.setCount(c.getCount() / 2);
			}
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK) && !phantom_1.isRunning()
				&& !phantom_2.isRunning()) {
			Predicate<Entity> predicate = LocationPlusUtil.HAVE_ABILITY().and(Predicates.STRICT(getPlayer()));
			target = LocationPlusUtil.getFarthestEntity(Player.class, getPlayer().getLocation(), predicate);

			if (target != null) {
				Location location = target.getLocation().clone();
				float playerYaw = location.getYaw();

				double radYaw = Math.toRadians(playerYaw + 180);

				double x = -Math.sin(radYaw);
				double z = Math.cos(radYaw);
				Vector velocity = new Vector(x, 0, z);
				velocity.normalize().multiply(10);
				
				Location after = target.getLocation().add(velocity);
				getPlayer().teleport(after.add(0, LocationUtil.getFloorYAt(after.getWorld(), after.getY(), after.getBlockX(), after.getBlockZ()) + (0.1), 0));

				return phantom_1.start();
			}
		}
		return false;
	}

	@Override
	public void TargetSkill(Material arg0, LivingEntity arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(target) && phantom_1.isRunning()) {
			phantom_1.stop(true);
			phantom_2.start();
			getPlayer().sendMessage("능력 훔치기를 시도합니다!");
			target.sendTitle("경  고", "팬텀시프가 당신의 능력을 훔칠려 합니다.", 10, 30, 10);
		}
	}
}