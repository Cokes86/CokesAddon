package cokes86.addon.configuration;

import cokes86.addon.configuration.Config.Nodes;
import daybreak.abilitywar.config.CachedConfig;
import daybreak.abilitywar.config.interfaces.Cacher;
import daybreak.abilitywar.config.interfaces.Node;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;

public class Config extends CachedConfig<Nodes> {

	public static final Config INSTANCE;

	static {
		try {
			INSTANCE = new Config();
		} catch (IOException | InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private Config() throws IOException, InvalidConfigurationException {
		super(Nodes.class, "CokesAddon/config.yml");
	}

	public static boolean isRemakeAbilityAllowed() {
		return INSTANCE.get(Nodes.ALLOW_REMAKE_ABILITY_IN_GAME);
	}

	public enum Nodes implements Node {
		ALLOW_REMAKE_ABILITY_IN_GAME("리메이크능력_배정", false, "#리메이크 예정 능력들이 일반 능력자 전쟁에 배정할지 선택");

		private final String path;
		private final Object defaultValue;
		private final String[] comments;

		Nodes(String path, Object defaultValue, String... comments) {
			this.path = path;
			this.defaultValue = defaultValue;
			this.comments = comments;
		}

		public String getPath() {
			return path;
		}

		public Object getDefault() {
			return defaultValue;
		}

		@Override
		public boolean hasCacher() {
			return false;
		}

		@Override
		public Cacher getCacher() {
			return null;
		}

		public String[] getComments() {
			return comments;
		}
	}
}
