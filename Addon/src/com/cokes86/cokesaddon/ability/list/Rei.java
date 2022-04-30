package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "레이", rank = Rank.L, species = Species.HUMAN, explain = {
		"쿨타임이 아닐 때 상대방을 공격할 시 최대 체력의 $[cost]%를 코스트로",
		"상대에게 주는 대미지가 $[damage]% 증가합니다.",
		"자신의 체력이 0이 되었을 시 그 공격을 무효로 하고 체력이 $[respawn] 됩니다. $[cool]",
		"코스트로 지불할 체력이 부족한 경우 코스트를 소비하지 않습니다."
})
@Tips(tip = {
		"추가적인 공격력이 더해져 강한 공격력을 가진 능력",
		"자해라는 디메리트가 있지만 이를 상쇠하는 부활이 있어",
		"감안해서라도 플레이가 가능함."
}, strong= {
		@Description(explain = { "공격력이 강해 상대방에게 더욱 큰 대미지를 줄 수 있다." }, subject = "강한 공격력")
}, weak = {
		@Description(explain = { "체력이 높을 수록 그만큼 디메리트역시 커진다." }, subject = "자신의 높은 체력")
},
stats = @Stats(offense = Level.NINE, survival = Level.FIVE, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.THREE), difficulty = Difficulty.NORMAL)
public class Rei extends CokesAbility {
	private static final Config<Double> damage = Config.of(Rei.class, "추가대미지", 30.0, FunctionalInterfaceUnit.positive());
	private static final Config<Double> cost = Config.of(Rei.class, "코스트(%)", 4.5, FunctionalInterfaceUnit.positive());
	private static final Config<Integer> cool = Config.of(Rei.class, "쿨타임", 100, Config.Condition.COOLDOWN);
	private static final Config<Integer> respawn = Config.of(Rei.class, "부활체력", 4, FunctionalInterfaceUnit.positive(),
			FunctionalInterfaceUnit.addJosa(Josa.이가));

	private final Cooldown cooldown = new Cooldown(cool.getValue(), CooldownDecrease._75);

	public Rei(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);

		Entity damager = e.getDamager();
		if (NMS.isArrow(damager)) {
			Projectile arrow = (Projectile) e.getDamager();
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer()) && damager.equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled()) {
			e.setDamage(e.getDamage() * (1 + damage.getValue() / 100.0));
			final double maxHealth = AttributeUtil.getMaxHealth(getPlayer()), health = getPlayer().getHealth();
			final float absorption = NMS.getAbsorptionHearts(getPlayer());
			final double damage = maxHealth * cost.getValue() / 100.0f;
			if (getPlayer().getGameMode().equals(GameMode.SURVIVAL) || getPlayer().getGameMode().equals(GameMode.ADVENTURE)) {
				if (absorption >= damage) {
					NMS.setAbsorptionHearts(getPlayer(), (float) (absorption - damage));
				} else {
					final double temp = damage - absorption;
					if (health > temp) {
						if (absorption != 0) NMS.setAbsorptionHearts(getPlayer(), 0);
						getPlayer().setHealth(Math.max(0.0, health - temp));
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = 5)
	private void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled() && e.getHealth() <= 0) {
			e.setCancelled(true);
			getPlayer().setHealth(respawn.getValue());
			cooldown.start();
			SoundLib.ENTITY_FIREWORK_ROCKET_LAUNCH.playSound(getPlayer());
		}
	}

	@SubscribeEvent(priority = 5, childs = {EntityDamageByBlockEvent.class})
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled()) {
			double damage = e.getFinalDamage();
			if (getPlayer().getHealth() - damage <= 0) {
				e.setDamage(0);
				getPlayer().setHealth(respawn.getValue());
				cooldown.start();
				SoundLib.ENTITY_FIREWORK_ROCKET_LAUNCH.playSound(getPlayer());
			}
		}
	}
}
