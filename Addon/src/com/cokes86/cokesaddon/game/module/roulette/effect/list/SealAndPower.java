package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.effect.list.Seal;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;

@RouletteManifest(name = "비활성화 및 힘2 5초 부여", defaultPriority = 4)
public class SealAndPower extends RouletteSingleEffect {
    @Override
    public void effect(AbstractGame.Participant participant) {
        Seal.apply(participant, TimeUnit.SECONDS, 5);
        PotionEffects.INCREASE_DAMAGE.addPotionEffect(participant.getPlayer(), 100, 1, true);
        Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f은(는) 5초간 능력이 봉인되고 힘 2단계를 부여합니다.");
    }
}
