package cokes86.addon.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class PredicateUnit {
    public static @NotNull <T extends Number> Predicate<T> positive(){
        return t -> t.doubleValue() > 0.0;
    }

    public static @NotNull <T extends Number> Predicate<T> negative(){
        return t -> t.doubleValue() < 0.0;
    }

    public static @NotNull <T extends Number> Predicate<T> upper(T than) {
        return t -> t.doubleValue() > than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> greaterThanOrEqual(T than) {
        return t -> t.doubleValue() >= than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> lower(T than) {
        return t -> t.doubleValue() < than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> lessThanOrEqual(T than) {
        return t -> t.doubleValue() >= than.doubleValue();
    }

    public static @NotNull <T extends Number> Predicate<T> between(T a, T b, boolean equals) {
        double min, max;
        if (a.doubleValue() > b.doubleValue()) {
            min = a.doubleValue();
            max = b.doubleValue();
        } else {
            min = b.doubleValue();
            max = a.doubleValue();
        }
        return equals ? t -> t.doubleValue() >= min && t.doubleValue() <= max : t -> t.doubleValue() > min && t.doubleValue() < max;
    }
}
