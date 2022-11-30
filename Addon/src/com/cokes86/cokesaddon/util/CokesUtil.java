package com.cokes86.cokesaddon.util;

import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import daybreak.google.common.collect.ImmutableSet.Builder;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

public class CokesUtil {

    public static String xor_encoding(String value, int key) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            builder.append((char)(c^key));
        }
        return builder.toString();
    }

    public static ImmutableSet<Material> getSwords() {
        Builder<Material> builder = ImmutableSet.builder();
        builder.add(MaterialX.WOODEN_SWORD.getMaterial(), MaterialX.STONE_SWORD.getMaterial(),
                MaterialX.IRON_SWORD.getMaterial(), MaterialX.GOLDEN_SWORD.getMaterial(), MaterialX.DIAMOND_SWORD.getMaterial());
        if (MaterialX.NETHERITE_SWORD.isSupported()) {
            builder.add(MaterialX.NETHERITE_SWORD.getMaterial());
        }
        return builder.build();
    }

    public static String repeatWithTwoColor(String repeat, char color1, int count1, char color2, int count2) {
        return "§"+Character.toString(color1).concat(Strings.repeat(repeat, count1)).concat("§"+ color2).concat(Strings.repeat(repeat, count2));
    }

    public static Entity getDamager(Entity entity) {
        Entity attacker = entity;
        if (attacker != null) {
            if (NMS.isArrow(attacker)) {
                Projectile arrow = (Projectile) attacker;
                if (arrow.getShooter() instanceof Entity) {
                    attacker = (Entity) arrow.getShooter();
                }
            }
        }
        return attacker;
    }
}
