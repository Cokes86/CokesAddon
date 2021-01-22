package cokes86.addon.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;

import java.io.File;
import java.util.function.Predicate;

public class CokesAbility extends AbilityBase {
	public final static AbilitySettings config = new AbilitySettings(new File("plugins/AbilityWar/CokesAddon/AbilityConfig.yml"));

	public CokesAbility(Participant arg0) {
		super(arg0);
	}

	public static class Config<T> extends SettingObject<T> {
		private final Condition condition;
		private final Predicate<T> predicate;

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, String[] arg3, Condition arg4, Predicate<T> predicate) {
			config.super(arg0, arg1, arg2, arg3);
			this.condition = arg4;
			this.predicate = predicate;
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, String... arg3) {
			this(arg0, arg1, arg2, arg3, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, Condition arg3) {
			this(arg0, arg1, arg2, new String[]{}, arg3, a -> true);
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2) {
			this(arg0, arg1, arg2, new String[]{}, Condition.NORMAL, a -> true);
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, Predicate<T> predicate) {
			this(arg0, arg1, arg2, new String[]{}, Condition.NORMAL, predicate);
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
