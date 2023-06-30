package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import java.text.DecimalFormat;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;

@RouletteManifest(name = "위치 공개")
public class Location extends RouletteSingleEffect {

    @Override
    public void effect(Participant participant) {
        double x = participant.getPlayer().getLocation().getX();
        double y = participant.getPlayer().getLocation().getY();
        double z = participant.getPlayer().getLocation().getZ();
        DecimalFormat format = new DecimalFormat("0.##");

        Bukkit.broadcastMessage("[룰렛] §b"+ participant.getPlayer().getName()+"§f의 위치 ==========");
        Bukkit.broadcastMessage("x: §e"+format.format(x));
        Bukkit.broadcastMessage("y: §e"+format.format(y));
        Bukkit.broadcastMessage("z: §e"+format.format(z));
        Bukkit.broadcastMessage("========================");
    }
    
}
