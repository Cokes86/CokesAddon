package cokes86.addon.ability.list;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.compat.nms.Hologram;
import daybreak.abilitywar.utils.base.minecraft.compat.nms.NMSHandler;
import daybreak.google.common.base.Strings;

@AbilityManifest(name="오비스니", rank = Rank.B, species = Species.HUMAN , explain= {
		"상대방을 공격할 시 상대방에게 §2맹독 카운터§f를 1씩 상승시키며",
		"상대방은 매 10초마다 §2맹독 카운터§f만큼의 대미지를 수시로 입습니다.",
		"철괴 우클릭시 모든 플레이어의 §2맹독 카운터§f를 없애고",
		"그 수의 2배만큼의 대미지를 입힙니다. $[cool]",
		"각각 플레이어마다 §2맹독 카운터§f는 최대 $[max]씩 쌓입니다.",
		"철괴 좌클릭시 모든 플레이어의 §2맹독 카운터§f를 알 수 있습니다.",
		"0개는 따로 표시하지 않습니다."
})
public class Ovisni extends AbilityBase implements ActiveHandler {
	public static Config<Integer> cool = new Config<Integer>(Ovisni.class, "쿨타임", 30, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, max = new Config<Integer>(Ovisni.class, "최대카운터", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	
	Map<Player, Integer> ovisniCounter = new HashMap<>();
	Map<Player, HologramTimer> ovisniTimer = new HashMap<>();
	
	CooldownTimer cooldown = new CooldownTimer(cool.getValue());
	Timer ovisni = new Timer() {

		@Override
		protected void run(int arg0) {
			for (Player p : ovisniCounter.keySet()) {
				if (arg0 % 10 == 0) {
					p.damage(ovisniCounter.get(p), getPlayer());
				}
			}
		}
		
	};

	public Ovisni(Participant arg0) {
		super(arg0);
	}
	
	public void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			ovisni.start();
		default:
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			for (Player p : ovisniCounter.keySet()) {
				p.damage(ovisniCounter.get(p)*2, getPlayer());
				ovisniTimer.get(p).stop(false);
			}
			ovisniCounter.clear();
			ovisniTimer.clear();
			cooldown.start();
		} else if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK)) {
			getPlayer().sendMessage("§e===== §2맹독 카운터§f 수치 §e=====");
			for (Player target : ovisniCounter.keySet()) {
				getPlayer().sendMessage("§f" + target.getName() + " §7: §2" + ovisniCounter.get(target));
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}
		
		if (damager.equals(getPlayer()) && getGame().isGameStarted() && e.getEntity() instanceof Player) {
			if (getGame().isParticipating((Player) e.getEntity())) {
				ovisniCounter.put((Player) e.getEntity(), Math.min(ovisniCounter.getOrDefault((Player) e.getEntity(), 0)+1, max.getValue()));
				ovisniTimer.put((Player) e.getEntity(), new HologramTimer((Player) e.getEntity()));
			}
		}
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			for (Player p : ovisniTimer.keySet()) {
				ovisniTimer.get(p).stop(false);
			}
			ovisniCounter.clear();
			ovisniTimer.clear();
		} else if (ovisniCounter.containsKey(e.getPlayer())) {
			ovisniCounter.remove(e.getPlayer());
			ovisniTimer.get(e.getPlayer()).stop(false);
			ovisniTimer.remove(e.getPlayer());
		}
	}
	
	private class HologramTimer extends Timer {
		private final Player entity;
		private final Hologram hologram;
		
		private HologramTimer(Player entity) {
			super();
			setPeriod(TimeUnit.TICKS, 2);
			this.entity = entity;
			this.hologram = NMSHandler.getNMS().newHologram(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY() + entity.getEyeHeight() + 0.6, entity.getLocation().getZ());
			this.hologram.setText("");
			
		}

		@Override
		protected void run(int arg0) {
			this.hologram.setText(Strings.repeat("§2◆", ovisniCounter.get(entity)).concat(Strings.repeat("§2◇", max.getValue() - ovisniCounter.get(entity))));
			hologram.teleport(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY() + entity.getEyeHeight() + 0.6, entity.getLocation().getZ(), entity.getLocation().getYaw(), 0);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hologram.hide(getPlayer());
			ovisniTimer.remove(entity);
		}
	}
}
