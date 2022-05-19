package com.cokes86.cokesaddon.util.damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DamageImpl {

    boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage);
    boolean damageWither(@NotNull Entity entity, float damage);
}
