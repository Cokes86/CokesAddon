package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import com.google.common.base.Strings;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

@AbilityManifest(name = "물고기", rank = Rank.B, species = Species.ANIMAL, explain = {
		"§7능력 활성화 §8- §c철푸덕§r: 능력이 활성화되면 물갈퀴III 인챈트 쿠폰을 획득하며,",
		"  신발 착용 중 쿠폰을 우클릭하면 인챈트가 부여됩니다.",
		"§7물 속 §8- §c첨벙첨벙§r: 이동 속도가 빨라지고, 수중 호흡 효과를 얻으며",
		"  받는 대미지가 1 감소합니다.",
		"§7물 밖 §8- §c파닥파닥§r: 구속효과가 걸리며 이후 3초마다 수분이 감소합니다.",
		"  감소할 수분이 없을 경우 1의 고정피해를 입습니다.",
		"§7철괴 좌클릭 §8- §c촤아악§r: 자신의 위치에 물을 설치합니다. $[cool]",
		"  이미 물이 있는 자리에서는 물이 생성되지 않습니다.",
		"§7사망 시 §8- §c주르륵§r: 자신이 사망할 시 자신 주변 10칸의 물을 삭제합니다."
})
public class Fish extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool = new Config<Integer>(Fish.class, "쿨타임", 30, Config.Condition.COOLDOWN) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	private static boolean isWater(final Block block) {
		return block.getType().name().endsWith("WATER");
	}

	private static final ItemStack coupon = new ItemBuilder(MaterialX.PAPER).displayName("§a물갈퀴III 인챈트 쿠폰").build();
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final Cooldown cooldown = new Cooldown(cool.getValue());
	private int moisture = 10;
	private boolean kit = true;
	private final AbilityTimer timer = new AbilityTimer() {
		private int count = 0;
		private int up = 0;

		@Override
		protected void run(int arg0) {
			if (isWater(getPlayer().getLocation().getBlock())) {
				if (count != 0) {
					PotionEffects.SLOW.removePotionEffect(getPlayer());
					count = 0;
				}
				up++;
				if (up % 20 == 0 && moisture < 10) {
					moisture++;
					actionbarChannel.update("§b" + Strings.repeat("●", moisture) + "§7" + Strings.repeat("○", 10 - moisture));
				}
				PotionEffects.WATER_BREATHING.addPotionEffect(getPlayer(), 21, 0, true);
			} else {
				if (count == 0) {
					PotionEffects.WATER_BREATHING.removePotionEffect(getPlayer());
					up = 0;
				}
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 21, 0, true);
				count++;
				if (count % 60 == 0) {
					if (moisture > 0) {
						moisture--;
						actionbarChannel.update("§b" + Strings.repeat("●", moisture) + "§7" + Strings.repeat("○", 10 - moisture));
					} else {
						Damages.damageFixed(getPlayer(), getPlayer(), 1);
					}
				}
			}
		}

	};

	public Fish(Participant arg0) {
		super(arg0);
		timer.register();
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !cooldown.isCooldown()) {
			if (!getPlayer().getLocation().getBlock().isLiquid()) {
				getPlayer().getLocation().getBlock().setType(Material.WATER);
				cooldown.start();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			timer.setPeriod(TimeUnit.TICKS, 1).start();
			if (kit) {
				getPlayer().getInventory().addItem(coupon);
				this.kit = false;
			}
		}
	}

	@SubscribeEvent
	private void onPlayerInteract(PlayerInteractEvent e) {
		final Player player = e.getPlayer();
		if (player.equals(getPlayer())) {
			final ItemStack main = player.getInventory().getItemInMainHand();
			if (main.equals(coupon)) {
				final ItemStack boots = player.getInventory().getBoots();
				if (boots != null) {
					boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
					player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					player.sendMessage("쿠폰을 사용하였습니다.");
				} else {
					player.sendMessage("신발을 착용한 뒤에 사용해주세요.");
				}
			}
		}
	}

	@SubscribeEvent
	private void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		final Player player = e.getPlayer();
		if (player.equals(getPlayer())) {
			final ItemStack main = player.getInventory().getItemInMainHand();
			if (main.equals(coupon)) {
				final ItemStack boots = player.getInventory().getBoots();
				if (boots != null) {
					boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
					player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					player.sendMessage("쿠폰을 사용하였습니다.");
				} else {
					player.sendMessage("신발을 착용한 뒤에 사용해주세요.");
				}
			}
		}
	}

	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && isWater(getPlayer().getLocation().getBlock())) {
			e.setDamage(e.getDamage() - 1);
		}
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	private long lastBoost = 0;

	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && isWater(e.getFrom().getBlock()) && e.getTo() != null && isWater(e.getTo().getBlock())) {
			final long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis - lastBoost > 250) {
				this.lastBoost = currentTimeMillis;
				getPlayer().setVelocity(e.getTo().toVector().subtract(e.getFrom().toVector()).multiply(1.5));
			}
		}
	}

	@SubscribeEvent
	private void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getParticipant().equals(getParticipant())) {
			for (Block block : LocationUtil.getBlocks3D(getPlayer().getLocation(), 10, false, false)) {
				if (block.getType().equals(Material.WATER)) {
					block.setType(Material.AIR);
				}
			}
		}
	}
}
