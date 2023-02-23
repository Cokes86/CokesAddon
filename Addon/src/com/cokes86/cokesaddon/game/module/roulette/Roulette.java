package com.cokes86.cokesaddon.game.module.roulette;

import java.util.ArrayList;
import java.util.List;

import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameStartEvent;
import daybreak.abilitywar.game.event.InvincibilityStatusChangeEvent;
import daybreak.abilitywar.game.module.Invincibility;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.game.module.Invincibility.Observer;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@ModuleBase(Roulette.class)
public class Roulette implements ListenerModule {
    private final List<Pair<RouletteEffect, Integer>> effects = new ArrayList<>();
    private int maxPriority = 0;
    public static final RouletteConfig config = new RouletteConfig(CokesAddon.getAddonFile("CokesRoulette.yml"));
    private final List<Participant> rouletteTarget;

    private final AbstractGame abstractGame;

    private RouletteTimer timer;

    public Roulette(AbstractGame game) {
        this.abstractGame = game;
        config.update();

        rouletteTarget = new ArrayList<>(game.getParticipants());

        for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer> > effect : RouletteRegister.getMapPairs()) {
            if (effect.getRight().getValue() > 0) {
                try {
                    int tmp = maxPriority + effect.getRight().getValue();
                    effects.add(Pair.of(effect.getLeft().getConstructor().newInstance(), tmp));
                    maxPriority = tmp;
                } catch (Exception e) {
                    System.out.println("[CokesAddon] 룰렛 모듈 등록 중 오류가 발생했습니다. : "+ effect.getLeft());
                }
            }
        }
    }

    private boolean start() {
        if (timer == null || !timer.isRunning()) {
            timer = new RouletteTimer();
            Bukkit.broadcastMessage("§a룰렛 게임 모듈이 활성화되었습니다.");
            return timer.start();
        }
        return false;
    }

    private boolean stop() {
        if (timer != null && timer.isRunning()) {
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
        start();
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
            }
        }
    }

    @EventHandler
    public void onInvincibilityStatusChange(InvincibilityStatusChangeEvent e) {
        if (e.getGame().hasModule(this.getClass())) {
            if (e.getNewStatus()) stop();
            else start();
        }
    }

    @EventHandler
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (e.getParticipant().getGame().hasModule(this.getClass())) {
            if (e.getParticipant().getGame() instanceof DeathManager.Handler) {
                rouletteTarget.remove(e.getParticipant());
            }
        }
    }

    private class RouletteTimer {
        private final NoticeTimer noticeTimer = new NoticeTimer();
        private final PeriodTimer periodTimer = new PeriodTimer();

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
                abstractGame.super(TaskType.REVERSE, RouletteRegister.getRoulletPeriod());
            }

            public void onEnd() {
                if (GameManager.getGame().isGameStarted()) {
                    Invincibility invincibility = ((Invincibility.Handler) GameManager.getGame()).getInvincibility();
                    if (invincibility.isEnabled()) {
                        periodTimer.start();
                    }
                }
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
            private Participant target;

            public NoticeTimer() {
                abstractGame.super(TaskType.REVERSE, 20);
                setPeriod(TimeUnit.TICKS, 2);
            }

            public void run(int arg0) {
                if (arg0 > 5) {
                    target = random.pick(rouletteTarget);
                    int a = random.nextInt(maxPriority);
                    for (int i = 0 ; i < effects.size(); i++) {
                        int min = effects.get(i).getRight();
                        int max = i == effects.size()-1 ? maxPriority : effects.get(i+1).getRight();
                        if (a >= min && a < max) {
                            effect = effects.get(i).getLeft();
                            break;
                        }
                    }
                    for (Participant participant : getGame().getParticipants()) {
                        NMS.sendTitle(participant.getPlayer(), random.pick(chatColors) + target.getPlayer().getName(), random.pick(chatColors) + RouletteRegister.getEffectName(effect.getClass()),0,20,0);
                        SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(participant.getPlayer());
                    }
                }
            }

            public void onEnd() {
                if (effect.apply(target)) {
                    periodTimer.start();
                    for (Participant participant : abstractGame.getParticipants()) {
                        NMS.sendTitle(participant.getPlayer(), "", "",0,20,0);
                    }
                }
            }
        }
    }
}