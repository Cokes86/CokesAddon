package com.cokes86.cokesaddon.util.nms;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NMSUtil {
    private static final INMS INSTANCE;

    static {
        try {
            INSTANCE = Class.forName("com.cokes86.cokesaddon.util.nms." + ServerVersion.getName() + ".NMSImpl")
                    .asSubclass(INMS.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static IDummy createDummy(Location location, Player player) {
        return INSTANCE.createDummy(location, player);
    }

    public static void setCritical(Entity arrow, boolean critical) {
        INSTANCE.setCritical(arrow, critical);
    }

    public static boolean isCritical(Entity arrow) {
        return INSTANCE.isCritical(arrow);
    }

    public static boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage) {
        return INSTANCE.damageMagicFixed(entity, damager, damage);
    }

    public static boolean damageWither(@NotNull Entity entity, float damage) {
        return INSTANCE.damageWither(entity, damage);
    }

    public static boolean damageVoid(@NotNull Player entity, float damage) {
        return INSTANCE.damageVoid(entity, damage);
    }

    public static void changeSkin(Player player, UUID uuid) {
        INSTANCE.changeSkin(player, uuid);
    }

    public static void setPlayerNameTag(Player player, UUID uuid){
        INSTANCE.setPlayerNameTag(player, uuid);
    }

    public static void saveSkinData() {
        INSTANCE.saveSkinData();
    }

    public static void clearSkinData() {
        INSTANCE.clearSkinData();
    }

    public static void addSkinData(UUID uuid) { INSTANCE.addSkinData(uuid); }

    public static void reloadPlayerData(Player player) {
        INSTANCE.reloadPlayerSkin(player);
    }

    public static boolean isChangedSkin(Player player) {
        return INSTANCE.isChangedSkin(player);
    }

    public static void hidePlayer(Participant hide) { INSTANCE.hidePlayer(hide); }

    public static void showPlayer(Participant show) { INSTANCE.showPlayer(show); }
}
