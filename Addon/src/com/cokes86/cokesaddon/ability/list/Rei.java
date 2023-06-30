package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
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
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@AbilityManifest(name = "레이", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c영혼의 검성§f: 근접 공격 시 최대 체력의 $[COST]%를 소비하고",
		"  상대에게 주는 대미지가 §a$[DAMAGE]% 증가합니다.",
		"  §c치명적인 대미지§f를 입었을 경우, 그 공격을 §b무효§f로 하고",
		"  $[RESPAWN]의 체력으로 §b부활홥니다. $[COOLDOWN]",
		"  §c쿨타임§f동안, 또는 소비할 체력이 없을 경우 <§c영혼의 검성§f>은 발동하지 않습니다."
})
@Tips(tip = {
		"추가적인 공격력이 더해져 강한 공격력을 가진 능력",
		"자해라는 디메리트가 있지만 이를 상쇠하는 부활이 있어",
		"감안해서라도 플레이가 가능함."
}, strong= {
		@Description(explain = { "공격력이 강해 상대방에게 더욱 큰 대미지를 줄 수 있다." }, subject = "강한 공격력")
}, weak = {
		@Description(explain = { "고정적인 체력 손해로 장기전으로 갈 수록 불리해진다." }, subject = "고정적인 체력 지불")
},
stats = @Stats(offense = Level.NINE, survival = Level.FIVE, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.THREE), difficulty = Difficulty.NORMAL)
public class Rei extends CokesAbility {
	private static final Config<Double> DAMAGE = Config.of(Rei.class, "damage-increment", 65.0, FunctionalInterfaces.positive(),
			"# 영혼의 검성으로 증가하는 대미지",
			"# 기본값: 65.0 (%)");
	private static final Config<Double> COST = Config.of(Rei.class, "coast", 4.5, FunctionalInterfaces.positive(),
			"# 영혼의 검성으로 소모할 체력",
			"# 기본값: 4.5 (%)");
	private static final Config<Integer> COOLDOWN = Config.of(Rei.class, "cooldown", 100, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
			"# 영혼의 검성 쿨타임",
			"# 기본값: 100 (초)");
	private static final Config<Integer> RESPAWN = Config.of(Rei.class, "respawn-health", 4, FunctionalInterfaces.positive(),
			"# 영혼의 검성 중 치명적인 대미지를 받을 시 부활 체력",
			"# 기본값: 4");

	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._75);

	public Rei(Participant participant) {
		super(participant);
	}

	@SubscribeEvent(priority = 999)
	public void onBeforeDeath(CEntityDamageEvent e) {
		Entity damager = e.getDamager();
		if (damager == null) return;

		if (e.getEntity().equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled()) {
			double damage = e.getFinalDamage();
			if (getPlayer().getHealth() - damage <= 0) {
				e.setDamage(0);
				getPlayer().setHealth(RESPAWN.getValue());
				cooldown.start();
				SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {

		Entity damager = e.getDamager();
		if (damager == null) return;

		if (e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer()) && damager.equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled()) {
			final double maxHealth = AttributeUtil.getMaxHealth(getPlayer()), health = getPlayer().getHealth();
			final float absorption = NMS.getAbsorptionHearts(getPlayer());
			final double damage = maxHealth * COST.getValue() / 100.0f;
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
				e.setDamage(e.getDamage() * (1 + DAMAGE.getValue() / 100.0));
			}
		}
	}

	@SubscribeEvent(priority = 5)
	private void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer()) && !cooldown.isRunning() && !e.isCancelled() && e.getHealth() <= 0) {
			e.setCancelled(true);
			getPlayer().setHealth(RESPAWN.getValue());
			cooldown.start();
			SoundLib.ITEM_TOTEM_USE.playSound(getPlayer());
		}
	}
}
