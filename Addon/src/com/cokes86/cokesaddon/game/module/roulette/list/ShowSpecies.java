package com.cokes86.cokesaddon.game.module.roulette.list;

import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.AbstractMix.MixParticipant;

@RouletteManifest(name = "능력 종족 공개")
public class ShowSpecies extends RouletteEffect {

    @Override
    public boolean apply(Participant participant) {
        if (participant.hasAbility()) {
            if (participant instanceof MixParticipant) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 종족은 "+mix.getSynergy().getSpecies().getSpeciesName()+"§f입니다.");
                    return true;
                }
                Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 종족은 "+mix.getFirst().getSpecies().getSpeciesName()+"§f + " + mix.getSecond().getSpecies().getSpeciesName()+ "§f입니다.");
                return true;
            }
            Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 종족은 "+participant.getAbility().getSpecies().getSpeciesName()+"§f입니다.");
            return true;
        }
        return false;
    }
    
}
