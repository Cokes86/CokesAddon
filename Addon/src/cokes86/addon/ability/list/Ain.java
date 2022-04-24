package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

@AbilityManifest(name = "아인", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c능력 망각§f: 주변 $[RANGE]블럭 이내 능력자가 쿨타임이 작동중일 때,",
        "  그 쿨타임을 $[INCREASE] 증가시킵키다. $[COOL]"
})
public class Ain extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RANGE = Config.of(Ain.class, "range", 5, FunctionalInterfaceUnit.positive(),
            "# 능력 망각 범위", "# 기본값: 5 (블럭)");
    private static final Config<Integer> INCREASE = Config.of(Ain.class, "increase", 30, Config.Condition.TIME,
            "# 능력 망각 상대방 쿨타임 증가량", "# 기본값: 30 (초)");
    private static final Config<Integer> COOL = Config.of(Ain.class, "cooldown", 60, Config.Condition.COOLDOWN,
            "# 능력 망각 쿨타임", "# 기본값: 60 (초)");

    private final Cooldown cooldown = new Cooldown(COOL.getValue());

    public Ain(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT) {
            if (clickType == ClickType.RIGHT_CLICK) {
                boolean used = false;
                List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation().clone(), RANGE.getValue(), RANGE.getValue(), player -> !player.equals(getPlayer()));
                for (Player near : nearby) {
                    if (!getGame().isParticipating(near)) continue;
                    AbstractGame.Participant participant = getGame().getParticipant(near);
                    if (!participant.attributes().TARGETABLE.getValue()) continue;
                    if (participant.getAbility() == null) continue;
                    for (SimpleTimer timer : participant.getAbility().getRunningTimers()) {
                        if (timer instanceof Cooldown.CooldownTimer) {
                            timer.setCount(timer.getCount() + INCREASE.getValue());
                            used = true;
                        }
                    }
                }
                return used && cooldown.start();
            }
        }
        return false;
    }
}
