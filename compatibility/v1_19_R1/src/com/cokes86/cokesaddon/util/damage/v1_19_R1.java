package com.cokes86.cokesaddon.util.damage;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSourceIndirect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class v1_19_R1 implements DamageImpl{
    private static final ItemStack SPLASH_POTION = new ItemStack(Items.sr);

    @Override
    public boolean damageMagicFixed(@NotNull org.bukkit.entity.Entity entity, @Nullable Player damager, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.a(magic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull org.bukkit.entity.Entity entity, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.a(DamageSource.p, damage);
    }

    @Override
    public boolean damageVoid(org.bukkit.entity.@NotNull Entity entity, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.a(DamageSource.m, damage);
    }

    private EntityDamageSourceIndirect magic(Entity nmsEntity, EntityPlayer nmsDamager) {
        EntityDamageSourceIndirect source;
        if (nmsDamager != null) {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.s, nmsDamager)), nmsDamager) {{
                y(); m(); o();
            }};
        } else {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.s, nmsEntity.dg(), nmsEntity.di(), nmsEntity.dm())), null){{
                y(); m(); o();
            }};
        }
        return source;
    }

    private EntityPotion setItem(final EntityPotion potion) {
        potion.a(SPLASH_POTION);
        return potion;
    }
}
