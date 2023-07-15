package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import org.bukkit.entity.Player;

import java.util.List;

@AbilityManifest(name = "아인", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c망각 지대§f: $[PERIOD]마다 주변 $[RANGE]블럭 이내 능력자가 쿨타임이 작동중일 때,",
        "  그 쿨타임을 $[INCREASE] 증가시킵키다."
}, summarize = {
        "자신 주변 능력자의 §c쿨타임§f을 §e느리게§f 흐르게 합니다."
})
public class Ain extends CokesAbility {
    private static final Config<Integer> RANGE = Config.of(Ain.class, "range", 5, FunctionalInterfaces.positive(),
            "# 망각 지대 범위", "# 기본값: 5 (블럭)");
    private static final Config<Integer> INCREASE = Config.of(Ain.class, "increase", 1, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 망각 지대 상대방 쿨타임 증가량", "# 기본값: 1 (초)");
    private static final Config<Integer> PERIOD = Config.of(Ain.class, "period", 3, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 망각 지대 주기", "# 기본값: 3 (초)");

    private final PlaceOfOblivion passive = new PlaceOfOblivion();

    public Ain(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        } else {
            passive.stop(true);
        }
    }

    public class PlaceOfOblivion extends AbilityTimer {
        public PlaceOfOblivion() {
            super();
            setInitialDelay(TimeUnit.SECONDS, PERIOD.getValue());
            setPeriod(TimeUnit.SECONDS, PERIOD.getValue());
        }

        public void run(int a) {
            List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation().clone(), RANGE.getValue(), RANGE.getValue(), player -> !player.equals(getPlayer()));
            for (Player near : nearby) {
                if (!getGame().isParticipating(near)) continue;
                AbstractGame.Participant participant = getGame().getParticipant(near);
                if (!participant.attributes().TARGETABLE.getValue()) continue;
                if (participant.getAbility() == null) continue;
                for (SimpleTimer timer : participant.getAbility().getRunningTimers()) {
                    if (timer instanceof Cooldown.CooldownTimer) {
                        timer.setCount(timer.getCount() + INCREASE.getValue());
                    }
                }
            }
        }
    }
}
