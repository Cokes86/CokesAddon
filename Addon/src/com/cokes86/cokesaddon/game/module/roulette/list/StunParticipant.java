package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@RouletteManifest(name = "3초간 기절", defaultPriority = 4)
public class StunParticipant extends RouletteEffect {
    @Override
    public boolean apply(Participant participant) {
        Stun.apply(participant, TimeUnit.SECONDS, 3);
        return true;
    }
}
