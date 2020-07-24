package cokes86.addon.ability.list;

import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "부활",
		rank = Rank.S,
		species = Species.DEMIGOD,
		explain = {"자신의 체력이 0이 되었을 시 모든 체력을 회복하고 모든 버프가 사라집니다.",
		"이후 게임 스폰으로 이동합니다. (게임 스폰으로 이동할 수 없을 경우, $[cool]간 무적이 됩니다.)",
		"이 능력이 발동될 경우 비활성화됩니다."}
)
public class Resurrection extends AbilityBase {
	public static boolean spawn = Settings.getSpawnEnable();
	public static Config<Integer> cool = new Config<Integer>(Resurrection.class,"무적시간",1, 2) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	AbilityTimer t = new AbilityTimer(cool.getValue()) {
		@Override
		protected void run(int Count) {
		}

		@Override
		protected void onEnd() {
			Resurrection.this.setRestricted(true);
		}
	};

	public Resurrection(Participant arg0) {
		super(arg0);
	}

	public void clearPotionEffect() {
		for (PotionEffect e : getPlayer().getActivePotionEffects()) {
			getPlayer().removePotionEffect(e.getType());
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (!t.isRunning()) {
				double health = getPlayer().getHealth();
				double damage = e.getFinalDamage();
				if (health - damage <= 0) {
					e.setDamage(0);
					getPlayer().setFireTicks(0);
					SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.playSound(getPlayer());
					clearPotionEffect();
					getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
					if (spawn) {
						getPlayer().teleport(Settings.getSpawnLocation());
						Resurrection.this.setRestricted(true);
					} else {
						t.start();
					}
				}
			} else {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}
