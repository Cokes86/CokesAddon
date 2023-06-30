package com.cokes86.cokesaddon.ability.synergy.list;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

@AbilityManifest(name = "사신의 화살", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.GOD, explain = {
		"매 $[DURATION]마다 사신의 낫이 1개씩 충전됩니다. (최대 5회)",
		"철괴 우클릭 시 죽음의 화살을 장전하며, 발사할 수 있습니다. $[COOLDOWN]",
		"죽음의 화살을 맞은 엔티티는 사신의 낫의 개수에 따라 최대체력에 비례한 관통 대미지를 줍니다.",
		"1개: $[DAMAGE1]%, 2개: $[DAMAGE2]%, 3개: $[DAMAGE3]%, 4개: $[DAMAGE4]%, 5개: $[DAMAGE5]%"
})
public class ReaperArrow extends CokesSynergy implements ActiveHandler {
	private static final Config<Integer> DURATION = Config.time(ReaperArrow.class, "charge-time", 60,
			"# 사신의 낫이 충전되는 시간",
			"# 기본값 : 60 (초)");
	private static final Config<Integer> COOLDOWN = Config.cooldown(ReaperArrow.class, "cooldown", 60,
	"# 쿨타임",
	"# 기본값 : 60 (초)");
	private static final Config<Double> VALUE_1 = Config.of(ReaperArrow.class, "damage-value1", 10.0, FunctionalInterfaces.chance(true, true));
	private static final Config<Double> VALUE_2 = Config.of(ReaperArrow.class, "damage-value2", 25.0, FunctionalInterfaces.chance(true, true));
	private static final Config<Double> VALUE_3 = Config.of(ReaperArrow.class, "damage-value3", 50.0, FunctionalInterfaces.chance(true, true));
	private static final Config<Double> VALUE_4 = Config.of(ReaperArrow.class, "damage-value4", 75.0, FunctionalInterfaces.chance(true, true));
	private static final Config<Double> VALUE_5 = Config.of(ReaperArrow.class, "damage-value5", 95.0, FunctionalInterfaces.chance(true, true));

	private static final double DAMAGE1, DAMAGE2, DAMAGE3, DAMAGE4, DAMAGE5;

	static {
		double[] stackDamage = {VALUE_1.getValue(), VALUE_2.getValue(), VALUE_3.getValue(), VALUE_4.getValue(), VALUE_5.getValue()};
		Arrays.sort(stackDamage);
		DAMAGE1 = stackDamage[0];
		DAMAGE2 = stackDamage[1];
		DAMAGE3 = stackDamage[2];
		DAMAGE4 = stackDamage[3];
		DAMAGE5 = stackDamage[4];
	}

	private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac = newActionbarChannel();
	private final RGB rgb = RGB.of(1, 1, 1);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private boolean ready = false;
	private Projectile reaperArrow = null;
	private final AbilityTimer effect = new AbilityTimer() {
		protected void run(int arg) {
			ParticleLib.REDSTONE.spawnParticle(reaperArrow.getLocation(), rgb);
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	private int stack = 0;
	private final AbilityTimer chargeTimer = new AbilityTimer() {
		protected void run(int arg) {
			if (cooldown.isRunning()) {
				ac.update("");
				return;
			}

			int reloadCount = Wreck.isEnabled(GameManager.getGame()) ? (int) (Wreck.calculateDecreasedAmount(20) * DURATION.getValue()) : DURATION.getValue();
			if (reloadCount == 0) reloadCount = (int) (0.2 * DURATION.getValue());
			if (arg % reloadCount == 0) {
				if (stack >= 5) return;
				stack++;
			}
			ac.update("사신의 낫: " + stack);
		}
	}.register();

	public ReaperArrow(AbstractGame.Participant participant) {
		super(participant);
		effect.register();
		chargeTimer.register();
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			chargeTimer.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !ready && stack > 0 && !cooldown.isCooldown()) {
			getPlayer().sendMessage("사신의 화살을 장전하였습니다.");
			ready = true;
			return true;
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (ready && NMS.isArrow(e.getProjectile()) && e.getEntity().equals(getPlayer())) {
			SoundLib.BLOCK_GRASS_BREAK.playSound(getPlayer());
			this.reaperArrow = (Projectile) e.getProjectile();
			this.ready = false;
			effect.start();
			cooldown.start();
		}
	}

	@SubscribeEvent
	public void onCEntityDamage(EntityDamageByEntityEvent e) {
		if (e.getDamager() != null && e.getDamager().equals(reaperArrow) && e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
			e.setCancelled(true);
			AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (attribute != null) {
				double max_Health = attribute.getValue();
				double percentage = stack == 5 ? DAMAGE5 : (stack == 4 ? DAMAGE4 : (stack == 3 ? DAMAGE3 : (stack == 2 ? DAMAGE2 : (stack == 1 ? DAMAGE1 : 0))));
				Damages.damageFixed(entity, getPlayer(), (float) (max_Health * ((percentage)/100.0f)));
				stack = 0;
			}
		}
	}

	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity().equals(reaperArrow)) {
			effect.stop(false);
			reaperArrow = null;
			stack = 0;
		}
	}
}
