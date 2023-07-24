package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteEffect;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;

@RouletteManifest(name = "랜덤 상태이상 5초 부여", defaultPriority = 4)
public class RandomEffect implements RouletteEffect {
    @Override
    public void apply(Participant... target) {
        final Random random = new Random();
        EffectRegistry.EffectRegistration<?> registration = random.pick(EffectRegistry.values().toArray(new EffectRegistry.EffectRegistration[0]));
        AbstractGame.Effect e = registration.apply(target[0], TimeUnit.SECONDS, 5);
        if (e == null) {
            apply(target);
        }
        Bukkit.broadcastMessage("[룰렛] §b"+target[0].getPlayer().getName()+"§f에게 "+registration.getManifest().displayName()+"§f 효과를 5초간 부여합니다.");
    }
}
