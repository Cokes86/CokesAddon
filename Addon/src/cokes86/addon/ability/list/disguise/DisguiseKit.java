package cokes86.addon.ability.list.disguise;

import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.entity.Player;

public class DisguiseKit {
    private static final IDisguise instance;

    static {
        try {
            instance = Class.forName("cokes86.addon.ability.list.disguise." + ServerVersion.getName())
                    .asSubclass(IDisguise.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }

    public static void changeSkin(Player player, String targetName) {
        instance.changeSkin(player, targetName);
    }

    public static void setPlayerNameTag(Player player, String targetName){
        instance.setPlayerNameTag(player, targetName);
    }
}
