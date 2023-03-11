package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.PotionEffects;

@RouletteManifest(name = "5초간 신속, 재생 1단계 부여")
public class Acceleration extends RouletteEffect {
    @Override
    public boolean apply(Participant participant) {
        PotionEffects.SPEED.addPotionEffect(participant.getPlayer(), 100, 0, true);
        PotionEffects.HEALTH_BOOST.addPotionEffect(participant.getPlayer(), 100, 0, true);
        return true;
    }
}
