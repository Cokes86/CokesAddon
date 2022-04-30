package com.cokes86.cokesaddon.gamemode.tailcatch;

import daybreak.abilitywar.game.AbstractGame.GameUpdate;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.HashMap;

@ModuleBase(NoticeTail.class)
public class NoticeTail implements ListenerModule, Observer {
    private final HashMap<Participant, BossBar> noticeBarMap = new HashMap<>();
    private final TailCatch tailCatch;

    public NoticeTail(TailCatch game) {
        game.attachObserver(this);
        this.tailCatch = game;
    }

    @Override
    public void update(GameUpdate update) {
        if (update == GameUpdate.START) {
            for (Participant participant : tailCatch.getParticipants()) {
                Participant tail = tailCatch.getNextTail(participant);
                BossBar bar = Bukkit.createBossBar("당신의 타겟 : "+tail.getPlayer().getDisplayName(), BarColor.GREEN, BarStyle.SOLID);
                bar.addPlayer(participant.getPlayer());
                bar.setVisible(true);
                noticeBarMap.put(participant, bar);
            }
        } else if (update == GameUpdate.END) {
            for (Participant participant : noticeBarMap.keySet()) {
                noticeBarMap.get(participant).removeAll();
            }
            noticeBarMap.clear();
        }
    }

    protected void updateBossBar() {
        for (Participant participant : noticeBarMap.keySet()) {
            Participant tail = tailCatch.getNextTail(participant);
            BossBar bar = noticeBarMap.get(participant);
            bar.setTitle("당신의 타겟 : "+ tail.getPlayer().getDisplayName());
        }
    }

    public interface Handler {
        NoticeTail getNoticeTail();
    }
}
