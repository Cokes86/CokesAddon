package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;

@AbilityManifest(name = "부활", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"치명적인 공격을 받았을 시 모든 체력을 회복하고 모든 버프가 사라집니다.",
		"그 이후 모든 공격에 $[BONUS_DAMAGE]의 대미지를 추가로 줍니다."
})
public class Resurrection extends CokesAbility {
	private static final Config<Double> BONUS_DAMAGE = new Config<>(Resurrection.class, "추가대미지", 2.0, a -> a > 0);
	private boolean resurrection = false;

	public Resurrection(Participant arg0) {
		super(arg0);
	}

	public void clearPotionEffect() {
		for (PotionEffect e : getPlayer().getActivePotionEffects()) {
			getPlayer().removePotionEffect(e.getType());
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamage(EntityDamageEvent e) {
		if (!resurrection) {
			if (e.getEntity().equals(getPlayer())) {
				double health = getPlayer().getHealth();
				double damage = e.getFinalDamage();
				if (health - damage <= 0) {
					e.setDamage(0);
					getPlayer().setFireTicks(0);
					SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
					clearPotionEffect();
					getPlayer().setHealth(AttributeUtil.getMaxHealth(getPlayer()));
					resurrection = true;
				}
			}
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		if (resurrection) {
			Entity damager = e.getDamager();
			if (NMS.isArrow(damager)) {
				Projectile arrow = (Projectile) damager;
				if (arrow.getShooter() instanceof Entity) {
					damager = (Entity) arrow.getShooter();
				}
			}
			if (damager.equals(getPlayer())) {
				e.setDamage(e.getDamage() + BONUS_DAMAGE.getValue());
			}
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}
