package com.cokes86.cokesaddon.game.module.roulette;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RouletteManifest {
    public String name();
    public int defaultPriority() default 6;
}
