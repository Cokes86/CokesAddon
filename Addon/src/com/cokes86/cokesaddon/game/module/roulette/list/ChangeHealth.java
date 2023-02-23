package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;

@RouletteManifest(name = "특정 플레이어와 체력 변경", defaultPriority = 1)
public class ChangeHealth extends RouletteEffect {
    @Override
    public boolean apply(AbstractGame.Participant participant) {
        AbstractGame.Participant other = new Random().pick(participant.getGame().getParticipants().toArray(new AbstractGame.Participant[]{}));
        if (other.equals(participant)) return apply(participant);
        if (other.getAbility() == null) return apply(participant);
        double toOther = participant.getPlayer().getHealth();
        double toTarget = other.getPlayer().getHealth();

        Healths.setHealth(participant.getPlayer(), toTarget);
        Healths.setHealth(other.getPlayer(), toOther);
        Bukkit.broadcastMessage("§b"+participant.getPlayer().getName()+"§f와 §b"+other.getPlayer().getName()+"§f의 체력이 서로 바뀝니다.");
        return true;
    }
}
