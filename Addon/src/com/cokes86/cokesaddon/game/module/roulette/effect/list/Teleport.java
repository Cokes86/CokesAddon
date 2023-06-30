package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteTargetEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import org.bukkit.Bukkit;

@RouletteManifest(name = "특정 플레이어에게 텔레포트", defaultPriority = 3, display = "$[player1]에게 텔레포트")
public class Teleport extends RouletteTargetEffect {
    public void effect(Participant participant1, Participant participant2) {
        participant1.getPlayer().teleport(participant2.getPlayer().getLocation());
        Bukkit.broadcastMessage("[룰렛] §b"+participant1.getPlayer().getName()+"§f이(가) §b"+participant2.getPlayer().getName()+"§f에게 이동합니다.");
    }
}
