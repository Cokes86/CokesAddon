package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;

@RouletteManifest(name = "쿨타임 초기화", defaultPriority = 3)
public class CooldownReset extends RouletteEffect{

    @Override
    public boolean apply(Participant participant) {
        final AbilityBase ability = participant.getAbility();
		if (ability != null) {
			for (GameTimer timer : ability.getTimers()) {
				if (timer instanceof Cooldown.CooldownTimer) {
					timer.stop(false);
				}
			}
            return true;
		}
        return false;
    }
    
}
