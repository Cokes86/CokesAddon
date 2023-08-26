package com.cokes86.cokesaddon.ability.murdermystery.module;

import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;

@ModuleBase(DisguiseModule.class)
public class DisguiseModule implements ListenerModule {
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        NMSUtil.saveSkinData();
    }

    @Override
    public void unregister() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NMSUtil.isChangedSkin(player)) {
                NMSUtil.changeSkin(player, player.getUniqueId());
                NMSUtil.setPlayerNameTag(player, player.getUniqueId());
                NMSUtil.reloadPlayerData(player);
            }
        }
        NMSUtil.clearSkinData();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (NMSUtil.isChangedSkin(e.getPlayer())) {
            NMSUtil.changeSkin(e.getPlayer(), e.getPlayer().getUniqueId());
            NMSUtil.setPlayerNameTag(e.getPlayer(), e.getPlayer().getUniqueId());
            NMSUtil.reloadPlayerData(e.getPlayer());
        }
    }
}
