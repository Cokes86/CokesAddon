package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Strings;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "물고기", rank = Rank.B, species = Species.ANIMAL, explain = {
		"§7능력 활성화 §8- §c철푸덕§r: 물갈퀴II 인챈트 쿠폰을 획득, 신발 착용중 쿠폰 우클 시 해당 인챈트가 부여됨.",
		"§7물 속 §8- §c첨벙첨벙§r: 이동속도 1.5배. 수중호흡 효과를 얻으며 받는 대미지가 1 감소.",
		"§7물 밖 §8- §c파닥파닥§r: 구속효과가 걸리며 이후 3초마다 수분이 감소. 감소할 수분이 없을 경우 1의 고정피해.",
		"§7철괴 좌클릭 §8- §c촤아악§r: 자신의 위치에 물을 설치. $[cool]"
})
public class Fish extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> cool =  new Config<Integer>(Fish.class, "쿨타임", 30, 1) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}		
	};
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown c = new Cooldown(cool.getValue());
	private int moisture = 10;
	
	private static final ItemStack coupon = new ItemBuilder(MaterialX.PAPER).displayName("§a물갈퀴III 인챈트 쿠폰").build();
	private boolean kit = true;

	public Fish(Participant arg0) {
		super(arg0);
		timer.register();
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !c.isCooldown()) {
			getPlayer().getLocation().clone().getBlock().setType(Material.WATER);
			c.start();
			return true;
		}
		return false;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			timer.setPeriod(TimeUnit.TICKS, 1).start();
			if (kit) {
				getPlayer().getInventory().addItem(coupon);
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
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
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
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
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getLocation().getBlock().getType().name().endsWith("WATER")) {
			e.setDamage(e.getDamage()-1);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getFrom().getBlock().getType().name().endsWith("WATER") && e.getTo().getBlock().getType().name().endsWith("WATER")) {
			getPlayer().setVelocity(getPlayer().getLocation().getDirection().multiply(1.5));
		}
	}
	
	private AbilityTimer timer = new AbilityTimer() {
		int count = 0;
		int up = 0;

		@Override
		protected void run(int arg0) {
			if (getPlayer().getLocation().getBlock().getType().name().endsWith("WATER")) {
				if (count != 0) {
					PotionEffects.SLOW.removePotionEffect(getPlayer());
					count = 0;
				}
				up++;
				if (up % 20 == 0 && moisture < 10) {
					moisture++;
					ac.update("§b".concat(Strings.repeat("●", moisture)).concat(Strings.repeat("○", 10 - moisture)));
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
						ac.update("§b".concat(Strings.repeat("●", moisture)).concat(Strings.repeat("○", 10 - moisture)));
					} else {
						Damages.damageFixed(getPlayer(), getPlayer(), 1);
					}
				}
			}
		}
		
	};
}
