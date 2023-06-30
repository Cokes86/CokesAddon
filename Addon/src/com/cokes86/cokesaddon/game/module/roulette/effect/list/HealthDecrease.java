package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import com.cokes86.cokesaddon.util.AttributeUtil;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Bukkit;

@RouletteManifest(name = "체력 10% 감소")
public class HealthDecrease extends RouletteSingleEffect {
    @Override
    public void effect(Participant participant) {
        final double max_health = AttributeUtil.getMaxHealth(participant.getPlayer());
        final double gain = max_health / 10.0;
        Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() - gain);
        Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 체력이 10% 감소합니다.");
    }
}
