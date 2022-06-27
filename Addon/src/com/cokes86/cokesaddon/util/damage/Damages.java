package com.cokes86.cokesaddon.util.damage;

import daybreak.abilitywar.utils.base.minecraft.damage.Damages.INSTANCE;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Damages {
    private static final DamageImpl instance;

    static {
        try {
            instance = Class.forName("com.cokes86.cokesaddon.util.damage." + ServerVersion.getName())
                    .asSubclass(DamageImpl.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage) {
        return instance.damageMagicFixed(entity, damager, damage);
    }

    public static boolean damageWither(@NotNull Entity entity, float damage) {
        return instance.damageWither(entity, damage);
    }

    public static boolean damageVoid(@NotNull Player entity, @Nullable Player damager, float damage) {
        return instance.damageVoid(entity, damager, damage);
    }

    public static boolean damageMagic(@NotNull Entity entity, @Nullable Player damager, boolean ignore, float damage) {
        return originDamages().damageMagic(entity, damager, ignore, damage);
    }

    public static boolean damageFixed(@NotNull Player entity, @NotNull LivingEntity damager, float damage) {
        return originDamages().damageFixed(entity,damager,damage);
    }

    public static boolean canDamage(@NotNull Entity victim, @Nullable Entity entity, DamageCause cause, float damage) {
        if (entity != null) return originDamages().canDamage(victim, entity, cause, damage);
        else return originDamages().canDamage(victim, cause, damage);
    }

    public static INSTANCE originDamages() {
        return daybreak.abilitywar.utils.base.minecraft.damage.Damages.INSTANCE;
    }
}
