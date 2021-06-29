package cokes86.addon.synergy;

import cokes86.addon.CokesAddon;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;

import java.util.function.Predicate;

public class CokesSynergy extends Synergy {
	public final static AbilitySettings config = new AbilitySettings(CokesAddon.getAddonFile("CokesSynergy.yml"));

	public CokesSynergy(Participant participant) {
		super(participant);
	}

	public static class Config<T> extends SettingObject<T> {
		private final Config.Condition condition;
		private final Predicate<T> predicate;

		public Config(Class<? extends CokesSynergy> aClass, String name, T value, String[] description, Condition condition, Predicate<T> predicate) {
			config.super(aClass, name, value, description);
			this.condition = condition;
			this.predicate = predicate;
		}

		public Config(Class<? extends CokesSynergy> aClass, String name, T value, String... description) {
			this(aClass, name, value, description, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends CokesSynergy> aClass, String name, T value, Condition arg3) {
			this(aClass, name, value, new String[]{}, arg3, a -> true);
		}

		public Config(Class<? extends CokesSynergy> aClass, String name, T value) {
			this(aClass, name, value, new String[]{}, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends CokesSynergy> aClass, String name, T value, Predicate<T> predicate) {
			this(aClass, name, value, new String[]{}, Condition.NORMAL, predicate);
		}

		public String toString() {
			if (condition == Config.Condition.COOLDOWN && getValue() instanceof Integer) {
				return Formatter.formatCooldown((Integer) getValue());
			} else if (condition == Config.Condition.TIME && getValue() instanceof Integer) {
				return TimeUtil.parseTimeAsString((Integer) getValue());
			} else if (condition == Config.Condition.NUMBER && getValue() instanceof Double) {
				return getValue() + KoreanUtil.getJosa(getValue().toString(), KoreanUtil.Josa.을를);
			}else {
				return getValue().toString();
			}
		}

		public boolean condition(T object) {
			if (condition == Config.Condition.COOLDOWN || condition == Config.Condition.TIME) {
				return (Integer) object > 0 && predicate.test(object);
			}
			return predicate.test(object);
		}

		public enum Condition {
			COOLDOWN,
			TIME,
			NUMBER,
			NORMAL
		}
	}

}
