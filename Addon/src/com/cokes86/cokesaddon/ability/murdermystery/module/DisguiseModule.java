package com.cokes86.cokesaddon.ability.murdermystery.module;

import com.cokes86.cokesaddon.ability.murdermystery.murder.DisguiserMurderer;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@ModuleBase(DisguiseModule.class)
public class DisguiseModule implements ListenerModule {
    private static final String TEAM_NAME = "DisguiseNameTag";

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        NMSUtil.saveSkinData();
        Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = score.registerNewTeam(TEAM_NAME);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName("§kDisguise");
            team.addEntry(player.getName());
        }
        Bukkit.broadcastMessage("§7§o변장술사가 당신의 정보를 습득합니다.");
    }

    @Override
    public void unregister() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NMSUtil.isChangedSkin(player)) {
                NMSUtil.changeSkin(player, player.getUniqueId());
                NMSUtil.reloadPlayerData(player);
            }
            player.setPlayerListName(player.getName());
        }
        NMSUtil.clearSkinData();
        Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = score.getTeam(TEAM_NAME);
        team.unregister();
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

    @EventHandler(priority = EventPriority.LOWEST)
    private void onMurder(MurderEvent e) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NMSUtil.isChangedSkin(player)) {
                NMSUtil.changeSkin(player, player.getUniqueId());
                NMSUtil.reloadPlayerData(player);
            }
        }

        boolean disguise = false;
        for (Participant participant : GameManager.getGame().getParticipants()) {
            if (participant.getAbility() instanceof DisguiserMurderer) {
                disguise = true;
            }
        }

        if (!disguise) {
            this.unregister();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onArrowKill(ArrowKillEvent e) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NMSUtil.isChangedSkin(player)) {
                NMSUtil.changeSkin(player, player.getUniqueId());
                NMSUtil.reloadPlayerData(player);
            }
        }
        boolean disguise = false;
        for (Participant participant : GameManager.getGame().getParticipants()) {
            if (participant.getAbility() instanceof DisguiserMurderer) {
                disguise = true;
            }
        }

        if (!disguise) {
            this.unregister();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerDeath(PlayerDeathEvent e) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (NMSUtil.isChangedSkin(player)) {
                NMSUtil.changeSkin(player, player.getUniqueId());
                NMSUtil.reloadPlayerData(player);
            }
        }
        boolean disguise = false;
        for (Participant participant : GameManager.getGame().getParticipants()) {
            if (participant.getAbility() instanceof DisguiserMurderer) {
                disguise = true;
            }
        }

        if (!disguise) {
            this.unregister();
        }
    }
}
