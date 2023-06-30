package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;

@RouletteManifest(name = "5초간 신속, 재생 1단계 부여")
public class Acceleration extends RouletteSingleEffect {
    @Override
    public void effect(Participant participant) {
        PotionEffects.SPEED.addPotionEffect(participant.getPlayer(), 100, 0, true);
        PotionEffects.REGENERATION.addPotionEffect(participant.getPlayer(), 100, 0, true);
        Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f은(는) 5초간 신속, 재생 1단계를 받습니다.");
    }
}
