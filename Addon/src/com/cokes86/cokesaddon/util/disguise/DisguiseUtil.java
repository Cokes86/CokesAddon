package com.cokes86.cokesaddon.util.disguise;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DisguiseUtil {
    private static final DisguiseImpl instance;

    static {
        try {
            instance = Class.forName("com.cokes86.cokesaddon.util.disguise." + ServerVersion.getName())
                    .asSubclass(DisguiseImpl.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static void changeSkin(Player player, UUID uuid) {
        instance.changeSkin(player, uuid);
    }

    public static void setPlayerNameTag(Player player, UUID uuid){
        instance.setPlayerNameTag(player, uuid);
    }

    public static void saveData() {
        instance.saveData();
    }

    public static void clearData() {
        instance.clearData();
    }

    public static void addData(UUID uuid) { instance.addData(uuid); }

    public static void reloadPlayer(Player player) {
        instance.reloadPlayer(player);
    }

    public static boolean isChanged(Player player) {
        return instance.isChanged(player);
    }

    public static DisguiseImpl getInstance() {
        return instance;
    }

}
