package com.cokes86.cokesaddon.ability.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;

@AbilityManifest(name = "블랙 패더", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS, explain = {
        "§7공격 시 §8- §c검은 날개§f: 상대방을 공격할 시 주는 대미지가 ($[DAMAGE] + 게임 내 존재하는 §7블랙 패더 카운터§f × $[DAMAGE_UPGRADE])%로 변경됩니다.",
        "  이후 상대방에게 §7블랙 패더 카운터§f를 1 상승시킵니다.",
        "  이는 $[DURATION]간 유지됩니다.",
        "§7타 플레이어 사망 시 §8- §c검은 잔해§f: 가지고 있던 §7블랙 패더 카운터§f를 전부 제거하고",
        "  그 제거한 수치만큼 자신에게 §7블랙 패더 카운터§f를 상승시킵니다.",
        "  이는 영구히 유지됩니다."
})
public class BlackFeather extends CokesAbility {
    private static final Config<Double> DAMAGE = Config.of(BlackFeather.class, "damage", 100.0, FunctionalInterfaces.positive(),
            "# 검은 날개 주는 대미지 변경값",
            "# 기본값: 100.0 (%)");
    private static final Config<Double> DAMAGE_UPGRADE = Config.of(BlackFeather.class, "damage-upgrade", 2.5, FunctionalInterfaces.positive(),
            "# 블랙 패더 카운터 당 추가되는 주는 대미지 변경값",
            "# 기본값: 2.5 (%p)");
    private static final Config<Integer> DURATION =Config.of(BlackFeather.class, "duration", 15, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 블랙 패더 카운터 유지 시간",
            "# 기본값: 15 (초)");

    private final List<BlackFeatherCounter> counterList = new ArrayList<>();
    private final ActionbarChannel channel = newActionbarChannel();

    public BlackFeather(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            channel.update("§7"+ counterList.size());
        }
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity) {
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager != null && damager.equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            int counter = counterList.size();
            double damage1 = DAMAGE.getValue() / 100.0;
            double damage2 = counter * DAMAGE_UPGRADE.getValue() / 100.0;
            e.setDamage(e.getDamage() * (damage1 + damage2));
            AbstractGame.Participant target = getGame().getParticipant(e.getEntity().getUniqueId());
            counterList.add(new BlackFeatherCounter(target));
        }
    }

    @SubscribeEvent
    public void onParticipantDeath(@NotNull ParticipantDeathEvent e) {
        if (e.getParticipant().equals(getParticipant())) return;

        int addCount = 0;

        for (Iterator<BlackFeatherCounter> it = counterList.iterator(); it.hasNext();) {
            BlackFeatherCounter counter = it.next();
            if (counter.getParticipant().equals(e.getParticipant())) {
                // 1) 이 counter는 리스트에서 iterator가 제거 담당
                counter.detachFromList(); // onEnd/onSilentEnd에서 remove 안 하게
                counter.stop(true);       // 타이머만 종료
                it.remove();              // 실제 리스트 제거는 iterator로만

                addCount++;
            }
        }

        // 2) 추가는 루프 밖에서
        for (int i = 0; i < addCount; i++) {
            counterList.add(new BlackFeatherCounter()); // 내 영구 카운터
        }

        channel.update("§7" + counterList.size());
    }


    public class BlackFeatherCounter extends AbilityTimer {
        private final AbstractGame.Participant participant;
        private boolean autoRemoveFromList = true;

        public void detachFromList() {
            this.autoRemoveFromList = false;
        }

        public BlackFeatherCounter() {
            super();
            this.participant = BlackFeather.this.getParticipant();
            start();
            BlackFeather.this.channel.update("§7" + counterList.size());
        }

        public BlackFeatherCounter(AbstractGame.Participant participant) {
            super(DURATION.getValue());
            this.participant = participant;
            start();
            BlackFeather.this.channel.update("§7" + counterList.size());
        }

        @Override
        protected void onEnd() {
            if (autoRemoveFromList) {
                counterList.remove(this);
            }
            BlackFeather.this.channel.update("§7" + counterList.size());
        }

        @Override
        protected void onSilentEnd() {
            if (autoRemoveFromList) {
                counterList.remove(this);
            }
            BlackFeather.this.channel.update("§7" + counterList.size());
        }

        public AbstractGame.Participant getParticipant() {
            return participant;
        }
    }

}
