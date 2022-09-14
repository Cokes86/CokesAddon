package com.cokes86.cokesaddon.util;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

public class InvincibilityTimer extends AbilityTimer implements Listener {
    private final ActionbarChannel channel;
    private final Participant participant;
    private final boolean attackable;

    /**
     * 능력자의 무적을 설정합니다.
     * @param participant 능력자
     * @param time 시간
     * @param attackable 무적 중 공격 가능 여부
     */
    public InvincibilityTimer(Participant participant, int time, boolean attackable) {
        Objects.requireNonNull(participant.getAbility()).super(time);
        this.channel = participant.actionbar().newChannel();
        this.participant = participant;
        this.attackable = attackable;
    }

    public InvincibilityTimer(Participant participant, int time) {
        this(participant, time, true);
    }

    public InvincibilityTimer(Participant participant) {
        Objects.requireNonNull(participant.getAbility()).super();
        this.channel = participant.actionbar().newChannel();
        this.participant = participant;
        this.attackable = true;
    }

    @Override
    protected void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        onInvincibilityStart();
    }

    @Override
    protected void run(int count) {
        String attack = attackable ? "" : "/공격불능";
        channel.update(String.format("§e무적%s§f: %s", attack, TimeUtil.parseTimeAsString(getFixedCount())));
        if (count == (getMaximumCount()/2) || (getFixedCount() <= 5 && getFixedCount() >= 1)) {
            participant.getPlayer().sendMessage(String.format("§e무적%s§f: %s", attack, TimeUtil.parseTimeAsString(getFixedCount())));
            SoundLib.BLOCK_NOTE_BLOCK_HARP.playSound(participant.getPlayer());
        }
        onInvincibilityRun(count);
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        onInvincibilityEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        onInvincibilitySilentEnd();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = CokesUtil.getDamager(e.getDamager());
        if (damager.equals(participant.getPlayer()) && !attackable) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(participant.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void onInvincibilityStart() {}
    public void onInvincibilityRun(int count) {}
    public void onInvincibilityEnd() {}
    public void onInvincibilitySilentEnd() {}
}
