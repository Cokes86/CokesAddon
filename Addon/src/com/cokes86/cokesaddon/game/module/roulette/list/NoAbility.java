package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.effect.list.Seal;
import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.PotionEffects;

@RouletteManifest(name = "비활성화 및 힘2 5초 부여", defaultPriority = 4)
public class NoAbility extends RouletteEffect {
    @Override
    public boolean apply(AbstractGame.Participant participant) {
        Seal.apply(participant, TimeUnit.SECONDS, 5);
        PotionEffects.INCREASE_DAMAGE.addPotionEffect(participant.getPlayer(), 100, 1, true);
        return true;
    }
}
