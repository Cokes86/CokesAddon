package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

@RouletteManifest(name = "능력 첫글자 공개")
public class NoticeFirstName extends RouletteSingleEffect {
    @Override
    public void effect(Participant participant) {
        if (participant.getAbility() != null) {
            if (participant.getAbility() instanceof Mix) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.hasSynergy()) {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 §e["+mix.getSynergy().getName().toCharArray()[0]+"]§f입니다.");
                } else {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 §e["+mix.getFirst().getName().toCharArray()[0]+"]§f + §e[" + mix.getSecond().getName().toCharArray()[0]+ "]§f입니다.");
                }
            } else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 §e["+participant.getAbility().getName().toCharArray()[0]+"]§f입니다.");
            }
        } else {
            Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력이 없습니다.");
        }
    }
}
