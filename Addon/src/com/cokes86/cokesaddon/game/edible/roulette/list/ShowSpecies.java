package com.cokes86.cokesaddon.game.edible.roulette.list;

import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.edible.roulette.RouletteEffect;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.AbstractMix.MixParticipant;

public class ShowSpecies extends RouletteEffect {
    private final Participant participant;
    public ShowSpecies(Participant participant) {
        this.participant = participant;
    }

    @Override
    public String rouletteName() {
        return participant.getPlayer().getName()+"의 능력 종족 공개";
    }

    @Override
    public boolean apply() {
        if (participant.hasAbility()) {
            if (participant instanceof MixParticipant) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 등급은 "+mix.getSynergy().getSpecies().getSpeciesName()+"§f입니다.");
                    return true;
                }
                Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 등급은 "+mix.getFirst().getSpecies().getSpeciesName()+"§f + " + mix.getSecond().getSpecies().getSpeciesName()+ "§f입니다.");
                return true;
            }
            Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력 등급은 "+participant.getAbility().getSpecies().getSpeciesName()+"§f입니다.");
            return true;
        }
        return false;
    }
    
}
