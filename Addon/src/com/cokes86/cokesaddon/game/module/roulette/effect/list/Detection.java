package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteTargetEffect;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@RouletteManifest(name = "5초간 랜덤 플레이어의 근거리 공격 무시", defaultPriority = 3, display = "5초간 $[player1]의 근거리 공격 무시")
public class Detection extends RouletteTargetEffect {

    @Override
    protected void effect(Participant participant1, Participant participant2) {
        Bukkit.broadcastMessage("[룰렛] §b"+participant1.getPlayer().getName()+"§f은 5초간 §b"+participant2.getPlayer().getName()+"§f의 근거리 공격을 무시합니다.");
        GameTimer timer = new DetectionTimer(participant1, participant2);
        timer.start();
    }

    private class DetectionTimer extends GameTimer implements Listener {
        private final Participant participant, detection;

        public DetectionTimer(Participant participant, Participant detection) {
            participant.getGame().super(TaskType.NORMAL, 5);
            this.participant = participant;
            this.detection = detection;
        }

        @Override
        protected void onStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void onEnd() {
            HandlerList.unregisterAll(this);
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.getDamager().equals(detection.getPlayer()) && e.getEntity().equals(participant.getPlayer())) {
                SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer().getLocation(), 1, 1.7f);
                e.setCancelled(true);
            }
        }
    }
}
