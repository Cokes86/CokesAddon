package com.cokes86.cokesaddon.util.timer;

import com.cokes86.cokesaddon.util.CokesUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class InvincibilityTimer extends NoticeTimeTimer implements Listener {
    private final Participant participant;
    private final boolean attackable;

    /**
     * 능력자의 무적을 설정합니다.
     * @param participant 능력자
     * @param time 시간
     * @param attackable 무적 중 공격 가능 여부
     */
    public InvincibilityTimer(Participant participant, int time, boolean attackable) {
        super(participant, String.format("§e무적%s", (attackable ? "" : "/공격불가")), time);
        this.participant = participant;
        this.attackable = attackable;
    }

    public InvincibilityTimer(Participant participant, int time) {
        this(participant, time, true);
    }

    @Override
    protected void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        onInvincibilityStart();
    }

    @Override
    protected void run(int count) {
        super.run(count);
        if (count == (getMaximumCount()/2) || (getFixedCount() <= 5 && getFixedCount() >= 1)) {
            participant.getPlayer().sendMessage(String.format("§e무적%s§f: %s", (attackable ? "" : "/공격불가"), TimeUtil.parseTimeAsString(getFixedCount())));
            SoundLib.BLOCK_NOTE_BLOCK_HARP.playSound(participant.getPlayer());
        }
        onInvincibilityRun(count);
    }

    @Override
    protected void onEnd() {
        super.onEnd();
        HandlerList.unregisterAll(this);
        onInvincibilityEnd();
    }

    @Override
    protected void onSilentEnd() {
        super.onSilentEnd();
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
