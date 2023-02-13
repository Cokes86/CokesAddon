package com.cokes86.cokesaddon.game.module.roulette;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.TreeBasedTable;

import daybreak.abilitywar.config.Cache;
import daybreak.abilitywar.config.interfaces.Configurable;
import daybreak.abilitywar.utils.base.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

public class RouletteConfig {

	private static final Logger logger = Logger.getLogger(RouletteConfig.class.getName());
	private static final Map<String, RouletteConfig> roulletConfigMap = new HashMap<>();
	private final Table<String, String, SettingObject<?>> settings = TreeBasedTable.create();
	private final File configFile;
	private final YamlConfiguration config = new YamlConfiguration();
	private final Map<SettingObject<?>, Cache> cache = new HashMap<>();
	private long lastModified;
	private boolean error = false;
	public RouletteConfig(File configFile) {
		this.configFile = configFile;
		if (!configFile.exists()) {
			if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
				configFile.getParentFile().mkdirs();
			}
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				this.error = true;
			}
		}
		this.lastModified = configFile.lastModified();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			this.error = true;
		}
		roulletConfigMap.put(configFile.getName(), this);
	}

	public static Collection<RouletteConfig> getRoulletConfigs() {
		return roulletConfigMap.values();
	}

	public static RouletteConfig getRoulletConfig(String fileName) {
		return roulletConfigMap.get(fileName);
	}

	public File getConfigFile() {
		return configFile;
	}

	private void registerSetting(String name, SettingObject<?> object) {
		settings.put(name, object.key, object);
	}

	public Table<String, String, SettingObject<?>> getSettings() {
		return Tables.unmodifiableTable(settings);
	}

	public Map<String, SettingObject<?>> getSettings(String name) {
		return Collections.unmodifiableMap(settings.row(name));
	}

	public SettingObject<?> getSetting(String name, String key) {
		return settings.get(name, key);
	}

	public boolean isError() {
		return error;
	}

	public void update() {
		try {
			_update();
		} catch (IOException | InvalidConfigurationException e) {
			logger.log(Level.SEVERE, "콘피그를 업데이트하는 도중 오류가 발생하였습니다.");
		}
	}

	private void _update() throws IOException, InvalidConfigurationException {
		config.load(configFile);

		for (Entry<SettingObject<?>, Cache> entry : cache.entrySet()) {
			final Cache cache = entry.getValue();
			if (cache.isModifiedValue()) {
				config.set(entry.getKey().getPath(), cache.getValue());
			}
		}

		cache.clear();
		lastModified = configFile.lastModified();
		for (SettingObject<?> setting : settings.values()) {
			final Object value = config.get(setting.getPath());
			if (value != null) {
				cache.put(setting, new Cache(false, value));
			} else {
				config.set(setting.getPath(), setting.getDefaultValue());
				cache.put(setting, new Cache(false, setting.getDefaultValue()));
			}
		}
		config.save(configFile);
	}

	public class SettingObject<T> implements Configurable<T> {

		private final String key, path, displayName;
		private final T defaultValue;
		private final String[] comments;
		private final Class<? extends RouletteEffect> clazz;

		public SettingObject(Class<? extends RouletteEffect> clazz, String displayName, String key, T defaultValue, String... comments) {
			this.path = checkNotNull(key);
			this.key = key;
			this.defaultValue = checkNotNull(defaultValue);
			this.comments = comments;
			this.clazz = clazz;
			this.displayName = displayName;
			registerSetting("roulette", this);
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public T getDefaultValue() {
			return defaultValue;
		}

		@Override
		public String[] getComments() {
			return comments;
		}

		@Override
		public boolean condition(T value) {
			return true;
		}

		public String getDisplayName() {
			return displayName;
		}

		public Class<? extends RouletteEffect> getRouletteEffectClass() {
			return clazz;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getValue() {
			if (isError()) {
				logger.log(Level.SEVERE, "콘피그가 불러와지는 도중 오류가 발생했습니다.");
				throw new IllegalStateException("콘피그가 불러와지는 도중 오류가 발생했습니다.");
			}
			if (lastModified != configFile.lastModified()) {
				try {
					_update();
				} catch (IOException | InvalidConfigurationException e) {
					logger.log(Level.SEVERE, "콘피그를 다시 불러오는 도중 오류가 발생하였습니다.");
				}
			}

			if (!cache.containsKey(this)) {
				Object value = config.get(path);
				if (value != null) {
					cache.put(this, new Cache(false, value));
				} else {
					config.set(path, defaultValue);
					cache.put(this, new Cache(false, defaultValue));
				}
			}
			Object value = cache.get(this).getValue();

			if (value != null && value.getClass().isAssignableFrom(defaultValue.getClass())) {
				T castedValue = (T) value;
				if (condition(castedValue)) {
					return castedValue;
				} else {
					return defaultValue;
				}
			} else {
				return defaultValue;
			}
		}

		@Override
		public boolean setValue(T value) {
			if (condition(value)) {
				cache.put(this, new Cache(true, value));
				return true;
			}
			return false;
		}

		@Override
		public void reset() {
			setValue(defaultValue);
		}

		@Override
		public String toString() {
			return String.valueOf(getValue());
		}

	}
}
