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

@RouletteManifest(name = "능력 첫글자 공개")
public class ShowFirstName extends RouletteSingleEffect {
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
            } else if (participant.getAbility() instanceof TripleMix) {
                TripleMix mix = (TripleMix) participant.getAbility();
                final StringJoiner joiner = new StringJoiner(" §f+ ");
                joiner.add("§e["+mix.getFirst().getName().toCharArray()[0]+"]");
                joiner.add("§e["+mix.getSecond().getName().toCharArray()[0]+"]");
                joiner.add("§e["+mix.getThird().getName().toCharArray()[0]+"]");
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 "+ joiner +"§f입니다.");
            } else if (Compatibility.isOverlap(participant)) {
                String name;
                List<AbilityBase> abilities = Compatibility.getAbililitiesInOverlap(participant);
                if (abilities.size() > 0) {
                    final StringJoiner joiner = new StringJoiner(" §f+ ");
                    for (AbilityBase ability : abilities) {
                        joiner.add("§e["+ability.getName().toCharArray()[0]+"]");
                    }
                    name = joiner.toString();
                } else {
                    name = "중";
                }
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 "+name+"§f입니다.");
            } else {
                Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력명 첫글자는 §e["+participant.getAbility().getName().toCharArray()[0]+"]§f입니다.");
            }
        } else {
            Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 능력이 없습니다.");
        }
    }
}
