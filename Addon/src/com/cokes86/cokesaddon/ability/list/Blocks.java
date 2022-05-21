package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Objects;

@AbilityManifest(name = "블럭", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 우클릭시 자신의 상태를 변화시킵니다. 자신의 상태에따라 추가효과를 얻습니다.",
		"§7돌 §f: 받는 대미지가 $[stone]% 감소합니다.",
		"  곡괭이로 자신이 공격받을 시 그 재료로 만든 검의 데미지를 받습니다.",
		"  이때, 효율은 날카로움 취급을 받으며, 공속에 영향을 받지 않습니다.",
		"§6모래 §f: 낙하 대미지를 입지 않습니다. 피해를 입을 시 $[inv]초간 무적상태가 되어 무적상태에선 넉백당하지 않습니다.",
		"§f유리 §f: 받는 대미지가 $[glass]% 증가합니다. 유리상태동안 자신은 블라인드 버프를 얻습니다. 또한 스킬의 대상이 되지 않습니다.",
		"§5옵시디언 §f: 폭발피해를 입지 않습니다. 넉백당하지 않습니다."},
summarize = {
		"§6철괴 우클릭 시 §f자신이 받는 효과를 순차적으로 바꿀 수 있습니다.",
		"§7돌§f: 받는 대미지 감소, 곡괭이로 공격받을 시 검의 대미지로 받음.",
		"§6모래§f: 낙하대미지 삭제, 피해 받을 시 일정시간 무적",
		"§f유리§f: 받는 대미지 증가, 투명화, 타게팅 대상 제외",
		"§5옵시디언§f: 폭발 대미지 삭제, 넉백 삭제"
})
@Tips(tip = {
		"4가지 상태를 다양한 상황에 맞추어 사용 가능",
		"돌상태는 상대방이 곡괭이가 있지 않는 이상 늘 대미지 감소"
}, stats = @Tips.Stats(offense = Tips.Level.ZERO, survival = Tips.Level.EIGHT, crowdControl = Tips.Level.ZERO, mobility = Tips.Level.ZERO, utility = Tips.Level.EIGHT), difficulty = Tips.Difficulty.EASY)
public class Blocks extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> stone = Config.of(Blocks.class, "돌_받는대미지감소량(%)", 20, FunctionalInterfaceUnit.between(0, 100, false));
	private static final Config<Integer> glass = Config.of(Blocks.class, "유리_받는대미지_증가량(%)", 100, FunctionalInterfaceUnit.positive());
	private static final Config<Integer> inv = Config.of(Blocks.class, "모래_무적시간", 6, FunctionalInterfaceUnit.positive(), "단위: 틱");
	private Condition condition = Condition.STONE;
	private final Participant.ActionbarNotification.ActionbarChannel ac = this.newActionbarChannel();
	private ArmorStand armorStand;
	private final double knockback = Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).getBaseValue();

	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void onStart() {
			armorStand = getPlayer().getWorld().spawn(getPlayer().getLocation().clone().add(0,1,0), ArmorStand.class);

			armorStand.setBasePlate(false);
			armorStand.setArms(false);
			armorStand.setGravity(false);
			armorStand.setVisible(false);
			if (ServerVersion.getVersion() >= 10 && ServerVersion.getVersion() <= 16) {
				armorStand.setInvulnerable(true);
			}
			NMS.removeBoundingBox(armorStand);

			EntityEquipment equipment = armorStand.getEquipment();
			if (equipment != null) {
				if (condition.getMaterialX() != null) {
					equipment.setHelmet(new ItemStack(condition.getMaterialX().getMaterial()));
				} else {
					equipment.setHelmet(null);
				}
			}
		}

		@Override
		protected void run(int count) {
			ac.update("상태: " + condition.getName());
			armorStand.teleport(getPlayer().getLocation().clone().add(0,1,0));
		}

		@Override
		protected void onEnd() {
			armorStand.remove();
		}

		@Override
		protected void onSilentEnd() {
			armorStand.remove();
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	private final AbilityTimer invTimer = new AbilityTimer(inv.getValue()){}.setPeriod(TimeUnit.TICKS, 1);

	public Blocks(Participant arg0) {
		super(arg0);
		passive.register();
	}


	@SubscribeEvent
	public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().equals(armorStand)) e.setCancelled(true);
	}

	protected void onUpdate(Update update) {
		switch (update) {
			case RESTRICTION_CLEAR:
				passive.start();
				break;
			case ABILITY_DESTROY:
			case RESTRICTION_SET:
				PotionEffects.INVISIBILITY.removePotionEffect(getPlayer());
				Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(knockback);
				break;
			default:
		}
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			PotionEffects.INVISIBILITY.removePotionEffect(getPlayer());
			Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(knockback);
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			condition = condition.next();

			EntityEquipment equipment = armorStand.getEquipment();
			if (equipment != null) {
				if (condition.getMaterialX() != null) {
					equipment.setHelmet(new ItemStack(condition.getMaterialX().getMaterial()));
				} else {
					equipment.setHelmet(null);
				}
			}

			if (condition.equals(Condition.OBSIDIAN)) {
				Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(1);
			} else {
				Objects.requireNonNull(getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(knockback);
			}

			if (condition.equals(Condition.GLASS)) {
				getParticipant().attributes().TARGETABLE.setValue(false);
				PotionEffects.INVISIBILITY.addPotionEffect(getPlayer(), Integer.MAX_VALUE, 0, true);
			} else {
				getParticipant().attributes().TARGETABLE.setValue(true);
				PotionEffects.INVISIBILITY.removePotionEffect(getPlayer());
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getDamager() == null) return;
		if (e.getEntity().equals(armorStand)) {
			e.setCancelled(true);
			return;
		}
		if (e.getEntity().equals(getPlayer())) {
			if (invTimer.isRunning()) {
				e.setCancelled(true);
			} else {
				if (condition.equals(Condition.STONE)) {
					e.setDamage(e.getDamage() * (100.0 - stone.getValue()) / 100);
				} else if (condition.equals(Condition.SAND)) {
					if (e.getCause().equals(DamageCause.FALL)) {
						e.setCancelled(true);
						return;
					}
					invTimer.start();
				} else if (condition.equals(Condition.GLASS)) {
					e.setDamage(e.getDamage() * (1 + glass.getValue() / 100.0));
				} else if (condition.equals(Condition.OBSIDIAN)) {
					if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION)
							|| e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
						e.setCancelled(true);
					} else {
						getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth() - e.getFinalDamage()));
						e.setDamage(0);
						Vector vec = new Vector();
						getPlayer().setVelocity(vec);
						Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> getPlayer().setVelocity(vec),
								1L);
					}
				}
			}
		}

		if (e.getEntity().equals(getPlayer())) {
			if (e.getDamager() instanceof Player) {
				Player damager = (Player) e.getDamager();
				if (condition.equals(Condition.STONE)) {
					ItemStack i = damager.getInventory().getItemInMainHand();
					Material mainhand = i.getType();
					Pickaxe p = Pickaxe.getPickaxe(mainhand);
					if (p != null) {
						double damage = p.getDamage();

						int level = i.getEnchantmentLevel(Enchantment.DIG_SPEED);
						if (level > 0) {
							e.setDamage(damage + (level + 1) * 0.5);
						} else {
							e.setDamage(damage);
						}
					} else {
						e.setDamage(e.getDamage() * (100.0 - stone.getValue()) / 100);
					}
				} else {
					onEntityDamage(e);
				}
			} else {
				onEntityDamage(e);
			}
		}
	}

	public enum Condition {
		STONE("§7돌§f", MaterialX.STONE) {
			@Override
			protected Condition next() {
				return Condition.SAND;
			}
		},
		SAND("§6모래§f", MaterialX.SAND) {
			@Override
			protected Condition next() {
				return Condition.GLASS;
			}
		},
		GLASS("§f유리§f", null) {
			@Override
			protected Condition next() {
				return Condition.OBSIDIAN;
			}
		},
		OBSIDIAN("§5옵시디언§f", MaterialX.OBSIDIAN) {
			@Override
			protected Condition next() {
				return Condition.STONE;
			}
		};

		final String name;
		final MaterialX materialX;

		Condition(String name, MaterialX materialX) {
			this.name = name;
			this.materialX = materialX;
		}

		protected abstract Condition next();

		protected String getName() {
			return name;
		}

		public MaterialX getMaterialX() {
			return materialX;
		}
	}

	enum Pickaxe {
		WOOD(MaterialX.WOODEN_PICKAXE.getMaterial(), 4), STONE(MaterialX.STONE_PICKAXE.getMaterial(), 5),
		IRON(MaterialX.IRON_PICKAXE.getMaterial(), 6), GOLD(MaterialX.GOLDEN_PICKAXE.getMaterial(), 4),
		DIAMOND(MaterialX.DIAMOND_PICKAXE.getMaterial(), 7), NETHERITE(MaterialX.NETHERITE_PICKAXE.getMaterial(), 8);

		final Material m;
		final double damage;

		Pickaxe(Material m, double damage) {
			this.damage = damage;
			this.m = m;
		}

		public static Pickaxe getPickaxe(Material m) {
			for (Pickaxe p : Pickaxe.values()) {
				if (p.getPickaxeMaterial() != null && p.getPickaxeMaterial().equals(m)) {
					return p;
				}
			}
			return null;
		}

		public Material getPickaxeMaterial() {
			return m;
		}

		public double getDamage() {
			return damage;
		}
	}
}
