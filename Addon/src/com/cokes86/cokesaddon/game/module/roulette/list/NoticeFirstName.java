package com.cokes86.cokesaddon.game.module.roulette.list;

import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.AbstractMix.MixParticipant;

@RouletteManifest(name = "능력 첫글자 공개")
public class NoticeFirstName extends RouletteEffect {
    @Override
    public boolean apply(Participant participant) {
        if (participant.hasAbility()) {
            if (participant instanceof MixParticipant) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력명 첫글자는 §b"+mix.getSynergy().getName().toCharArray()[0]+"§f입니다.");
                    return true;
                }
                Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력명 첫글자는 §b"+mix.getFirst().getName().toCharArray()[0]+"§f + §b" + mix.getSecond().getName().toCharArray()[0]+ "§f입니다.");
                return true;
            }
            Bukkit.broadcastMessage(participant.getPlayer().getName()+"의 능력명 첫글자는 §b"+participant.getAbility().getName().toCharArray()[0]+"§f입니다.");
            return true;
        }
        return false;
    }
}
