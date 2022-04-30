package com.cokes86.cokesaddon.util;

import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class FunctionalInterfaceUnit {
    private static final Number n = 0.0;

    public static @NotNull <T extends Number> Predicate<T> positive(){
        return upper((T)n);
    }

    public static @NotNull <T extends Number> Predicate<T> upper(T than) {
        return t -> t.doubleValue() > than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> greaterThanOrEqual(T than) {
        return upper(than).or(equals(n));
    }

    public static @NotNull <T extends Number> Predicate<T> lower(T than) {
        return greaterThanOrEqual(than).negate();
    }

    public static @NotNull <T extends Number> Predicate<T> equals(T equal) {
        return t -> t.doubleValue() == equal.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> between(T a, T b, boolean equals) {
        Predicate<T> one = upper(a).and(lower(b));
        Predicate<T> two = upper(b).and(lower(a));
        Predicate<T> three = equals((T)n).and(t -> equals);
        return one.or(two).or(three);
    }

    public static <T> Predicate<T> always() {
        return a -> true;
    }

    public static <T> Function<T, String> addJosa(Josa josa) {
        return a -> KoreanUtil.addJosa(a.toString(), josa);
    }

    public static <T> Function<T, String> Formatter(String prefix) {
        return a -> prefix + a;
    }
}
