package cokes86.addon.configuration.synergy;

import cokes86.addon.CokesAddon;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;

public abstract class Config<T> extends SettingObject<T> {
	int option;

	public Config(Class<? extends Synergy> arg0, String arg1, T arg2, String[] arg3, int arg4) {
		CokesAddon.sconfig.super(arg0, arg1, arg2, arg3);
		if (getValue().getClass().equals(Integer.class) && arg4 >=0 && arg4 <= 2) {
			option = arg4;
		} else {
			option = 0;
		}
	}
	
	public Config(Class<? extends Synergy> arg0, String arg1, T arg2, int arg3) {
		this(arg0, arg1, arg2, new String[] {}, arg3);
	}
	
	public Config(Class<? extends Synergy> arg0, String arg1, T arg2) {
		this(arg0, arg1, arg2, new String[] {}, 0);
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
}