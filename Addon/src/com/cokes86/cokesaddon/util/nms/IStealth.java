package com.cokes86.cokesaddon.util.nms;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public interface IStealth {

    void hidePlayer();
    void showPlayer();
    void onPlayerJoin(PlayerJoinEvent e);
    void onPlayerQuit(PlayerQuitEvent e);
    void injectPlayer(Player inject);
    void injectSelf();
}
