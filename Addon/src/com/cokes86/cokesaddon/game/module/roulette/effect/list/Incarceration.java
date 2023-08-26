package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteTimerEffect;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

@RouletteManifest(name = "10초간 10블럭 이내에 감금", defaultPriority = 2)
public class Incarceration extends RouletteTimerEffect implements Listener {
    private Location center;
    private Participant participant;

    @Override
    public void beforeStart(Participant... target) {
        setPeriod(TimeUnit.TICKS, 2);
        setTaskType(TaskType.NORMAL);
        setMaximunCount(100);
        Bukkit.broadcastMessage("[룰렛] §b"+target[0].getPlayer().getName()+"§f은 10초간 10블럭 이내에서만 활동이 가능해집니다.");
        this.participant = target[0];
    }

    @Override
    public void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        center = participant.getPlayer().getLocation();
    }

    @Override
    public void onEnd() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onSilentEnd() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void run(int count) {
        final RGB rgb = RGB.PURPLE;
        for (Location l : Circle.iteratorOf(center, 10, 10 * 6).iterable()) {
            l.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(center.getWorld()), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.1);
            ParticleLib.REDSTONE.spawnParticle(l, rgb);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer().equals(participant.getPlayer())) {
            Location to = e.getTo();
            assert to != null;
            if (Math.pow((to.getX() - center.getX()), 2) + Math.pow((to.getZ() - center.getZ()), 2) > 100) {
                e.setCancelled(true);
            }
        }
    }
}
