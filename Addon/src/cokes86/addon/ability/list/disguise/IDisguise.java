package cokes86.addon.ability.list.disguise;

import org.bukkit.entity.Player;

import java.io.IOException;

public interface IDisguise {

    void changeSkin(Player player, String targetName);
    void reloadPlayer(Player p);
    void setPlayerNameTag(Player player, String targetName);
    String getUUID(String playername) throws IOException;
    String getProfile(String uuid) throws IOException;
}
