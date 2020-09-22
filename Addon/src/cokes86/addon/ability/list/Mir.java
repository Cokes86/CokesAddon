package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Random;
import java.util.function.Predicate;

@AbilityManifest(name = "미르", rank = Rank.B, species = Species.HUMAN, explain = {
		"철괴 우클릭 시 해당 위치에서 일정 범위 내에 각종 효과를 부여하는 정령을 소환합니다. $[cool]",
		"정령은 랜덤하게 소환되며 지속 시간 $[duration]가 지난 후 소멸합니다.",
		"정령은 같은 종류로 2회 이상 소환이 되지 않습니다.",
		"§4이프리트 : §f$[range_ifrit]블럭 안의 자신을 제외한 플레이어는 화상효과를 받습니다.",
		"§7셰이드 : §f$[range_shade]블럭 안의 자신을 제외한 플레이어는 블라인드효과를 받습니다.",
		"§b썬더버드 : §f$[range_thunder]블럭 안에 있는 자신이 상대방을 공격할 시 딜이 $[damage_increase]배 상승하며, 번개를 내려칩니다.",
		"§6노움 : §f$[range_gnome]블럭 안에 있는 자신은 흙으로 된 보호막을 생성해 자신이 받는 대미지가 $[damage_decrease]% 감소합니다.",
		"※제작자 자캐 기반 능력자"
})
@Beta
public class Mir extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = new Config<Integer>(Mir.class, "쿨타임", 20, 1) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, duration = new Config<Integer>(Mir.class, "지속시간", 10, 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, range_ifrit = new Config<Integer>(Mir.class, "범위.이프리트", 8) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, range_shade = new Config<Integer>(Mir.class, "범위.셰이드", 12) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, range_thunder = new Config<Integer>(Mir.class, "범위.썬더버드", 12) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, range_gnome = new Config<Integer>(Mir.class, "범위.노움", 15) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, damage_decrease = new Config<Integer>(Mir.class, "노움_대미지감소율(%)", 25) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	private static final Config<Double> damage_increase = new Config<Double>(Mir.class, "썬더버드_딜증가배율", 1.2) {
		@Override
		public boolean condition(Double value) {
			return value > 0;
		}
	};
	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())
					|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
					|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
				return false;
			}
			if (getGame() instanceof Teamable) {
				final Teamable Teamable = (Teamable) getGame();
				final Participant entityParticipant = getGame().getParticipant(entity.getUniqueId());
				return !Teamable.hasTeam(entityParticipant) || !Teamable.hasTeam(getParticipant()) || (!Teamable.getTeam(entityParticipant).equals(Teamable.getTeam(getParticipant())));
			}
			return getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue();
		}
		return true;
	};
	Element element = null;
	Cooldown cooldownTimer = new Cooldown(cool.getValue());
	private ArmorStand armorStand = null;
	Duration durationTimer = new Duration(duration.getValue() * 10, cooldownTimer) {
		@Override
		protected void onDurationStart() {
			element = getRandomElement();

			armorStand = getPlayer().getWorld().spawn(getPlayer().getLocation().clone().add(0, 3, 0), ArmorStand.class);
			armorStand.setCustomName(element.name());
			armorStand.setCustomNameVisible(true);
			armorStand.setBasePlate(false);
			armorStand.setArms(true);
			armorStand.setGravity(false);
			armorStand.setSmall(true);
			armorStand.setVisible(false);
			if (ServerVersion.getVersion() >= 10 && ServerVersion.getVersion() <= 16) {
				armorStand.setInvulnerable(true);
			}

			ItemStack chestplate = MaterialX.LEATHER_CHESTPLATE.createItem();
			LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();
			assert meta != null;
			meta.setColor(element.rgb.getColor());
			chestplate.setItemMeta(meta);

			EntityEquipment equipment = armorStand.getEquipment();
			assert equipment != null;
			equipment.setHelmet(new ItemStack(element.helmet));
			equipment.setChestplate(chestplate);

			getPlayer().sendMessage(element.name() + KoreanUtil.getJosa(element.name(), KoreanUtil.Josa.을를) + " 소환합니다.");
		}

		@Override
		protected void onDurationProcess(int i) {
			Location location = armorStand.getLocation();
			location.setYaw(location.getYaw() + 5);
			armorStand.teleport(location);
			int range = element.range;

			for (Location particle : Circle.of(range, range * 8).toLocations(armorStand.getLocation()).floor(armorStand.getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(particle, element.rgb);
			}

			for (Player player : LocationUtil.getNearbyEntities(Player.class, armorStand.getLocation(), range, range, predicate)) {
				if (element == Element.IFRIT) {
					player.setFireTicks(21);
				} else if (element == Element.SHADE) {
					PotionEffects.BLINDNESS.addPotionEffect(player, 41, 0, true);
				}
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
			if (armorStand != null) {
				armorStand.remove();
				armorStand = null;
			}
		}
	}.setPeriod(TimeUnit.TICKS, 2);

	public Mir(Participant arg0) {
		super(arg0);
	}

	public Element getRandomElement() {
		Random random = new Random();
		Element after = Element.values()[random.nextInt(Element.values().length)];
		if (element == null || !after.equals(element)) return after;
		else return getRandomElement();
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldownTimer.isCooldown() && !durationTimer.isDuration()) {
			durationTimer.start();
			return true;
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (element == Element.GNOME && armorStand != null && e.getEntity().equals(getPlayer()) && getPlayer().getLocation().subtract(armorStand.getLocation().clone().add(0, -5, 0)).length() <= element.range) {
			e.setDamage(e.getDamage() * (1 - damage_decrease.getValue() / 100.0));
		}

		if (durationTimer.isRunning() && e.getEntity().equals(armorStand)) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (element == Element.THUNDERBIRD && armorStand != null && damager.equals(getPlayer()) && getPlayer().getLocation().subtract(armorStand.getLocation().clone().add(0, -5, 0)).length() <= element.range) {
			if (!e.getEntity().equals(armorStand)) {
				e.setDamage(e.getDamage() * damage_increase.getValue());
				e.getEntity().getWorld().strikeLightningEffect(e.getEntity().getLocation());
			}
		}
	}

	@SubscribeEvent
	public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().equals(armorStand)) e.setCancelled(true);
	}

	enum Element {
		IFRIT(MaterialX.REDSTONE_BLOCK.getMaterial(), ParticleLib.RGB.of(255, 1, 1), range_ifrit.getValue()),
		SHADE(MaterialX.COAL_BLOCK.getMaterial(), ParticleLib.RGB.of(1, 1, 1), range_shade.getValue()),
		THUNDERBIRD(MaterialX.LAPIS_BLOCK.getMaterial(), ParticleLib.RGB.of(1, 1, 255), range_thunder.getValue()),
		GNOME(MaterialX.DIRT.getMaterial(), ParticleLib.RGB.of(179, 109, 65), range_gnome.getValue());

		private final Material helmet;
		private final ParticleLib.RGB rgb;
		private final int range;

		Element(Material helmet, ParticleLib.RGB rgb, int range) {
			this.helmet = helmet;
			this.rgb = rgb;
			this.range = range;
		}
	}
}
