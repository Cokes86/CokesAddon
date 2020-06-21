package cokes86.addon.ability.list;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
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
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;

@AbilityManifest(name="오비스니", rank = Rank.B, species = Species.HUMAN , explain= {
		"상대방을 공격할 시 상대방에게 §2맹독 카운터§f를 1씩 상승시키며",
		"상대방은 매 10초마다 §2맹독 카운터§f만큼의 대미지를 수시로 입습니다.",
		"철괴 우클릭시 모든 플레이어의 §2맹독 카운터§f를 없애고",
		"그 수의 2배만큼의 대미지를 입힙니다. $[cool]",
		"각각 플레이어마다 §2맹독 카운터§f는 최대 $[max]씩 쌓입니다."
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
	
	CooldownTimer cooldown = new CooldownTimer(cool.getValue());
	Timer ovisni = new Timer() {

		@Override
		protected void run(int arg0) {
			for (Player p : ovisniCounter.keySet()) {
				if (arg0 % 10 == 0) {
					p.damage(ovisniCounter.get(p), getPlayer());
				}
				for (Location location : Circle.iteratorOf(p.getLocation(), 1, ovisniCounter.get(p)).iterable()) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, new RGB(0,255,0));
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
			}
			ovisniCounter.clear();
			cooldown.start();
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && getGame().isGameStarted() && e.getEntity() instanceof Player) {
			if (getGame().isParticipating((Player) e.getEntity())) {
				ovisniCounter.put((Player) e.getEntity(), Math.min(ovisniCounter.getOrDefault((Player) e.getEntity(), 0)+1, max.getValue()));
			}
		}
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (ovisniCounter.containsKey(e.getPlayer())) {
			ovisniCounter.remove(e.getPlayer());
		}
	}
}
