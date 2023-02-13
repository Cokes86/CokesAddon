package com.cokes86.cokesaddon.game.module.roulette.list;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;

@RouletteManifest(name = "위치 공개")
public class Location extends RouletteEffect {

    @Override
    public boolean apply(Participant participant) {
        double x = participant.getPlayer().getLocation().getX();
        double y = participant.getPlayer().getLocation().getY();
        double z = participant.getPlayer().getLocation().getZ();
        DecimalFormat format = new DecimalFormat("0.##");

        Bukkit.broadcastMessage("========== "+ participant.getPlayer().getName()+"의 위치 ==========");
        Bukkit.broadcastMessage("x: "+format.format(x));
        Bukkit.broadcastMessage("y: "+format.format(y));
        Bukkit.broadcastMessage("z: "+format.format(z));
        Bukkit.broadcastMessage("========================");
        return false;
    }
    
}
