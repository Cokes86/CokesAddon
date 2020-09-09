package cokes86.addon.configuration.addon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;

import cokes86.addon.configuration.ConfigFile;
import daybreak.abilitywar.config.CommentedConfiguration;

public class Config {

	private static final Logger logger = Logger.getLogger(Config.class.getName());
	private static File file = null;
	private static long lastModified;
	private static CommentedConfiguration config = null;

	public static boolean isLoaded() {
		return file != null && config != null;
	}

	public static void load() throws IOException, InvalidConfigurationException {
		if (!isLoaded()) {
			file = ConfigFile.createFile("Config.yml");
			lastModified = file.lastModified();
			config = new CommentedConfiguration(file);
			update();
		}
	}

	public static void update() throws IOException, InvalidConfigurationException {
		config.load();

		for (Entry<Nodes, CacheG> entry : cache.entrySet()) {
			CacheG cache = entry.getValue();
			if (cache.isModifiedValue()) {
				config.set(entry.getKey().getPath(), cache.getValue());
			}
		}

		cache.clear();
		for (Nodes node : Nodes.values()) {
			Object value = config.get(node.getPath());
			if (value != null) {
				cache.put(node, new CacheG(false, value));
			} else {
				config.set(node.getPath(), node.getDefault());
				cache.put(node, new CacheG(false, node.getDefault()));
			}
		}
		config.save();
		lastModified = file.lastModified();
	}

	private static final EnumMap<Nodes, CacheG> cache = new EnumMap<>(Nodes.class);

	// Private only method
	private static <T> T get(Nodes configNode, Class<T> clazz) throws IllegalStateException {
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
	private static <T> List<T> getList(Nodes configNode, Class<T> clazz) throws IllegalStateException {
		List<?> list = (List<?>) get(configNode, List.class);
		List<T> newList = new ArrayList<>();
		for (Object object : list) {
			if (object != null && clazz.isAssignableFrom(object.getClass())) {
				newList.add((T) object);
			}
		}
		return newList;
	}

	public static void modifyProperty(Nodes node, Object value) {
		cache.put(node, new CacheG(true, value));
	}
	
	public static String getString(Nodes node) {
		return get(node, String.class);
	}

	public static int getInt(Nodes node) {
		return get(node, Integer.class);
	}

	public static boolean getBoolean(Nodes node) {
		return get(node, Boolean.class);
	}

	public static List<String> getStringList(Nodes node) {
		return getList(node, String.class);
	}
	
	public static enum Nodes {
		ALLOW_REMAKE_ABILITY_IN_GAME("리메이크능력_배정", false, "#리메이크 예정 능력들이 일반 능력자 전쟁에 배정할지 선택");
		
		private final String Path;
		private final Object Default;
		private final String[] Comments;
		
		Nodes(String name, Object Default, String... Comments) {
			this.Path = name;
			this.Default = Default;
			this.Comments = Comments;
		}
		
		public String getPath() {
			return Path;
		}
		
		public Object getDefault() {
			return Default;
		}

		public String[] getComments() {
			return Comments;
		}
	}
}

class CacheG {

	private final boolean isModifiedValue;
	private final Object value;

	CacheG(boolean isModifiedValue, Object value) {
		this.isModifiedValue = isModifiedValue;
		this.value = value;
	}

	boolean isModifiedValue() {
		return isModifiedValue;
	}

	Object getValue() {
		return value;
	}

}