package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.NMSUtil;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.text.DecimalFormat;
import java.util.HashMap;

@AbilityManifest(name = "복수", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 최근에 플레이어에게 받았던 대미지의 $[PERCENTAGE]% 만큼의",
		"§l고정 대미지§f를 상대방에게 추가적으로 입힙니다.",
})
public class Revenge extends CokesAbility {
	public static final Config<Double> PERCENTAGE = Config.of(Revenge.class, "반사대미지(%)", 40d, FunctionalInterfaces.positive());
	private final DecimalFormat df = new DecimalFormat("0.##");
	private double magicfixed_damage = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final HashMap<Player, AdditionalHit> map = new HashMap<>();

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
		Entity damager = CokesUtil.getDamager(e.getDamager());
		if (e.getEntity().equals(getPlayer()) && damager instanceof Player) {
			magicfixed_damage = e.getFinalDamage() * PERCENTAGE.getValue() / 100.0;
			ac.update(ChatColor.BLUE + " 고정 대미지 : " + df.format(magicfixed_damage));
		} else {
			if (e.getCause() == DamageCause.VOID) return;
			if (damager.equals(getPlayer()) && e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer())) {
				Player target = (Player) e.getEntity();
				if (map.containsKey(target)) {
					e.setCancelled(true);
				} else {
					AdditionalHit hit = new AdditionalHit(target, magicfixed_damage);
					hit.start();
					map.put(target, hit);
				}
			}
		}
	}

	private class AdditionalHit extends AbilityTimer implements Listener {
		private final Player player;
		private final double damage;
		public AdditionalHit(Player player, double damage) {
			super(10);
			this.player = player;
			this.damage = damage;
			setPeriod(TimeUnit.TICKS, 1);
		}

		public void onStart() {
			if (!player.isDead() && Damages.canDamage(player, getPlayer(), DamageCause.VOID, damage)) {
				player.setNoDamageTicks(0);
				NMSUtil.damageVoid(player, (float) magicfixed_damage);
			} else {
				this.stop(true);
			}
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		public void run(int a) {
			if (player.getNoDamageTicks() == 0) {
				this.stop(false);
			}
		}

		public void onEnd() {
			HandlerList.unregisterAll(this);
			map.remove(player);
		}

		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			map.remove(player);
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(player) && isRunning()) {
				e.setCancelled(true);
			}
		}
	}
}
