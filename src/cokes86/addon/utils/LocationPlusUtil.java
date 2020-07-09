package cokes86.addon.utils;

import java.util.function.Predicate;

import daybreak.abilitywar.game.interfaces.TeamGame;
import daybreak.abilitywar.game.manager.object.DeathManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManager;

public class LocationPlusUtil {
	@SuppressWarnings("unchecked")
	public static <T extends Entity> T getFarthestEntity(Class<T> entityType, Location center, Predicate<Entity> predicate) {
		double distance = Double.MIN_VALUE;
		T current = null;

		Location centerLocation = center.clone();
		for (Entity e : center.getWorld().getEntities()) {
			if (entityType.isAssignableFrom(e.getClass())) {
				T entity = (T) e;
				double compare = centerLocation.distanceSquared(entity.getLocation());
				if (compare > distance && (predicate == null || predicate.test(entity))) {
					distance = compare;
					current = entity;
				}
			}
		}

		return current;
	}
	
	public static Vector getForwardVector(Location location) {
		float yaw = location.getYaw(), pitch = location.getPitch();

		double radYaw = Math.toRadians(yaw), radPitch = Math.toRadians(pitch);

		double cosPitch = Math.cos(radPitch);

		double x = -Math.sin(radYaw) * cosPitch;
		double y = -Math.sin(radPitch);
		double z = Math.cos(radYaw) * cosPitch;

		Vector velocity = new Vector(x, y, z);
		return velocity.normalize();
	}
	
	public static Predicate<Entity> HAVE_ABILITY() {
		return entity -> {
			if (GameManager.isGameRunning() && entity instanceof Player) {
				AbstractGame game = GameManager.getGame();
				Player player = (Player)entity;
				return (game.isParticipating(player) && game.getParticipant(player).hasAbility());
			}
			return false;
		};
	}

	public static Predicate<Entity> STRICT(AbstractGame.Participant participant) {
		return entity -> {
			if (entity.equals(participant.getPlayer())) return false;
			if (entity instanceof Player) {
				if (!participant.getGame().isParticipating(entity.getUniqueId())) return false;
				AbstractGame.Participant target = participant.getGame().getParticipant(entity.getUniqueId());
				if (participant.getGame() instanceof DeathManager.Handler) {
					DeathManager.Handler game = (DeathManager.Handler)participant.getGame();
					if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
				}
				if (participant.getGame() instanceof TeamGame) {
					TeamGame game = (TeamGame) participant.getGame();
					return (!game.hasTeam(participant) || game.hasTeam(target) || game.getTeam(participant).equals(game.getTeam(target)));
				}
				return target.attributes().TARGETABLE.getValue();
			}
			return true;
		};
	}
}
