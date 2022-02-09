package cokes86.addon.ability;

import cokes86.addon.CokesAddon;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;

import java.util.function.Predicate;

public class CokesAbility extends AbilityBase {
	public final static AbilitySettings config = new AbilitySettings(CokesAddon.getAddonFile("CokesAbility.yml"));

	public CokesAbility(Participant arg0) {
		super(arg0);
	}

	public static class Config<T> extends SettingObject<T> {
		private final Condition condition;
		private final Predicate<T> predicate;

		public Config(Class<? extends CokesAbility> aClass, String name, T value, String[] description, Condition condition, Predicate<T> predicate) {
			config.super(aClass, name, value, description);
			this.condition = condition;
			this.predicate = predicate;
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value, String... description) {
			this(aClass, name, value, description, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value, Condition arg3) {
			this(aClass, name, value, new String[]{}, arg3, a -> true);
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value) {
			this(aClass, name, value, new String[]{}, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value, Predicate<T> predicate) {
			this(aClass, name, value, new String[]{}, Condition.NORMAL, predicate);
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value, Predicate<T> predicate, String... description) {
			this(aClass, name, value, description, Condition.NORMAL, predicate);
		}

		public Config(Class<? extends CokesAbility> aClass, String name, T value, Condition arg3, String... description) {
			this(aClass, name, value, description, arg3, a -> true);
		}

		public String toString() {
			if (condition == Condition.COOLDOWN && getValue() instanceof Integer) {
				return Formatter.formatCooldown((Integer) getValue());
			} else if (condition == Condition.TIME && getValue() instanceof Integer) {
				return TimeUtil.parseTimeAsString((Integer) getValue());
			} else if (condition == Condition.NUMBER && getValue() instanceof Double) {
				return getValue() + KoreanUtil.getJosa(getValue().toString(), KoreanUtil.Josa.을를);
			}else {
				return getValue().toString();
			}
		}

		public boolean condition(T object) {
			if (condition == Condition.COOLDOWN || condition == Condition.TIME) {
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
