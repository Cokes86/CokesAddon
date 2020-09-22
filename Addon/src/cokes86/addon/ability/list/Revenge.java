package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.text.DecimalFormat;

@AbilityManifest(name = "복수", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 최근에 플레이어에게 받았던 대미지의 $[per]% 만큼의 고정대미지를 상대방에게 추가적으로 입힙니다."
})
public class Revenge extends CokesAbility {
	public static Config<Integer> per = new Config<Integer>(Revenge.class, "반사대미지(%)", 40) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	DecimalFormat df = new DecimalFormat("0.00");
	double finalDamage = 0;
	boolean damage = false;
	ActionbarChannel ac = newActionbarChannel();

	public Revenge(Participant participant) {
		super(participant);
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(ChatColor.BLUE + "반사고정대미지 : " + df.format(finalDamage * per.getValue() / (double) 100));
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && (e.getDamager() instanceof Player || e.getDamager() instanceof Arrow)) {
			finalDamage = e.getFinalDamage();
			ac.update(ChatColor.BLUE + "반사고정대미지 : " + df.format(finalDamage * per.getValue() / 100.00));
		} else {
			Entity damager = e.getDamager();
			if (damager instanceof Arrow) {
				Arrow arrow = (Arrow) damager;
				if (arrow.getShooter() instanceof Entity) {
					damager = (Entity) arrow.getShooter();
				}
			}

			if (damager.equals(getPlayer()) && e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer())) {
				float plus = (float) (finalDamage * per.getValue() / 100.0f);
				damage = !damage;
				e.setCancelled(damage);
				if (damage) {
					Damages.damageFixed(e.getEntity(), getPlayer(), (float) (e.getFinalDamage() + plus));
				}
			}
		}
	}
}
