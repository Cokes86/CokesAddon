package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.util.AttributeUtil;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;

@RouletteManifest(name = "체력 10% 감소")
public class HealthDecrease extends RouletteEffect {
    @Override
    public boolean apply(Participant participant) {
        final double max_health = AttributeUtil.getMaxHealth(participant.getPlayer());
        final double gain = max_health / 10.0;
        Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() - gain);
        return true;
    }
}
