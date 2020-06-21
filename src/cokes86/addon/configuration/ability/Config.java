package cokes86.addon.configuration.ability;

import cokes86.addon.CokesAddon;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;

public abstract class Config<T> extends SettingObject<T> {
	int option;

	public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, String[] arg3, int arg4) {
		CokesAddon.config.super(arg0, arg1, arg2, arg3);
		if (getValue().getClass().equals(Integer.class) && arg4 >=0 && arg4 <= 2) {
			option = arg4;
		} else {
			option = 0;
		}
	}
	
	public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2, int arg3) {
		this(arg0, arg1, arg2, new String[] {}, arg3);
	}
	
	public Config(Class<? extends AbilityBase> arg0, String arg1, T arg2) {
		this(arg0, arg1, arg2, new String[] {}, 0);
	}
	
	public Config(AbilityBase arg0, String arg1, T arg2, int option) {
		this(arg0.getClass(), arg1, arg2, new String[] {}, option);
	}
	
	public Config(AbilityBase arg0, String arg1, T arg2) {
		this(arg0.getClass(), arg1, arg2, new String[] {}, 0);
	}

	public String toString() {
		if (option == 1 && getValue().getClass().equals(Integer.class)) {
			return Formatter.formatCooldown((Integer) getValue());
		} else if (option == 2 && getValue().getClass().equals(Integer.class)) {
			return TimeUtil.parseTimeAsString((Integer) getValue());
		} else {
			return getValue().toString();
		}
	}
	
	public abstract boolean Condition(T value);
	
	public boolean condition(T value) {
		return Condition(value);
	}
}