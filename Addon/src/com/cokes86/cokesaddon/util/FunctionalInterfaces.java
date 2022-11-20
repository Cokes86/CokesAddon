package com.cokes86.cokesaddon.util;

import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class FunctionalInterfaces {
    private static final Number ZERO = 0.0;
    public static final Function<Integer, String> COOLDOWN = Formatter::formatCooldown;
    public static final Function<Integer, String> TIME = TimeUtil::parseTimeAsString;
    public static final Function<Boolean, String> ON_OFF = a -> a ? "§b켜짐§f" : "§c꺼짐§f";

    public static @NotNull <T extends Number> Predicate<T> positive() {
        return upper((T) ZERO);
    }

    public static @NotNull <T extends Number> Predicate<T> upper(T than) {
        return t -> t.doubleValue() > than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> greaterThanOrEqual(T than) {
        return upper(than).or(equals(ZERO));
    }

    public static @NotNull <T extends Number> Predicate<T> lessThanOrEqual(T than) {
        return upper(than).negate();
    }

    public static @NotNull <T extends Number> Predicate<T> lower(T than) {
        return greaterThanOrEqual(than).negate();
    }

    public static @NotNull <T extends Number> Predicate<T> equals(T equal) {
        return t -> t.doubleValue() == equal.doubleValue();
    }

    public static @NotNull <T> Predicate<T> always() {
        return t -> true;
    }

    public static @NotNull <K> Function<K, String> toStrings() {
        return Object::toString;
    }

    public static <T> Function<T, String> addJosa(Josa josa) {
        return a -> KoreanUtil.addJosa(a.toString(), josa);
    }

    public static <T> Function<T, String> prefix(String prefix) {
        return a -> prefix + a;
    }
}
