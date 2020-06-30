package cokes86.addon.utils;

import java.util.function.Predicate;

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
		return new Predicate<Entity> () {

			@Override
			public boolean test(Entity entity) {
				if (GameManager.isGameRunning() && entity instanceof Player) {
					AbstractGame game = GameManager.getGame();
					Player player = (Player)entity;
					return (game.isParticipating(player) && game.getParticipant(player).hasAbility());
				}
				return false;
			}
			
		};
	}
}
