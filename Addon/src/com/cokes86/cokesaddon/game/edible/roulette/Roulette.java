package com.cokes86.cokesaddon.game.edible.roulette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.event.Listener;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.game.edible.roulette.RouletteConfig.SettingObject;
import com.cokes86.cokesaddon.game.edible.roulette.list.*;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

public class Roulette extends GameTimer implements Listener {
    private final HashSet<RouletteEffect> effects = new HashSet<>();
    public static final RouletteConfig config = new RouletteConfig(CokesAddon.getAddonFile("CokesRoulette.yml"));
    private static final SettingObject<Integer> period = config.new SettingObject<>("period", 45);

    private static final SettingObject<Boolean> teleport = config.new SettingObject<>("roulette.teleport", true);

    public static final List<SettingObject<?>> list = new ArrayList<>();

    public Roulette(AbstractGame abstractGame) {
        abstractGame.super(TaskType.INFINITE, -1);

        for (Participant participant : abstractGame.getParticipants()) {
            if (teleport.getValue()) {
                for (Participant participant2 : abstractGame.getParticipants()) {
                    if (!participant.equals(participant2)) {
                        effects.add(new Teleport(participant, participant2));
                    }
                }
            }
            effects.add(new ShowSpecies(participant));
            effects.add(new ShowRank(participant));
        }

        list.add(teleport);

        setPeriod(TimeUnit.TICKS, 1);
    }
    
    int count = 0;
    @Override
    public void run(int arg0) {
        count++;
    }
}