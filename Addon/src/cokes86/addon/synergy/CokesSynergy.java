package cokes86.addon.synergy;

import cokes86.addon.CokesAddon;
import cokes86.addon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class CokesSynergy extends Synergy {
	public final static AbilitySettings config = new AbilitySettings(CokesAddon.getAddonFile("CokesSynergy.yml"));

	public CokesSynergy(Participant participant) {
		super(participant);
	}

	public static class Config<T> extends SettingObject<T> {
		private final Condition condition;
		private final Predicate<T> predicate;
		private final Function<T, String> function;

		public Config(Class<? extends CokesSynergy> aClass, String name, T value, String[] description, Condition condition, Predicate<T> predicate, Function<T, String> function) {
			config.super(aClass, name, value, description);
			this.condition = condition;
			this.predicate = predicate;
			this.function = function;
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, String[] description, Condition condition, Predicate<T> predicate, Function<T, String> function) {
			return new Config<>(aClass, name, value, description, condition, predicate, function);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, String[] description, Condition condition, Predicate<T> predicate) {
			return of(aClass, name, value, description, condition, predicate, Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, String[] description, Condition condition, Function<T, String> function) {
			return of(aClass, name, value, description, condition, FunctionalInterfaceUnit.always(), function);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, Condition condition, String... description) {
			return of(aClass, name, value, description, condition, FunctionalInterfaceUnit.always(), Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, Predicate<T> predicate, String... description) {
			return of(aClass, name, value, description, Condition.NORMAL, predicate, Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, Predicate<T> predicate, Function<T, String> function, String... description) {
			return of(aClass, name, value, description, Condition.NORMAL, predicate, function);
		}

		public static <T> Config<T> of(Class<? extends CokesSynergy> aClass, String name, T value, String... description) {
			return of(aClass, name, value, description, Condition.NORMAL, FunctionalInterfaceUnit.always(), Objects::toString);
		}

		public String toString() {
			if (condition == Condition.COOLDOWN && getValue() instanceof Integer) {
				return Formatter.formatCooldown((Integer) getValue());
			} else if (condition == Condition.TIME && getValue() instanceof Integer) {
				return TimeUtil.parseTimeAsString((Integer) getValue());
			} else {
				return function.apply(getValue());
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
