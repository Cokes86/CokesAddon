package cokes86.addon.ability.list;

import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
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
		"쿨타임이 아닐 때 상대방을 공격할 시 최대 체력의 $[cost]%를 코스트로",
		"$[damage]만큼의 추가 대미지를 입힙니다.",
		"자신의 체력이 0이 되었을 시 그 공격을 무효로 하고 체력이 $[respawn] 됩니다. $[cool]",
		"코스트로 지불할 체력이 부족한 경우 코스트를 소비하지 않습니다.",
		"※제작자 자캐 기반 능력자"
})
public class Rei extends AbilityBase {
	private static final Config<Double> damage = new Config<Double>(Rei.class, "추가대미지", 3.0) {
		public boolean condition(Double value) {
			return value >= 0.0;
		}
	}, cost = new Config<Double>(Rei.class, "코스트(%)", 5.0) {
		public boolean condition(Double value) {
			return value > 0.0;
		}
	};
	private static final Config<Integer> cool = new Config<Integer>(Rei.class, "쿨타임", 100, 1) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, respawn = new Config<Integer>(Rei.class, "부활체력", 4) {
		public boolean condition(Integer value) {
			return value > 0;
		}

		public String toString() {
			return getValue().toString() + KoreanUtil.getJosa(getValue().toString(), KoreanUtil.Josa.이가);
		}
	};

	public Rei(Participant participant) {
		super(participant);
	}

	Cooldown c = new Cooldown(cool.getValue());

	@SubscribeEvent(priority = 5)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer()) && damager.equals(getPlayer()) && !c.isRunning()) {
			e.setDamage(e.getDamage() + damage.getValue());
			double max_Health = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
			double health = getPlayer().getHealth();
			float Absorption = NMS.getAbsorptionHearts(getPlayer());
			double damage = max_Health * cost.getValue() / 100.0f;
			if (getPlayer().getGameMode().equals(GameMode.SURVIVAL) || getPlayer().getGameMode().equals(GameMode.ADVENTURE)) {
				if (Absorption >= damage) {
					NMS.setAbsorptionHearts(getPlayer(), (float) (Absorption - damage));
				} else {
					double temp = damage - Absorption;
					if (health > temp) {
						NMS.setAbsorptionHearts(getPlayer(), 0);
						getPlayer().setHealth(Math.max(0.0, health - temp));
					}
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
		if (e.getEntity().equals(getPlayer()) && !c.isRunning() && !e.isCancelled()) {
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
