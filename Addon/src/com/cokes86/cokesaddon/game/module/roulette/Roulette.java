package com.cokes86.cokesaddon.game.module.roulette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;
import com.cokes86.cokesaddon.game.module.roulette.list.*;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.event.GameStartEvent;
import daybreak.abilitywar.game.module.Invincibility;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.game.module.Invincibility.Observer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@ModuleBase(Roulette.class)
public class Roulette implements ListenerModule {
    private final HashSet<RouletteEffect> effects = new HashSet<>();
    public static final RouletteConfig config = new RouletteConfig(CokesAddon.getAddonFile("CokesRoulette.yml"));
    private static final SettingObject<Integer> period = config.new SettingObject<>("period", 45);

    private static final SettingObject<Boolean> teleport = config.new SettingObject<>("roulette.teleport", true);
    private static final SettingObject<Boolean> rank = config.new SettingObject<>("roulette.show-rank", true);
    private static final SettingObject<Boolean> species = config.new SettingObject<>("roulette.show-species", true);

    public static final List<SettingObject<?>> list = new ArrayList<>();

    private final AbstractGame abstractGame;

    private RouletteTimer timer;

    public Roulette(AbstractGame game) {
        this.abstractGame = game;

        for (Participant participant : game.getParticipants()) {
            if (teleport.getValue()) {
                for (Participant participant2 : game.getParticipants()) {
                    if (!participant.equals(participant2)) {
                        effects.add(new Teleport(participant, participant2));
                    }
                }
            }
            if(species.getValue()) effects.add(new ShowSpecies(participant));
            if(rank.getValue()) effects.add(new ShowRank(participant));
        }
    }

    private boolean start() {
        if (timer == null || !timer.isRunning()) {
            this.timer = new RouletteTimer();
            return timer.start();
        }
        return false;
    }

    private boolean stop() {
        if (timer != null || timer.isRunning()) {
            timer.stop();
            timer = null;
            return true;
        }
        return false;
    }

    private boolean isRunning() {
        return timer != null && timer.isRunning();
    }

    @EventHandler
    public void onGameStart(GameStartEvent e) {
        if (e.getGame().hasModule(this.getClass()) && !isRunning()) {
            Invincibility invincibility = e.getGame().getModule(Invincibility.class);
            if (invincibility != null) {
                invincibility.attachObserver(new Observer() {
                    @Override
                    public void onEnd() {
                        start();
                    }

                    @Override
                    public void onStart() {
                        stop();
                    }
                });
            } else {
                start();
            }
        }
    }

    @EventHandler
    public void onGameEnd(GameEndEvent e) {
        if (e.getGame().hasModule(this.getClass()) && isRunning()) {
            stop();
        }
    }

    private class RouletteTimer {
        private NoticeTimer noticeTimer = new NoticeTimer();
        private PeriodTimer periodTimer = new PeriodTimer();

        private boolean start() {
            return (!noticeTimer.isRunning() || !periodTimer.isRunning()) && periodTimer.start();
        }

        private boolean stop() {
            return noticeTimer.stop(true) || periodTimer.stop(true);
        }

        private boolean isRunning() {
            return noticeTimer.isRunning() || periodTimer.isRunning();
        }

        private class PeriodTimer extends GameTimer {
            public PeriodTimer() {
                abstractGame.super(TaskType.REVERSE, period.getValue());
            }

            public void onEnd() {
                noticeTimer.start();
            }
        }

        private class NoticeTimer extends GameTimer {
            private final ChatColor[] chatColors = {
                ChatColor.YELLOW,
                ChatColor.RED,
                ChatColor.GOLD,
                ChatColor.LIGHT_PURPLE,
                ChatColor.DARK_PURPLE,
                ChatColor.AQUA
            };
            private final Random random = new Random();
            private RouletteEffect effect;

            public NoticeTimer() {
                abstractGame.super(TaskType.REVERSE, 20);
                setPeriod(TimeUnit.TICKS, 10);
            }

            public void run(int arg0) {
                if (arg0 > 5) {
                    effect = random.pick(effects.toArray(new RouletteEffect[] {}));
                    for (Participant participant : abstractGame.getParticipants()) {
                        NMS.sendTitle(participant.getPlayer(), "", random.pick(chatColors)+effect.rouletteName(),0,20,0);
                        SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(participant.getPlayer());
                    }
                }
            }

            public void onEnd() {
                effect.apply();
                periodTimer.start();
            }
        }
    }
}