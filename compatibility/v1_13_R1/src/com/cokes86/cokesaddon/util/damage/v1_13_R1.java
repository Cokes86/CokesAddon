package com.cokes86.cokesaddon.util.damage;

import net.minecraft.server.v1_13_R1.*;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class v1_13_R1 implements DamageImpl{
    private static final ItemStack SPLASH_POTION = new ItemStack(Items.SPLASH_POTION);

    @Override
    public boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage) {
        net.minecraft.server.v1_13_R1.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        EntityPlayer nmsDamager = damager != null ? ((CraftPlayer)damager).getHandle() : null;

        return nmsEntity.damageEntity(magic(nmsEntity, nmsDamager), damage);
    }

    private EntityDamageSourceIndirect magic(net.minecraft.server.v1_13_R1.Entity nmsEntity, EntityPlayer nmsDamager) {
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
