package cokes86.addon.gamemode.huntingrunner.effects;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EffectManifest {
    String name();
    String[] explain() default {};
}
