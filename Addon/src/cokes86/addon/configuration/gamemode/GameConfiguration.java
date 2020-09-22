package cokes86.addon.configuration.gamemode;

import cokes86.addon.configuration.ConfigFiles;
import daybreak.abilitywar.config.Cache;
import daybreak.abilitywar.config.CommentedConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameConfiguration {

	private static final Logger logger = Logger.getLogger(GameConfiguration.class.getName());
	private static final EnumMap<GameNodes, Cache> cache = new EnumMap<>(GameNodes.class);
	private static File file = null;
	private static long lastModified;
	private static CommentedConfiguration config = null;

	public static boolean isLoaded() {
		return file != null && config != null;
	}

	public static void load() throws IOException, InvalidConfigurationException {
		if (!isLoaded()) {
			file = ConfigFiles.createFile("GameConfig.yml");
			lastModified = file.lastModified();
			config = new CommentedConfiguration(file);
			update();
		}
	}

	public static void update() throws IOException, InvalidConfigurationException {
		config.load();

		for (Entry<GameNodes, Cache> entry : cache.entrySet()) {
			Cache cache = entry.getValue();
			if (cache.isModifiedValue()) {
				config.set(entry.getKey().getPath(), cache.getValue());
			}
		}

		cache.clear();
		for (GameNodes node : GameNodes.values()) {
			Object value = config.get(node.getPath());
			if (value != null) {
				cache.put(node, new Cache(false, value));
			} else {
				config.set(node.getPath(), node.getDefaultValue());
				cache.put(node, new Cache(false, node.getDefaultValue()));
			}
		}
		config.save();
		lastModified = file.lastModified();
	}

	// Private only method
	private static <T> T get(GameNodes configNode, Class<T> clazz) throws IllegalStateException {
		if (!isLoaded()) {
			logger.log(Level.SEVERE, "콘피그가 불러와지지 않은 상태에서 접근을 시도하였습니다.");
			throw new IllegalStateException("콘피그가 아직 불러와지지 않았습니다.");
		}
		if (lastModified != file.lastModified()) {
			try {
				update();
			} catch (IOException | InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "콘피그를 다시 불러오는 도중 오류가 발생하였습니다.");
			}
		}
		return (T) cache.get(configNode).getValue();
	}

	// Private only method
	private static <T> List<T> getList(GameNodes configNode, Class<T> clazz) throws IllegalStateException {
		List<?> list = (List<?>) get(configNode, List.class);
		List<T> newList = new ArrayList<>();
		for (Object object : list) {
			if (object != null && clazz.isAssignableFrom(object.getClass())) {
				newList.add((T) object);
			}
		}
		return newList;
	}

	public static void modifyProperty(GameNodes node, Object value) {
		cache.put(node, new Cache(true, value));
	}

	public static class Config {

		public static String getString(GameNodes node) {
			return get(node, String.class);
		}

		public static int getInt(GameNodes node) {
			return get(node, Integer.class);
		}

		public static boolean getBoolean(GameNodes node) {
			return get(node, Boolean.class);
		}

		public static List<String> getStringList(GameNodes node) {
			return getList(node, String.class);
		}
	}
}
