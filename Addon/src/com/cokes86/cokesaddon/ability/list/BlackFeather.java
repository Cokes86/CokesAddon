package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@AbilityManifest(name = "블랙 패더", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS, explain = {
        "§7공격 시 §8- §c검은 날개§f: 상대방을 공격할 시 주는 대미지가 ($[DAMAGE] + 게임 내 존재하는 §7블랙 패더 카운터§f × $[DAMAGE_UPGRADE])%로 변경됩니다.",
        "  이후 상대방에게 §7블랙 패더 카운터§f를 1 상승시킵니다.",
        "  이는 $[DURATION]간 유지됩니다.",
        "§7타 플레이어 사망 시 §8- §c검은 잔해§f: 가지고 있던 §7블랙 패더 카운터§f를 전부 제거하고",
        "  그 제거한 수치만큼 자신에게 §7블랙 패더 카운터§f를 상승시킵니다.",
        "  이는 영구히 유지됩니다."
})
public class BlackFeather extends CokesAbility {
    private static final Config<Double> DAMAGE = Config.of(BlackFeather.class, "damage", 75.0, FunctionalInterfaces.positive(),
            "# 검은 날개 주는 대미지 변경값",
            "# 기본값: 75.0 (%)");
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
            new BlackFeatherCounter(target);
        }
    }

    @SubscribeEvent
    public void onParticipantDeath(@NotNull ParticipantDeathEvent e) {
        if (!e.getParticipant().equals(getParticipant())) {
            Iterator<BlackFeatherCounter> iterator = counterList.iterator();
            while(iterator.hasNext()) {
                BlackFeatherCounter counter = iterator.next();
                if (counter.getParticipant().equals(e.getParticipant())) {
                    counter.stop(true);
                    iterator.remove();
                    new BlackFeatherCounter();
                }
            }
        }
    }

    public class BlackFeatherCounter extends AbilityTimer {
        private final AbstractGame.Participant participant;

        public BlackFeatherCounter() {
            super();
            this.participant = BlackFeather.this.getParticipant();
            counterList.add(this);
            start();
            BlackFeather.this.channel.update("§7"+ counterList.size());
        }

        public BlackFeatherCounter(AbstractGame.Participant participant) {
            super(DURATION.getValue());
            this.participant = participant;
            counterList.add(this);
            start();
            BlackFeather.this.channel.update("§7"+ counterList.size());
        }

        @Override
        protected void onEnd() {
            counterList.remove(this);
            BlackFeather.this.channel.update("§7"+ counterList.size());
        }

        @Override
        protected void onSilentEnd() {
            counterList.remove(this);
            BlackFeather.this.channel.update("§7"+ counterList.size());
        }

        public AbstractGame.Participant getParticipant() {
            return participant;
        }
    }
}
