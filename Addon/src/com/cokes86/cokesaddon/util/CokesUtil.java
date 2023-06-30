package com.cokes86.cokesaddon.util;

import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import daybreak.google.common.collect.ImmutableSet.Builder;
import kotlin.ranges.RangesKt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

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

    public static void healPlayer(Player player, double healamount) {
        final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healamount, RegainReason.CUSTOM);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            Healths.setHealth(player, player.getHealth() + event.getAmount());
        }
    }

    public static void vampirePlayer(Player player, double vampireAmount) {
        final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, vampireAmount, RegainReason.CUSTOM);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            player.setHealth(RangesKt.coerceIn(player.getHealth() + event.getAmount(), 0, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        }
    }
}
