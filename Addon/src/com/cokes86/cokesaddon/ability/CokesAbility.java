package com.cokes86.cokesaddon.ability;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;

import java.util.function.Function;
import java.util.function.Predicate;

@NotAvailable(AbstractTripleMix.class)
public class CokesAbility extends AbilityBase {
	public final static AbilitySettings config = new AbilitySettings(CokesAddon.getAddonFile("CokesAbility.yml"));

	public CokesAbility(Participant arg0) {
		super(arg0);
	}

	public static class Config<T> extends SettingObject<T> {
		private final Predicate<T> predicate;
		private final Function<T, String> function;

		public Config(Class<? extends CokesAbility> aClass, String name, T value, String[] description, Predicate<T> predicate, Function<T, String> function) {
			config.super(aClass, name, value, description);
			this.predicate = predicate;
			this.function = function;
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, String[] description, Predicate<T> predicate, Function<T, String> function) {
			return new Config<>(aClass, name, value, description, predicate, function);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, String[] description, Predicate<T> predicate) {
			return of(aClass, name, value, description, predicate, Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, String[] description, Function<T, String> function) {
			return of(aClass, name, value, description, FunctionalInterfaces.always(), function);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, String... description) {
			return of(aClass, name, value, description, FunctionalInterfaces.always(), Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, Predicate<T> predicate, String... description) {
			return of(aClass, name, value, description, predicate, Object::toString);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, Function<T, String> function, String... description) {
			return of(aClass, name, value, description, FunctionalInterfaces.always(), function);
		}

		public static <T> Config<T> of(Class<? extends CokesAbility> aClass, String name, T value, Predicate<T> predicate, Function<T, String> function, String... description) {
			return of(aClass, name, value, description, predicate, function);
		}

        public String toString() {
			return function.apply(getValue());
		}

		public boolean condition(T object) {
			return predicate.test(object);
		}
	}
}
