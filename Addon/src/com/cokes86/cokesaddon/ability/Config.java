package com.cokes86.cokesaddon.ability;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;

import java.util.function.Function;
import java.util.function.Predicate;

public class Config<T> extends SettingObject<T> {
    private final Predicate<T> predicate;
    private final Function<T, String> function;
    private static final AbilitySettings abilitySettings = new AbilitySettings(CokesAddon.getAddonFile("CokesAbility.yml"));
    private static final AbilitySettings synergySettings = new AbilitySettings(CokesAddon.getAddonFile("CokesSynergy.yml"));
    private static final AbilitySettings mysterySettings = new AbilitySettings(CokesAddon.getAddonFile("CokesMurderMystery.yml"));

    private static AbilitySettings getSettings(Class<? extends AbilityBase> aClass) {
        if (CokesAbility.class.isAssignableFrom(aClass)) {
            return abilitySettings;
        } else if (CokesSynergy.class.isAssignableFrom(aClass)) {
            return synergySettings;
        } else if (AbstractJob.class.isAssignableFrom(aClass)) {
            return mysterySettings;
        }
        return AbilityBase.abilitySettings;
    }

    public Config(Class<? extends AbilityBase> aClass, String name, T value, String[] description, Predicate<T> predicate, Function<T, String> function) {
        getSettings(aClass).super(aClass, name, value, description);
        this.predicate = predicate;
        this.function = function;
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, String[] description, Predicate<T> predicate, Function<T, String> function) {
        return new Config<>(aClass, name, value, description, predicate, function);
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, String[] description, Predicate<T> predicate) {
        return of(aClass, name, value, description, predicate, FunctionalInterfaces.toStrings());
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, String[] description, Function<T, String> function) {
        return of(aClass, name, value, description, FunctionalInterfaces.always(), function);
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, String... description) {
        return of(aClass, name, value, description, FunctionalInterfaces.always(), FunctionalInterfaces.toStrings());
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, Predicate<T> predicate, String... description) {
        return of(aClass, name, value, description, predicate, FunctionalInterfaces.toStrings());
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, Function<T, String> function, String... description) {
        return of(aClass, name, value, description, FunctionalInterfaces.always(), function);
    }

    public static <T> Config<T> of(Class<? extends AbilityBase> aClass, String name, T value, Predicate<T> predicate, Function<T, String> function, String... description) {
        return of(aClass, name, value, description, predicate, function);
    }

    public static Config<Integer> cooldown(Class<? extends AbilityBase> aClass, String name, Integer value, String... description) {
        return new Config<>(aClass, name, value, description, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
    }

    public static Config<Integer> time(Class<? extends AbilityBase> aClass, String name, Integer value, String... description) {
        return new Config<>(aClass, name, value, description, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
    }

    public static Config<Integer> tickToSecond(Class<? extends AbilityBase> aClass, String name, Integer value, String... description) {
        return new Config<>(aClass, name, value, description, FunctionalInterfaces.positive(), FunctionalInterfaces.tickToSecond());
    }

    public static <T extends Number> Config<T> positive(Class<? extends AbilityBase> aClass, String name, T value, String... description) {
        return new Config<>(aClass, name, value, description, FunctionalInterfaces.positive(), FunctionalInterfaces.toStrings());
    }

    public static void update() {
        abilitySettings.update();
        synergySettings.update();
    }

    public String toString() {
        return function.apply(getValue());
    }

    public boolean condition(T object) {
        return predicate.test(object);
    }
}
