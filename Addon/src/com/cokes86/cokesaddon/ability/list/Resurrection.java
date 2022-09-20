package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;

@AbilityManifest(name = "부활", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"치명적인 공격을 받았을 시 모든 체력을 회복하고 모든 버프가 사라집니다.",
		"그 이후 모든 공격이 상대에게 주는 대미지가 $[BONUS_DAMAGE] 증가합니다."
})
public class Resurrection extends CokesAbility {
	private static final Config<Double> BONUS_DAMAGE = Config.of(Resurrection.class, "추가대미지", 2.0, FunctionalInterfaces.positive());
	private boolean resurrection = false;

	public Resurrection(Participant arg0) {
		super(arg0);
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (!resurrection) {
			if (e.getEntity().equals(getPlayer())) {
				if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
					e.setDamage(0);
					getPlayer().setFireTicks(0);
					SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
					for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
						getPlayer().removePotionEffect(effect.getType());
					}
					getPlayer().setHealth(AttributeUtil.getMaxHealth(getPlayer()));
					resurrection = true;
				}
			}
		} else {
			Entity damager = CokesUtil.getDamager(e.getDamager());
			if (damager == null) return;

			if (damager.equals(getPlayer())) {
				e.setDamage(e.getDamage() + BONUS_DAMAGE.getValue());
			}
		}
	}
}