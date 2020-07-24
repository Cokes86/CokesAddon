package cokes86.addon.ability.list;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.game.manager.object.DeathManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.library.ParticleLib;

import java.util.function.Predicate;

@AbilityManifest(name = "하모니", rank = Rank.C, species = Species.HUMAN, explain= {
		"$[duration]마다 주변 10블럭 이내의 플레이어의 수/2 만큼 체력을 회복하고,",
		"그 주변 플레이어 역시 0.5의 체력을 증가시켜줍니다."
})
public class Harmony extends AbilityBase {
	private static final Config<Integer> duration = new Config<Integer>(Harmony.class, "주기", 5, 2){
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				return !game.getDeathManager().isExcluded(entity.getUniqueId());
			}
			return getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue();
		}
		return true;
	};

	public Harmony(Participant arg0) {
		super(arg0);
		passive.register();
	}
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int arg0) {
			int a = 0;
			for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
				a++;
				p.setHealth(Math.min(p.getHealth()+0.5, Settings.getDefaultMaxHealth()));
				for (Location l : Line.iteratorBetween(getPlayer().getLocation(), p.getLocation(), 10).iterable()) {
					ParticleLib.HEART.spawnParticle(l);
				}
			}
			getPlayer().setHealth(Math.min(getPlayer().getHealth()+a/2.0, Settings.getDefaultMaxHealth()));
		}
	}.setPeriod(TimeUnit.SECONDS, duration.getValue());
}
