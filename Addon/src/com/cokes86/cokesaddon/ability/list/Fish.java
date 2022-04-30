package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
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
		"§7능력 활성화 §8- §b철푸덕§f: 능력이 활성화되면 물갈퀴III 인챈트 쿠폰을 획득하며,",
		"  신발 착용 중 쿠폰을 우클릭하면 인챈트가 부여됩니다.",
		"§7물 속 §8- §b첨벙첨벙§f: 이동 속도가 빨라지고, 수중 호흡 효과를 얻으며",
		"  받는 대미지가 1 감소합니다.",
		"§7물 밖 §8- §b파닥파닥§f: 구속효과가 걸리며 이후 3초마다 수분이 감소합니다.",
		"  감소할 수분이 없을 경우 1의 고정피해를 입습니다.",
		"§7철괴 좌클릭 §8- §b촤아악§f: 자신의 위치에 물을 설치합니다. $[SPRINKLE_COOLDOWN]",
		"  이미 물이 있는 자리에서는 물이 생성되지 않습니다.",
		"§7철괴 우클릭 §8- §b뻐끔뻐끔§f: 자신 주변 10블럭의 물을 흡수합니다.",
		"  자신이 흡수한 물의 비례해 §b파닥파닥§f의 효과를 최대 $[QUEAK_MAX_DRATION] 무시합니다.",
		"  또한, 지속시간동안 저항 1버프를 부여합니다. $[QUEAK_COOLDOWN]",
		"§7사망 시 §8- §c꼬로록§f: 자신이 사망할 시 자신 주변 10칸의 물을 삭제합니다."
})
public class Fish extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> SPRINKLE_COOLDOWN = Config.of(Fish.class, "촤아악_쿨타임", 30, Config.Condition.COOLDOWN);
	private static final Config<Integer> QUEAK_MAX_DRATION = Config.of(Fish.class, "뻐끔뻐끔_최대_지속시간", 20, Config.Condition.TIME);
	private static final Config<Integer> QUEAK_COOLDOWN = Config.of(Fish.class, "뻐끔뻐끔_쿨타임", 60, Config.Condition.COOLDOWN);

	private static boolean isWater(final Block block) {
		return block.getType().name().endsWith("WATER");
	}

	private static final ItemStack coupon = new ItemBuilder(MaterialX.PAPER).displayName("§a물갈퀴III 인챈트 쿠폰").build();
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final Cooldown sprinkle_cooldown = new Cooldown(SPRINKLE_COOLDOWN.getValue(), "촤아악");
	private final Cooldown queak_cooldown = new Cooldown(QUEAK_COOLDOWN.getValue(), "뻐끔뻐끔");
	private final Duration queak_duration = new Duration(QUEAK_MAX_DRATION.getValue(), queak_cooldown, "뻐끔뻐끔") {
		@Override
		protected void onDurationProcess(int i) {
			PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 22, 0, true);
		}
	};
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
				if (!queak_duration.isRunning()) {
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
		}
	};

	public Fish(Participant arg0) {
		super(arg0);
		timer.register();
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !sprinkle_cooldown.isCooldown()) {
			if (!getPlayer().getLocation().getBlock().isLiquid()) {
				getPlayer().getLocation().getBlock().setType(Material.WATER);
				sprinkle_cooldown.start();
				return true;
			}
		}
		else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !queak_cooldown.isCooldown() && !queak_duration.isDuration()) {
			int water = 0;
			for (Block block : LocationUtil.getBlocks3D(getPlayer().getLocation(), 10, false, false)) {
				if (isWater(block)) {
					block.setType(Material.AIR);
					water++;
				}
			}

			if (water >= 0) {
				int duration = Math.min(QUEAK_MAX_DRATION.getValue() * water / 10, QUEAK_MAX_DRATION.getValue());
				queak_duration.start();
				queak_duration.setCount(duration);
				return true;
			}
			getPlayer().sendMessage("[§c!§f] 주변에 흡수할 물이 부족합니다.");
		}
		return false;
	}

	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			timer.setPeriod(TimeUnit.TICKS, 1).start();
			actionbarChannel.update("§b" + Strings.repeat("●", moisture) + "§7" + Strings.repeat("○", 10 - moisture));
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

	@SubscribeEvent(childs = {EntityDamageByEntityEvent.class, EntityDamageByBlockEvent.class})
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && isWater(getPlayer().getLocation().getBlock())) {
			e.setDamage(e.getDamage() - 1);
		}
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
				if (isWater(block)) {
					block.setType(Material.AIR);
				}
			}
		}
	}
}