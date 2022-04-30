package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.text.DecimalFormat;

@AbilityManifest(name = "복수", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 최근에 플레이어에게 받았던 대미지의 $[PERCENTAGE]% 만큼의 고정 마법 대미지를 상대방에게 추가적으로 입힙니다."
})
public class Revenge extends CokesAbility {
	public static final Config<Integer> PERCENTAGE = Config.of(Revenge.class, "반사대미지(%)", 40, FunctionalInterfaceUnit.positive());
	private final DecimalFormat df = new DecimalFormat("0.##");
	private double finalDamage = 0;
	private final ActionbarChannel ac = newActionbarChannel();

	public Revenge(Participant participant) {
		super(participant);
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(ChatColor.BLUE + "반사고정대미지 : " + df.format(finalDamage * PERCENTAGE.getValue() / (double) 100));
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && (e.getDamager() instanceof Player || NMS.isArrow(e.getDamager()))) {
			finalDamage = e.getFinalDamage();
			ac.update(ChatColor.BLUE + "반사고정대미지 : " + df.format(finalDamage * PERCENTAGE.getValue() / 100.00));
		} else {
			Entity damager = e.getDamager();
			if (NMS.isArrow(damager)) {
				Projectile arrow = (Projectile) damager;
				if (arrow.getShooter() instanceof Entity) {
					damager = (Entity) arrow.getShooter();
				}
			}
			if (e.getCause() == EntityDamageEvent.DamageCause.MAGIC) return;
			if (damager.equals(getPlayer()) && e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer())) {
				new AbilityTimer(1) {
					public void run(int arg0) {
						Player target = (Player) e.getEntity();
						if (!target.isDead()) {
							target.setNoDamageTicks(0);
							Damages.damageMagic(target, getPlayer(), true, (float) finalDamage);
						}
					}
				}.setInitialDelay(TimeUnit.TICKS, 1).setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}
}