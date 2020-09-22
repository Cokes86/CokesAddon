package cokes86.addon.ability.synergy;

import cokes86.addon.ability.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

@AbilityManifest(name = "사신의 화살", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.GOD, explain = {
		"매 $[duration]마다 사신의 낫이 1개씩 충전됩니다. (최대 5회)",
		"철괴 우클릭 시 사신의 화살을 장전하며, 발사할 수 있습니다. $[cool]",
		"죽음의 화살을 맞은 엔티티는 사신의 낫의 개수에 따라 최대체력에 비례한 고정대미지를 줍니다.",
		"1개: $[damage1]%, 2개: $[damage2]%, 3개: $[damage3]%, 4개: $[damage4]%, 5개: $[damage5]%"
})
@Beta
public class ReaperArrow extends CokesSynergy implements ActiveHandler {
	private static final Config<Integer> duration = new Config<Integer>(ReaperArrow.class, "충전시간", 60, 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(ReaperArrow.class, "쿨타임", 60, 1) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, damage1 = new Config<Integer>(ReaperArrow.class, "체력비례대미지.1스택", 10) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0 && value <= 100;
		}
	}, damage2 = new Config<Integer>(ReaperArrow.class, "체력비례대미지.2스택", 25) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0 && value <= 100;
		}
	}, damage3 = new Config<Integer>(ReaperArrow.class, "체력비례대미지.3스택", 50) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0 && value <= 100;
		}
	}, damage4 = new Config<Integer>(ReaperArrow.class, "체력비례대미지.4스택", 75) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0 && value <= 100;
		}
	}, damage5 = new Config<Integer>(ReaperArrow.class, "체력비례대미지.5스택", 95) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0 && value <= 100;
		}
	};
	private static final int[] stackDamage;

	static {
		if (damage1.getValue() > damage2.getValue() || damage2.getValue() > damage3.getValue() || damage3.getValue() > damage4.getValue() || damage4.getValue() > damage5.getValue()) {
			damage1.setValue(damage1.getDefaultValue());
			damage2.setValue(damage2.getDefaultValue());
			damage3.setValue(damage3.getDefaultValue());
			damage4.setValue(damage4.getDefaultValue());
			damage5.setValue(damage5.getDefaultValue());
		}
		stackDamage = new int[]{damage1.getValue(), damage2.getValue(), damage3.getValue(), damage4.getValue(), damage5.getValue()};
	}

	AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac = newActionbarChannel();
	ParticleLib.RGB rgb = ParticleLib.RGB.of(1, 1, 1);
	Cooldown cooldown = new Cooldown(cool.getValue());
	private boolean ready = false;
	private Arrow reaperArrow = null;
	AbilityTimer effect = new AbilityTimer() {
		protected void run(int arg) {
			ParticleLib.REDSTONE.spawnParticle(reaperArrow.getLocation(), rgb);
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	private int stack = 0;
	AbilityTimer chargeTimer = new AbilityTimer() {
		protected void run(int arg) {

			if (arg % duration.getValue() == 0) {
				stack++;
			}
			ac.update("사신의 낫: " + stack);
		}
	};

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
		if (ready && e.getProjectile() instanceof Arrow) {
			SoundLib.BLOCK_GRASS_BREAK.playSound(getPlayer());
			this.reaperArrow = (Arrow) e.getProjectile();
			this.ready = false;
			effect.start();
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(reaperArrow) && e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
			e.setCancelled(true);
			AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (attribute != null) {
				double max_Health = attribute.getValue();
				Damages.damageFixed(entity, getPlayer(), (float) (max_Health * stackDamage[stack - 1]));
			}
		}
	}

	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity().equals(reaperArrow)) {
			effect.stop(false);
			reaperArrow = null;
		}
	}
}
