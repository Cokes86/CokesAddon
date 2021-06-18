package cokes86.addon.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class AttributeUtil {

    public static AttributeInstance getInstance(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if(instance != null) return instance;
        else throw new NullPointerException();
    }

    public static void setMaxHealth(LivingEntity entity, double health) {
        if (health < 0 || health > 1024) throw new IllegalArgumentException("Max Health must be between 0 ~ 1024: (value = " + health + ")");
        AttributeInstance instance = getInstance(entity, Attribute.GENERIC_MAX_HEALTH);
        instance.setBaseValue(health);
    }

    public static double getMaxHealth(LivingEntity entity) {
        AttributeInstance instance = getInstance(entity, Attribute.GENERIC_MAX_HEALTH);
        return instance.getValue();
    }

    public static double getKnockbackResistance(LivingEntity entity) {
        AttributeInstance instance = getInstance(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        return instance.getBaseValue();
    }

    public static void setKnockbackResistance(LivingEntity entity, double resistance) {
        AttributeInstance instance = getInstance(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        instance.setBaseValue(resistance);
    }
}
