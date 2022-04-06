package cokes86.addon.gamemode.tailcatch;

import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TailCatchDeathManager extends DeathManager {
    private final boolean autoRespawn = DeathSettings.getAutoRespawn();
    private final TailCatch game;

    public TailCatchDeathManager(TailCatch game) {
        super(game);
        this.game = game;
    }

    @Override
    public void Operation(Participant victim) {
        Bukkit.broadcastMessage("§c" + victim.getPlayer().getName() + "§f님이 탈락하셨습니다.");
        victim.getPlayer().setGameMode(GameMode.SPECTATOR);
        excludedPlayers.add(victim.getPlayer().getUniqueId());
        AttributeUtil.setMaxHealth(victim.getPlayer(), Settings.getDefaultMaxHealth());
        if (autoRespawn) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    NMS.respawn(victim.getPlayer());
                }
            }.runTaskLater(AbilityWar.getPlugin(), 2L);
        }
        if (game.removeTail(victim)) {
            game.getNoticeTail().updateBossBar();
        }

        if (game.getParticipants().size() - excludedPlayers.size() == 1) {
            List<Participant> winner = new ArrayList<>(game.getParticipants());
            winner.removeIf(participant -> excludedPlayers.contains(participant.getPlayer().getUniqueId()));
            game.Win(winner.get(0));
        }
    }
}
