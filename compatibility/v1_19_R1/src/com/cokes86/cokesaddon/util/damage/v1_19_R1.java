package com.cokes86.cokesaddon.util.damage;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class v1_19_R1 implements DamageImpl {
    @Override
    public boolean damageMagicFixed(@NotNull org.bukkit.entity.Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.hurt(DamageSource.indirectMagic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull org.bukkit.entity.Entity entity, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.hurt(DamageSource.WITHER, damage);
    }

    @Override
    public boolean damageVoid(org.bukkit.entity.@NotNull Entity entity, float damage) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.hurt(DamageSource.OUT_OF_WORLD, damage);
    }
}
