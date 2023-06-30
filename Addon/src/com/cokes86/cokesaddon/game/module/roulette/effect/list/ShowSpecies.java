package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

@RouletteManifest(name = "능력 종족 공개")
public class ShowSpecies extends RouletteSingleEffect {

    @Override
    public void effect(Participant participant) {
        if (participant.getAbility() != null) {
            if (participant.getAbility() instanceof Mix) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+mix.getSynergy().getSpecies().getSpeciesName()+"§f입니다.");
                } else {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+mix.getFirst().getSpecies().getSpeciesName()+"§f + " + mix.getSecond().getSpecies().getSpeciesName()+ "§f입니다.");
                }
            } else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+participant.getAbility().getSpecies().getSpeciesName()+"§f입니다.");
            }
        }
    }
    
}
