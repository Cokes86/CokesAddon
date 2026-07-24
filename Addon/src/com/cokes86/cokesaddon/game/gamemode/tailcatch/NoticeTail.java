package com.cokes86.cokesaddon.game.gamemode.tailcatch;

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
import java.util.Map;

@ModuleBase(NoticeTail.class)
public class NoticeTail implements ListenerModule, Observer {

    private final Map<Participant, BossBar> noticeBarMap = new HashMap<>();
    private final TailCatch tailCatch;

    public NoticeTail(TailCatch game) {
        this.tailCatch = game;
        game.attachObserver(this);
    }

    @Override
    public void update(GameUpdate update) {
        if (update == GameUpdate.END) {
            clear();
        }
    }

    public void initializeBossBar() {
        clear();

        for (Participant participant : tailCatch.getTailList()) {
            BossBar bar = Bukkit.createBossBar(
                    getTitle(participant),
                    BarColor.GREEN,
                    BarStyle.SOLID
            );

            bar.addPlayer(participant.getPlayer());
            bar.setVisible(true);
            noticeBarMap.put(participant, bar);
        }
    }

    public void updateBossBar() {
        for (Participant participant : tailCatch.getTailList()) {
            BossBar bar = noticeBarMap.get(participant);

            if (bar == null) {
                bar = Bukkit.createBossBar(
                        getTitle(participant),
                        BarColor.GREEN,
                        BarStyle.SOLID
                );
                bar.addPlayer(participant.getPlayer());
                bar.setVisible(true);
                noticeBarMap.put(participant, bar);
            } else {
                bar.setTitle(getTitle(participant));
            }
        }
    }

    public void removeBossBar(Participant participant) {
        BossBar bar = noticeBarMap.remove(participant);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void clear() {
        for (BossBar bar : noticeBarMap.values()) {
            bar.removeAll();
        }
        noticeBarMap.clear();
    }

    private String getTitle(Participant participant) {
        if (tailCatch.getTailList().size() <= 1) {
            return "§a마지막 생존자입니다!";
        }

        Participant target = tailCatch.getNextTail(participant);
        return "당신의 타겟 : " + target.getPlayer().getDisplayName();
    }

    public interface Handler {
        NoticeTail getNoticeTail();
    }
}