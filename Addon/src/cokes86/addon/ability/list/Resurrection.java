package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.team.TeamGame;
import daybreak.abilitywar.game.team.interfaces.Members;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;

@AbilityManifest(name = "부활", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"치명적인 공격을 받았을 시 모든 체력을 회복하고 모든 버프가 사라집니다.",
		"$[explain]"
})
public class Resurrection extends CokesAbility {
	private static final Config<Integer> cool = new Config<Integer>(Resurrection.class, "무적시간", 1, Config.Condition.TIME) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	private static final Config<Boolean> respawn = new Config<Boolean>(Resurrection.class, "리스폰_이동", true){};
	@SuppressWarnings("unused")
	private static final Object explain = new Object() {
		public String toString() {
			String result;
			if (spawn) {
				result = "이후 게임 스폰으로 이동합니다. (게임 스폰으로 이동할 수 없을 경우, "+ cool +"간 무적이 됩니다.)";
			} else {
				result = "이후 "+ cool +"간 무적상태가 됩니다.";
			}
			return result;
		}
	};
	public static boolean spawn = Settings.getSpawnEnable();
	private boolean usable = true;
	private final AbilityTimer t = new AbilityTimer(cool.getValue()) {
		@Override
		protected void run(int Count) {
		}

		@Override
		protected void onEnd() {
			usable = false;
		}

		@Override
		protected void onSilentEnd() {
			usable = false;
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
		if (usable) {
			if (e.getEntity().equals(getPlayer())) {
				if (!t.isRunning()) {
					double health = getPlayer().getHealth();
					double damage = e.getFinalDamage();
					if (health - damage <= 0) {
						e.setDamage(0);
						getPlayer().setFireTicks(0);
						SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.playSound(getPlayer());
						clearPotionEffect();
						getPlayer().setHealth(AttributeUtil.getMaxHealth(getPlayer()));
						if (respawn.getValue() && spawn) {
							Location spawnLocation = Settings.getSpawnLocation().toBukkitLocation();
							if (getGame() instanceof TeamGame) {
								TeamGame teamGame = (TeamGame) getGame();
								Members team = teamGame.getTeam(getParticipant());
								if (team != null) spawnLocation = team.getSpawn().toBukkitLocation();
							}
							getPlayer().teleport(spawnLocation);
							usable = false;
						} else {
							t.start();
						}
					}
				} else {
					e.setCancelled(true);
				}
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
