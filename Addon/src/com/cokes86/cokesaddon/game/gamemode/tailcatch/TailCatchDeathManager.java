package com.cokes86.cokesaddon.game.gamemode.tailcatch;

import com.cokes86.cokesaddon.util.AttributeUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitRunnable;

public class TailCatchDeathManager extends DeathManager {

    private final boolean autoRespawn = DeathSettings.getAutoRespawn();
    private final TailCatch game;

    public TailCatchDeathManager(TailCatch game) {
        super(game);
        this.game = game;
    }

    @Override
    public void Operation(final Participant victim) {
        if (excludedPlayers.contains(victim.getPlayer().getUniqueId())) {
            return;
        }

        Bukkit.broadcastMessage("§c" + victim.getPlayer().getName() + "§f님이 탈락하셨습니다.");

        excludedPlayers.add(victim.getPlayer().getUniqueId());

        victim.getPlayer().setGameMode(GameMode.SPECTATOR);
        AttributeUtil.setMaxHealth(victim.getPlayer(), Settings.getDefaultMaxHealth());

        game.getNoticeTail().removeBossBar(victim);

        if (game.removeTail(victim)) {
            game.getNoticeTail().updateBossBar();
        }

        if (autoRespawn) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (victim.getPlayer().isOnline()) {
                        NMS.respawn(victim.getPlayer());
                        victim.getPlayer().setGameMode(GameMode.SPECTATOR);
                    }
                }
            }.runTaskLater(AbilityWar.getPlugin(), 2L);
        }

        if (game.getAliveCount() == 1) {
            Participant winner = game.getLastAliveParticipant();

            if (winner != null) {
                game.Win(winner);
            }
        }
    }
}