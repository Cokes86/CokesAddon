package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import com.google.common.collect.ImmutableList;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "양", rank = Rank.A, species = Species.ANIMAL, explain = {
        "불, 용암, 마그마블럭으로 인해 받는 대미지가 $[DAMAGE]% 증가합니다.",
        "이를 제외한 모든 대미지가 $[DAMAGE_REDUCE]% 감소합니다.",
        "$[RANGE]블럭 이내 플레이어가 존재할 시, 그 중 가까운 플레이어를 바라보게 시선이 변경됩니다."
})
public class Sheep extends CokesAbility {
    private static final Config<Double> DAMAGE = new Config<>(Sheep.class, "damage", 150.0, PredicateUnit.positive(),
            "# 불, 용암, 마그마블럭으로 인해 받는 대미지 감소량",
            " # 기본값: 150.0 (%)");
    private static final Config<Double> DAMAGE_REDUCE = new Config<>(Sheep.class, "damage_reduce", 15.0, PredicateUnit.between(0.0, 100.0, false),
            "# 이를 제외한 모든 대미지 감소량",
            " # 기본값: 15.0 (%)");

    private static final Config<Integer> RANGE = new Config<>(Sheep.class, "range", 5, PredicateUnit.positive(),
            "# 시선이 변경될 플레이어 거리 최댓값",
            " # 기본값: 5 (블럭)");

    private final List<DamageCause> damageCauseList = ImmutableList.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.HOT_FLOOR);

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            Participant target = getGame().getParticipant(entity.getUniqueId());
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
            }
            if (getGame() instanceof Teamable) {
                Teamable game = (Teamable) getGame();
                return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
            }
            return target.attributes().TARGETABLE.getValue();
        }
        return true;
    };

    public Sheep(AbstractGame.Participant arg0) {
        super(arg0);
    }

    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        protected void run(int count) {
            Player nearest = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation().clone(), predicate);
            if (nearest != null && getPlayer().getLocation().toVector().subtract(nearest.getLocation().toVector()).length() <= RANGE.getValue()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Vector direction = nearest.getEyeLocation().toVector().subtract(getPlayer().getEyeLocation().toVector());
                    NMS.rotateHead(player, getPlayer(), LocationUtil.getYaw(direction), LocationUtil.getPitch(direction));
                }
            }
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (damageCauseList.contains(e.getCause())) {
            e.setDamage(e.getDamage() * (1 + DAMAGE.getValue() / 100.0));
        } else {
            e.setDamage(e.getDamage() * (1 - DAMAGE_REDUCE.getValue() / 100.0));
        }
    }
}
