package cokes86.addon.gamemodes.battleability;

import cokes86.addon.configuration.gamemode.GameConfiguration.Config;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.manager.object.Invincibility;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class Border extends Invincibility {
	
	private final int size = Config.getInt(GameNodes.BattleAbility_startSize);
	private final int time = Config.getInt(GameNodes.BattleAbility_time);
	private final World world = Settings.getSpawnLocation().getWorld();
	private final WorldBorder wb = world.getWorldBorder();
	
	public Border(Game game) {
		super(game);
	}

	@Override
	public boolean start(boolean isInfinite) {
		wb.setCenter(Settings.getSpawnLocation());
		wb.setSize(size);
		return super.start(isInfinite);
	}

	@Override
	public boolean start(final int duration) {
		wb.setCenter(Settings.getSpawnLocation());
		wb.setSize(size);
		return super.start(duration);
	}

	@Override
	public boolean stop() {
		wb.setSize(1, time);
		return super.stop();
	}

	public interface Handler {
		Border getBorder();
	}
}
