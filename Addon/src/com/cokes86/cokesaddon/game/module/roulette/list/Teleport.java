package com.cokes86.cokesaddon.game.module.roulette.list;

import daybreak.abilitywar.game.module.DeathManager;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.random.Random;

import java.util.ArrayList;

@RouletteManifest(name = "특정 플레이어에게 이동", defaultPriority = 3)
public class Teleport extends RouletteEffect {
    public boolean apply(Participant participant) {
        ArrayList<Participant> others = new ArrayList<>(getParticipantsWithoutEliminater());
        others.remove(participant);
        if (others.size() == 0) {
            Bukkit.broadcastMessage("체력을 바꿀 플레이어가 없습니다.");
            return true;
        }
        Participant other = new Random().pick(others);
        participant.getPlayer().teleport(other.getPlayer().getLocation());
        Bukkit.broadcastMessage(participant.getPlayer().getName()+"이(가) "+other.getPlayer().getName()+"에게 이동합니다.");
        return true;
    }
}
