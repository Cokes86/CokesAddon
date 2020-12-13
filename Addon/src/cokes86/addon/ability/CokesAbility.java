package cokes86.addon.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;

import java.io.File;

public class CokesAbility extends AbilityBase {
	public final static AbilitySettings config = new AbilitySettings(new File("plugins/AbilityWar/CokesAddon/AbilityConfig.yml"));

	public CokesAbility(Participant arg0) {
		super(arg0);
	}

	public static abstract class Config<T> extends SettingObject<T> {
		Condition condition;

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, String[] arg3, Condition arg4) {
			config.super(arg0, arg1, arg2, arg3);
			if (getValue().getClass().equals(Integer.class)) {
				condition = arg4;
			} else {
				condition = Condition.NORMAL;
			}
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, String... arg3) {
			this(arg0, arg1, arg2, arg3, Condition.NORMAL);
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, Condition arg3) {
			this(arg0, arg1, arg2, new String[]{}, arg3);
		}

		public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2) {
			this(arg0, arg1, arg2, new String[]{}, Condition.NORMAL);
		}

		public String toString() {
			if (condition == Condition.COOLDOWN) {
				return Formatter.formatCooldown((Integer) getValue());
			} else if (condition == Condition.TIME) {
				return TimeUtil.parseTimeAsString((Integer) getValue());
			} else if (condition == Condition.NUMBER) {
				return getValue() + KoreanUtil.getJosa(getValue().toString(), KoreanUtil.Josa.을를);
			}else {
				return getValue().toString();
			}
		}

		public enum Condition {
			COOLDOWN,
			TIME,
			NUMBER,
			NORMAL
		}
	}
}
