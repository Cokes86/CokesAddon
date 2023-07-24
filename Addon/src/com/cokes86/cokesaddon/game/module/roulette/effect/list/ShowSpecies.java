package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import com.cokes86.cokesaddon.util.compatibility.Compatibility;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.list.mix.triplemix.TripleMix;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

import java.util.List;
import java.util.StringJoiner;

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
            } else if (participant.getAbility() instanceof TripleMix) {
                TripleMix mix = (TripleMix) participant.getAbility();
                final StringJoiner joiner = new StringJoiner(" §f+ ");
                joiner.add(mix.getFirst().getSpecies().getSpeciesName());
                joiner.add(mix.getSecond().getSpecies().getSpeciesName());
                joiner.add(mix.getThird().getSpecies().getSpeciesName());
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+ joiner +"§f입니다.");
            }
            else if (Compatibility.isOverlap(participant)) {
                String name;
                List<AbilityBase> abilities = Compatibility.getAbililitiesInOverlap(participant);
                if (abilities.size() > 0) {
                    final StringJoiner joiner = new StringJoiner(" §f+ ");
                    for (AbilityBase ability : abilities) {
                        joiner.add(ability.getSpecies().getSpeciesName());
                    }
                    name = joiner.toString();
                } else {
                    name = participant.getAbility().getSpecies().getSpeciesName();
                }
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+name+"§f입니다.");
            }
            else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력 종족은 "+participant.getAbility().getSpecies().getSpeciesName()+"§f입니다.");
            }
        }
    }
    
}
