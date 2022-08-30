package com.cokes86.cokesaddon.util.nms;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface INMS {
    void setCritical(Entity arrow, boolean critical);
    boolean isCritical(Entity arrow);

    boolean damageMagicFixed(@NotNull Entity entity, @Nullable Player damager, float damage);
    boolean damageWither(@NotNull Entity entity, float damage);
    boolean damageVoid(@NotNull Entity entity, float damage);

    void changeSkin(Player player, UUID uuid);
    void reloadPlayerSkin(Player p);
    void setPlayerNameTag(Player player, UUID uuid);
    void saveSkinData();
    void clearSkinData();
    void addSkinData(UUID uuid);
    boolean isChangedSkin(Player player);

    IDummy createDummy(Location location, Player player);

    void hidePlayer(Player hide);
    void showPlayer(Player show);
    void onPlayerJoin(Player hiding, PlayerJoinEvent e);
    void onPlayerQuit(Player hiding, PlayerQuitEvent e);
    void injectPlayer(Player hiding, Player inject);
    void injectSelf(Player hiding);
}
