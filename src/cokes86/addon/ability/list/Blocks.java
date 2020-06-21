package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "블럭", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 우클릭시 자신의 상태를 변화시킵니다. 자신의 상태에따라 추가효과를 얻습니다. $[cooldown]", "§7돌 §f: 받는 대미지가 $[stone]% 감소합니다.",
		"곡괭이로 자신이 공격받을 시 그 재료로 만든 검의 데미지를 받습니다.", "이때, 효율은 날카로움 취급을 받으며, 공속에 영향을 받지 않습니다.",
		"§6모래 §f: 낙하 대미지를 입지 않습니다. 피해를 입을 시 1초간 무적상태가 되어 무적상태에선 넉백당하지 않습니다.",
		"§f유리 §f: 받는 대미지가 $[glass]% 증폭합니다. 유리상태동안 자신은 블라인드 버프를 얻습니다. 또한 스킬의 대상이 되지 않습니다.",
		"§5옵시디언 §f: 폭발피해를 입지 않습니다. 넉백당하지 않습니다." })
public class Blocks extends AbilityBase implements ActiveHandler {
	protected Condition condition = Condition.STONE;
	protected Participant.ActionbarNotification.ActionbarChannel ac = this.newActionbarChannel();

	protected static Config<Integer> stone = new Config<Integer>(Blocks.class, "돌_받는대미지(%)", 20) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0 && value < 100;
		}
	}, glass = new Config<Integer>(Blocks.class, "유리_받는대미지(%)", 200) {
		@Override
		public boolean Condition(Integer value) {
			return value > 100;
		}
	};
	protected Object cooldown = new Object() {
		@Override
		public String toString() {
			return Formatter.formatCooldown(3);
		}
	};

	public Blocks(Participant arg0) {
		super(arg0);
	}
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			Passive.start();
			break;
		default:
		}
	}

	CooldownTimer c = new CooldownTimer(3);
	Timer Passive = new Timer() {
		@Override
		protected void run(int count) {
			ac.update("상태: " + condition.getName());

			if (condition.equals(Condition.GLASS)) {
				PotionEffects.INVISIBILITY.addPotionEffect(getPlayer(), 30, 0, true);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	Timer invTimer = new Timer(1) {
		@Override
		protected void run(int count) {
		}
	};

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				condition = condition.next();
				if (condition.equals(Condition.GLASS))
					getParticipant().attributes().TARGETABLE.setValue(false);
				else
					getParticipant().attributes().TARGETABLE.setValue(true);
				c.start();
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		Passive.start();
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (invTimer.isRunning()) {
				e.setCancelled(true);
			} else {
				if (condition.equals(Condition.STONE)) {
					e.setDamage(e.getDamage() * (100 - stone.getValue()) / (double) 100);
				} else if (condition.equals(Condition.SAND)) {
					if (e.getCause().equals(DamageCause.FALL)) {
						e.setCancelled(true);
					}
					invTimer.start();
				} else if (condition.equals(Condition.GLASS)) {
					e.setDamage(e.getDamage() * (double) glass.getValue() / 100);
				} else if (condition.equals(Condition.OBSIDIAN)) {
					if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION)
							|| e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
						e.setCancelled(true);
					} else {
						e.setCancelled(true);
						getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth() - e.getFinalDamage()));
						SoundLib.ENTITY_PLAYER_HURT.playSound(getPlayer());
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
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
						e.setDamage(e.getDamage() * stone.getValue() / (float) 100);
					}
				} else if (condition.equals(Condition.OBSIDIAN)) {
					onEntityDamage(e);
					SoundLib.ENTITY_PLAYER_HURT.playSound(damager);
				} else {
					onEntityDamage(e);
				}
			} else if (e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				if (a.getShooter() instanceof Player) {
					Player damager = (Player) a.getShooter();
					if (condition.equals(Condition.OBSIDIAN)) {
						onEntityDamage(e);
						SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(damager);
					} else {
						onEntityDamage(e);
					}
				} else {
					onEntityDamage(e);
				}
			} else {
				onEntityDamage(e);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	enum Condition {
		STONE("§7돌§f") {
			@Override
			protected Condition next() {
				return Condition.SAND;
			}
		},
		SAND("§6모래§f") {
			@Override
			protected Condition next() {
				return Condition.GLASS;
			}
		},
		GLASS("§f유리§f") {
			@Override
			protected Condition next() {
				return Condition.OBSIDIAN;
			}
		},
		OBSIDIAN("§5옵시디언§f") {
			@Override
			protected Condition next() {
				return Condition.STONE;
			}
		};

		String name;

		protected abstract Condition next();

		Condition(String name) {
			this.name = name;
		}

		protected String getName() {
			return name;
		}
	}

	enum Pickaxe {
		WOOD(MaterialX.WOODEN_PICKAXE.parseMaterial(), 4), STONE(MaterialX.STONE_PICKAXE.parseMaterial(), 5),
		IRON(MaterialX.IRON_PICKAXE.parseMaterial(), 6), GOLD(MaterialX.GOLDEN_PICKAXE.parseMaterial(), 4),
		DIAMOND(MaterialX.DIAMOND_PICKAXE.parseMaterial(), 7);

		Material m;
		double damage;

		Pickaxe(Material m, double damage) {
			this.damage = damage;
			this.m = m;
		}

		public Material getPickaxeMaterial() {
			return m;
		}

		public double getDamage() {
			return damage;
		}

		public static Pickaxe getPickaxe(Material m) {
			for (Pickaxe p : Pickaxe.values()) {
				if (p.getPickaxeMaterial().equals(m)) {
					return p;
				}
			}
			return null;
		}
	}
}
