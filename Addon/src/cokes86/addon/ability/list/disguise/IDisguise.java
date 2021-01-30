package cokes86.addon.ability.list.disguise;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

public interface IDisguise {
    void changeSkin(Player player, UUID uuid);
    void reloadPlayer(Player p);
    void setPlayerNameTag(Player player, UUID uuid);
    void saveData();
    void clearData();
    boolean isChanged(Player player);
}
