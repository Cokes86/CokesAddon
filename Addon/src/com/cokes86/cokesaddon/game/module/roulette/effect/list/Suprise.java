package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Bukkit;

@RouletteManifest(name = "서프라이즈", defaultPriority = 1)
public class Suprise implements RouletteEffect {
    @Override
    public void apply(Participant... participants) {
        for (Participant participant : participants) {
            Healths.setHealth(participant.getPlayer(), 10000000);
        }
        Bukkit.broadcastMessage("[룰렛] 서프라이즈!! (전체 체력 회복)");
    }
}
