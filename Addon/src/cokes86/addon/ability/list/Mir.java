package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
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

import java.util.function.Predicate;

@AbilityManifest(name = "미르", rank = Rank.B, species = Species.HUMAN, explain = {
		"철괴 우클릭 시 해당 위치에서 일정 범위 내에 각종 효과를 부여하는 정령을 소환합니다. $[cool]",
		"정령은 랜덤하게 소환되며 지속 시간 $[duration]가 지난 후 소멸합니다.",
		"정령은 같은 종류로 2회 이상 소환이 되지 않습니다.",
		"§4이프리트 §f: $[range_ifrit]블럭 안의 자신을 제외한 플레이어는 화상효과를 부여합니다.",
		"§7셰이드 §f: $[range_shade]블럭 안의 자신을 제외한 플레이어는 블라인드효과를 부여합니다.",
		"§b썬더버드 §f: $[range_thunder]블럭 안에 있는 자신이 상대방을 공격할 시 주는 대미지가 $[damage_increase]% 증가하며, 번개를 내려칩니다.",
		"§6노움 §f: $[range_gnome]블럭 안에 있는 자신은 흙으로 된 보호막을 생성해 자신이 받는 대미지가 $[damage_decrease]% 감소합니다."
}, summarize = {
		"철괴 우클릭 시 무작위의 정령을 소환해 각종 효과를 받습니다.",
		"이프리트: 자신 제외 화상, 셰이드: 자신 제외 블라인드",
		"썬더버드: 자신이 영역 내 공격 시 대미지 증가, 노움: 대미지 감소"
})
public class Mir extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = new Config<>(Mir.class, "쿨타임", 20, Config.Condition.COOLDOWN);
	private static final Config<Integer> duration = new Config<>(Mir.class, "지속시간", 10, Config.Condition.TIME);
	private static final Config<Integer> range_ifrit = new Config<>(Mir.class, "범위.이프리트", 8, PredicateUnit.positive(Integer.class));
	private static final Config<Integer> range_shade = new Config<>(Mir.class, "범위.셰이드", 12, PredicateUnit.positive(Integer.class));
	private static final Config<Integer> range_thunder = new Config<>(Mir.class, "범위.썬더버드", 12, PredicateUnit.positive(Integer.class));
	private static final Config<Integer> range_gnome = new Config<>(Mir.class, "범위.노움", 15, PredicateUnit.positive(Integer.class));
	private static final Config<Integer> damage_decrease = new Config<>(Mir.class, "노움_대미지감소율(%)", 25, PredicateUnit.positive(Integer.class));
	private static final Config<Integer> damage_increase = new Config<>(Mir.class, "썬더버드_딜증가배율(%)", 20, PredicateUnit.positive(Integer.class));

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
	private Element element = null;
	private final Cooldown cooldownTimer = new Cooldown(cool.getValue());
	private ArmorStand armorStand = null;
	private final Duration durationTimer = new Duration(duration.getValue() * 10, cooldownTimer) {
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
			armorStand.setInvulnerable(true);
			NMS.removeBoundingBox(armorStand);

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
		Element after = random.pick(Element.values());
		return element.equals(after) ? getRandomElement() : after;
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
		IFRIT(MaterialX.REDSTONE_BLOCK.getMaterial(), RGB.of(255, 1, 1), range_ifrit.getValue()),
		SHADE(MaterialX.COAL_BLOCK.getMaterial(), RGB.of(1, 1, 1), range_shade.getValue()),
		THUNDERBIRD(MaterialX.LAPIS_BLOCK.getMaterial(), RGB.of(1, 1, 255), range_thunder.getValue()),
		GNOME(MaterialX.DIRT.getMaterial(), RGB.of(179, 109, 65), range_gnome.getValue());

		private final Material helmet;
		private final RGB rgb;
		private final int range;

		Element(Material helmet, RGB rgb, int range) {
			this.helmet = helmet;
			this.rgb = rgb;
			this.range = range;
		}
	}
}
