package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.text.DecimalFormat;

@AbilityManifest(name = "복수", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 최근에 플레이어에게 받았던 대미지의 $[PERCENTAGE]% 만큼의",
		"§l고정 대미지§f를 상대방에게 추가적으로 입힙니다.",
})
public class Revenge extends CokesAbility {
	public static final Config<Double> PERCENTAGE = Config.of(Revenge.class, "반사대미지(%)", 40d, FunctionalInterfaces.positive());
	private final DecimalFormat df = new DecimalFormat("0.##");
	private double magicfixed_damage = 0;
	private final ActionbarChannel ac = newActionbarChannel();

	public Revenge(Participant participant) {
		super(participant);
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(ChatColor.BLUE + "고정 대미지 : " + df.format(magicfixed_damage));
		}
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getDamager() == null) return;
		if (e.getEntity().equals(getPlayer()) && (e.getDamager() instanceof Player || NMS.isArrow(e.getDamager()))) {
			magicfixed_damage = e.getFinalDamage() * PERCENTAGE.getValue() / 100;
			ac.update(ChatColor.BLUE + " 고정 대미지 : " + df.format(magicfixed_damage));
		} else {
			Entity damager = CokesUtil.getDamager(e.getDamager());
			if (e.getCause() == DamageCause.VOID) return;
			Player target = (Player) e.getEntity();
			if (damager.equals(getPlayer()) && e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
				new AbilityTimer(1) {
					public void run(int arg0) {
						if (!target.isDead() && Damages.canDamage(target, getPlayer(), DamageCause.VOID, (float) magicfixed_damage)) {
							target.setNoDamageTicks(0);
							NMSUtil.damageVoid(target, (float) magicfixed_damage);
							target.setNoDamageTicks(9);
						}
					}
				}.setInitialDelay(TimeUnit.TICKS, 1).setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}
}
