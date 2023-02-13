package com.cokes86.cokesaddon.game.module.roulette.list;

import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.random.Random;

@RouletteManifest(name = "특정 플레이어에게 이동", defaultPriority = 3)
public class Teleport extends RouletteEffect {
    public boolean apply(Participant participant) {
        Participant other = new Random().pick(participant.getGame().getParticipants().toArray(new Participant[]{}));
        if (other.equals(participant)) return apply(participant);
        participant.getPlayer().teleport(other.getPlayer().getLocation());
        Bukkit.broadcastMessage(participant.getPlayer().getName()+"이(가) "+other.getPlayer().getName()+"에게 이동합니다.");
        return true;
    }
}
