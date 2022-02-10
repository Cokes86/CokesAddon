package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
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
        "패시브 - 검은 날개: 상대방을 공격할 시 주는 대미지가 $[DAMAGE]%로 조정됩니다.",
        "  모든 플레이어가 가지고 있는 블랙 패더 카운터 1개 당",
        "  $[DAMAGE_UPGRADE]%p만큼 증가합니다.",
        "공격 시 - 검은 비수: 상대방에게 블랙 패더 카운터 1개를 $[DURATION]동안 남깁니다.",
        "  이는 검은 날개가 적용된 이후 증가됩니다.",
        "플래이어 사망 시 - 검은 잔해: 가지고 있던 모든 블랙 패더 카운터를 자기가 흡수합니다.",
        "  흡수한 블랙 패더 카운터는 영구히 적용됩니다."
})
public class BlackFeather extends CokesAbility {
    private static final Config<Double> DAMAGE = new Config<>(BlackFeather.class, "damage", 80.0, a -> a >= 0,
            "# 검은 날개 주는 대미지 배율",
            "# 기본값: 80.0 (%)");
    private static final Config<Double> DAMAGE_UPGRADE = new Config<>(BlackFeather.class, "damage-upgrade", 1.5, a -> a >= 0,
            "# 검은 날개 카운터 당 추가 주는 대미지 배율",
            "# 기본값: 1.5 (%p)");
    private static final Config<Integer> DURATION = new Config<>(BlackFeather.class, "duration", 15, a -> a >= 0,
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
