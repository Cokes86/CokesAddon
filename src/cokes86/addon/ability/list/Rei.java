package cokes86.addon.ability.list;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "레이", rank = Rank.S, species = Species.HUMAN, explain = {
		"쿨타임이 아닐 때 상대방을 공격할 시 최대 체력의 $[cost]%를 코스트로", "$[damage]만큼의 추가 대미지를 입힙니다.",
		"자신의 체력이 0이 되었을 시 그 공격을 무효로 하고 체력이 $[respawn]이 됩니다. $[cool]", "※제작자 자캐 기반 능력자" })
public class Rei extends AbilityBase {
	private static final Config<Double> damage = new Config<Double>(Rei.class, "추가대미지", 3.0) {
		@Override
		public boolean Condition(Double value) {
			return value >= 0.0;
		}
	}, cost = new Config<Double>(Rei.class, "코스트(%)", 10.0) {
		@Override
		public boolean Condition(Double value) {
			return value > 0.0;
		}
	};
	private static final Config<Integer> cool = new Config<Integer>(Rei.class, "쿨타임", 100, 1) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, respawn = new Config<Integer>(Rei.class, "부활체력", 4) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	public Rei(Participant participant) {
		super(participant);
	}

	CooldownTimer c = new CooldownTimer(cool.getValue());

	@SubscribeEvent(priority = 5)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		if (e.getEntity() instanceof Player) {
			if (e.getDamager().equals(getPlayer())) {
				Player Damager = (Player) e.getDamager();
				if (!c.isRunning()) {
					double finald = e.getDamage() + damage.getValue();
					e.setDamage(finald);
					Damager.setHealth(Math.max(0.0, Damager.getHealth() - Damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() * cost.getValue() / 100.00));
				}
			} else if (e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				if (a.getShooter().equals(getPlayer()) && !c.isRunning()) {
					Player Damager = (Player) a.getShooter();
					double finald = e.getDamage() + damage.getValue();
					e.setDamage(finald);
                    Damager.setHealth(Math.max(0.0, Damager.getHealth() - Damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() * cost.getValue() / 100.00));
				}
			}
		}
	}

	@SubscribeEvent(priority = 5)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent(priority = 5)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !c.isRunning()) {
			double damage = e.getFinalDamage();
			if (getPlayer().getHealth() - damage <= 0) {
				e.setDamage(0);
				getPlayer().setHealth(respawn.getValue());
				c.start();
				SoundLib.ENTITY_FIREWORK_ROCKET_LAUNCH.playSound(getPlayer());
			}
		}
	}
}
