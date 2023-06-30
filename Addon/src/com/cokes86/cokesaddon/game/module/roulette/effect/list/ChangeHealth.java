package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteTargetEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Bukkit;

@RouletteManifest(name = "특정 플레이어와 체력 변경", defaultPriority = 1, display = "$[player1]와 체력 변경")
public class ChangeHealth extends RouletteTargetEffect {
    @Override
    public void effect(Participant participant1, Participant participant2) {
        double toOther = participant1.getPlayer().getHealth();
        double toTarget = participant2.getPlayer().getHealth();

        Healths.setHealth(participant1.getPlayer(), toTarget);
        Healths.setHealth(participant2.getPlayer(), toOther);
        Bukkit.broadcastMessage("[룰렛] §b"+participant1.getPlayer().getName()+"§f와(과) §b"+participant2.getPlayer().getName()+"§f의 체력이 서로 바뀝니다.");
    }
}
