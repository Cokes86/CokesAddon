package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

@RouletteManifest(name = "능력 등급 공개")
public class ShowRank extends RouletteSingleEffect {

    @Override
    public void effect(Participant participant) {
        if (participant.getAbility() != null) {
            if (participant.getAbility() instanceof Mix) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 등급은 "+mix.getSynergy().getRank().getRankName()+"§f입니다.");
                } else {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 등급은 "+mix.getFirst().getRank().getRankName()+"§f + " + mix.getSecond().getRank().getRankName()+ "§f입니다.");
                }
            } else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 등급은 "+participant.getAbility().getRank().getRankName()+"§f입니다.");
            }
        }
    }
}
