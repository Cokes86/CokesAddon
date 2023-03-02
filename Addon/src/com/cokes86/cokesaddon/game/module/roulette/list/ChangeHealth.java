package com.cokes86.cokesaddon.game.module.roulette.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;

import java.util.ArrayList;

@RouletteManifest(name = "특정 플레이어와 체력 변경", defaultPriority = 1)
public class ChangeHealth extends RouletteEffect {
    @Override
    public boolean apply(AbstractGame.Participant participant) {
        ArrayList<Participant> others = new ArrayList<>();
        for (Participant check : participant.getGame().getParticipants()) {
            if (check.equals(participant)) continue;
            if (check.getGame().hasModule(DeathManager.class)) {
                if (check.getGame().getModule(DeathManager.class).isExcluded(check.getPlayer())) {
                    continue;
                }
            }
            others.add(check);
        }
        if (others.size() == 0) {
            Bukkit.broadcastMessage("체력을 바꿀 플레이어가 없습니다.");
            return true;
        }
        Participant other = new Random().pick(others);
        double toOther = participant.getPlayer().getHealth();
        double toTarget = other.getPlayer().getHealth();

        Healths.setHealth(participant.getPlayer(), toTarget);
        Healths.setHealth(other.getPlayer(), toOther);
        Bukkit.broadcastMessage("§b"+participant.getPlayer().getName()+"§f와 §b"+other.getPlayer().getName()+"§f의 체력이 서로 바뀝니다.");
        return true;
    }
}
