package cokes86.addon.util;

import java.util.function.Predicate;

public class PredicateUnit {

    public static <T> Predicate<T> positive(Class<T> t) {
        return t1 -> t1 instanceof Number && ((Number) t1).doubleValue() > 0;
    }

    public static <T> Predicate<T> negative(Class<T> t) {
        return t1 -> t1 instanceof Number && ((Number) t1).doubleValue() < 0;
    }
}
