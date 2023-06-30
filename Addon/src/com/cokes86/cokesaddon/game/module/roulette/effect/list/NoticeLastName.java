package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import org.bukkit.Bukkit;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

@RouletteManifest(name = "능력 끝글자 공개")
public class NoticeLastName extends RouletteSingleEffect {
    @Override
    public void effect(Participant participant) {
        if (participant.getAbility() != null) {
            if (participant.getAbility() instanceof Mix) {
                Mix mix = (Mix) participant.getAbility();
                if (mix.getSynergy() != null) {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 끝글자는 §e["+mix.getSynergy().getName().toCharArray()[mix.getSynergy().getName().length() - 1]+"]§f입니다.");
                } else {
                    Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 끝글자는 §e["+mix.getFirst().getName().toCharArray()[mix.getFirst().getName().length() -1]+"]§f + §e[" + mix.getSecond().getName().toCharArray()[mix.getSecond().getName().length() -1]+ "]§f입니다.");
                }
            } else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 끝글자는 §e["+participant.getAbility().getName().toCharArray()[participant.getAbility().getName().length() - 1]+"]§f입니다.");
            }
        }
    }
}
