package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@AbilityManifest(name = "블랙 패더", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS, explain = {
        "§7공격 시 §8- §c검은 날개§f: 상대방을 공격할 시 주는 대미지가 ($[DAMAGE] + 게임 내 존재하는 §7블랙 패더 카운터§f * $[DAMAGE_UPGRADE])%로 변경됩니다.",
        "  이후 상대방에게 §7블랙 패더 카운터§f를 1 상승시킵니다.",
        "  이는 $[DURATION] 유지됩니다.",
        "§7플래이어 사망 시 §8- §c검은 잔해§f: 가지고 있던 §7블랙 패더 카운터§f를 전부 제거하고",
        "  그 제거한 수치만큼 자신에게 §7블랙 패더 카운터§f를 상승시킵니다.",
        "  이는 영구히 유지됩니다."
})
public class BlackFeather extends CokesAbility {
    private static final Config<Double> DAMAGE = new Config<>(BlackFeather.class, "damage", 80.0, PredicateUnit.positive(),
            "# 검은 날개 주는 대미지 변경값",
            "# 기본값: 80.0 (%)");
    private static final Config<Double> DAMAGE_UPGRADE = new Config<>(BlackFeather.class, "damage-upgrade", 5.0, PredicateUnit.positive(),
            "# 검은 날개 카운터 당 추가되는 주는 대미지 변경값",
            "# 기본값: 5.0 (%p)");
    private static final Config<Integer> DURATION = new Config<>(BlackFeather.class, "duration", 15, Config.Condition.TIME,
            "# 검은 비수 카운터 유지 시간",
            "# 기본값: 15 (초)");

    private final List<BlackFeatherCounter> counterList = new ArrayList<>();

    public BlackFeather(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Entity) {
                damager = (Entity) projectile.getShooter();
            }
        }

        if (damager.equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            int counter = counterList.size();

            e.setDamage(e.getDamage() * (DAMAGE.getValue()/100.0 + counter* DAMAGE_UPGRADE.getValue()));
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
        }

        public BlackFeatherCounter(AbstractGame.Participant participant) {
            super(DURATION.getValue());
            this.participant = participant;
            counterList.add(this);
            start();
        }

        @Override
        protected void onEnd() {
            counterList.remove(this);
        }

        public AbstractGame.Participant getParticipant() {
            return participant;
        }
    }
}
