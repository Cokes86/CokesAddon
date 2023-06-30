package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;

@RouletteManifest(name = "3초간 기절", defaultPriority = 4)
public class StunParticipant extends RouletteSingleEffect {
    public void effect(Participant participant) {
        Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f이(가) 3초간 기절합니다.");
        Stun.apply(participant, TimeUnit.SECONDS, 3);
    }
}
