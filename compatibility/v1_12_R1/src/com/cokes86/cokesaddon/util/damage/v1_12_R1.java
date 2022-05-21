package com.cokes86.cokesaddon.util.damage;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class v1_12_R1 implements DamageImpl{
    private static final ItemStack SPLASH_POTION = new ItemStack(Items.SPLASH_POTION);

    @Override
    public boolean damageMagicFixed(@NotNull org.bukkit.entity.Entity entity, @Nullable Player damager, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.damageEntity(magic(nmsEntity, nmsDamager), damage);
    }

    @Override
    public boolean damageWither(@NotNull org.bukkit.entity.Entity entity, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        return nmsEntity.damageEntity(DamageSource.WITHER, damage);
    }

    @Override
    public boolean damageVoid(org.bukkit.entity.@NotNull Entity entity, @Nullable Player damager, float damage) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;
        return nmsEntity.damageEntity(new EntityDamageSource("void", nmsDamager), damage);
    }

    private EntityDamageSourceIndirect magic(net.minecraft.server.v1_12_R1.Entity nmsEntity, EntityPlayer nmsDamager) {
        EntityDamageSourceIndirect source;
        if (nmsDamager != null) {
            source = new EntityDamageSourceIndirect("magic", new EntityPotion(nmsEntity.getWorld(), nmsDamager, SPLASH_POTION), nmsDamager) {{
                setMagic(); setIgnoreArmor(); m();
            }};
        } else {
            source = new EntityDamageSourceIndirect("magic", new EntityPotion(nmsEntity.getWorld(), nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ, SPLASH_POTION), null){{
                setMagic(); setIgnoreArmor(); m();
            }};
        }
        return source;
    }
}
