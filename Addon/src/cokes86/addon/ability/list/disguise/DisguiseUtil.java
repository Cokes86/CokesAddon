package cokes86.addon.ability.list.disguise;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

public class DisguiseUtil {
    private static final IDisguise instance;

    static {
        try {
            instance = Class.forName("cokes86.addon.ability.list.disguise." + ServerVersion.getName())
                    .asSubclass(IDisguise.class).getConstructor().newInstance();
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

    public static void reloadPlayer(Player player) {
        instance.reloadPlayer(player);
    }

    public static boolean isChanged(Player player) {
        return instance.isChanged(player);
    }

    public static IDisguise getInstance() {
        return instance;
    }
}
