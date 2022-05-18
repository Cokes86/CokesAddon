package com.cokes86.cokesaddon.util.damage;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class v1_16_R2 implements DamageImpl{
    private static final ItemStack SPLASH_POTION = new ItemStack(Items.SPLASH_POTION);

    @Override
    public boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.server.v1_16_R2.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.damageEntity(magic(nmsEntity, nmsDamager), damage);
    }

    private EntityDamageSourceIndirect magic(net.minecraft.server.v1_16_R2.Entity nmsEntity, EntityPlayer nmsDamager) {
        EntityDamageSourceIndirect source;
        if (nmsDamager != null) {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.getWorld(), nmsDamager)), nmsDamager) {{
                setMagic(); setIgnoreArmor(); setStarvation();
            }};
        } else {
            source = new EntityDamageSourceIndirect("magic", setItem(new EntityPotion(nmsEntity.getWorld(), nmsEntity.locX(), nmsEntity.locY(), nmsEntity.locZ())), null){{
                setMagic(); setIgnoreArmor(); setStarvation();
            }};
        }
        return source;
    }

    private EntityPotion setItem(final EntityPotion potion) {
        potion.setItem(SPLASH_POTION);
        return potion;
    }
}
