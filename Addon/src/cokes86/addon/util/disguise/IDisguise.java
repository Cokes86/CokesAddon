package cokes86.addon.util.disguise;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface IDisguise {
    void changeSkin(Player player, UUID uuid);
    void reloadPlayer(Player p);
    void setPlayerNameTag(Player player, UUID uuid);
    void saveData();
    void clearData();
    void addData(UUID uuid);
    boolean isChanged(Player player);
}
