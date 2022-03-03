package cokes86.addon.synergy.list;

import cokes86.addon.synergy.CokesSynergy;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "중력화살", rank = Rank.A, species = Species.OTHERS, explain = {
        "활을 쐈을 때, 화살이 맞은 위치에서 $[RANGE]칸 범위 내에 있는 생명체들에게",
        "최대 체력의 $[DAMAGE]%의 대미지를 주고 공중에 $[BLOCK]블럭 위로 옮깁니다. $[COOLDOWN]"
})
public class GravityArrow extends CokesSynergy {
    private static final Config<Integer> RANGE = new Config<>(GravityArrow.class, "range", 5, PredicateUnit.positive(),
            "# 범위", "# 기본값: 5 (블럭)");
    private static final Config<Double> DAMAGE = new Config<>(GravityArrow.class, "damage", 15.0, PredicateUnit.between(0.0,100.0,false),
            "# 최대 체력 비례 대미지", "# 기본값: 15.0 (%)");
    private static final Config<Integer> BLOCK = new Config<>(GravityArrow.class, "block", 5, PredicateUnit.positive(),
            "# 위로 올라갈 정도", "# 기본값: 5 (블럭)");
    private static final Config<Integer> COOLDOWN = new Config<>(GravityArrow.class, "cooldown", 8, PredicateUnit.positive(),
            "# 쿨타임", "# 기본값: 8 (초)");

    private final Predicate<Entity> ONLY_PARTICIPANTS = entity -> !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
            && (!(getGame() instanceof DeathManager.Handler) || !((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
            && getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());

    private final Cooldown cooldownTimer = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
    private final Circle circle = Circle.of(RANGE.getValue(), 14 * RANGE.getValue());

    public GravityArrow(Participant participant) {
        super(participant);
    }

    @SubscribeEvent
    public void onProjectileHit(ProjectileHitEvent e) {
        if (NMS.isArrow(e.getEntity())) {
            if (getPlayer().equals(e.getEntity().getShooter())) {
                if (!cooldownTimer.isCooldown()) {
                    SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
                    final Location center = e.getEntity().getLocation();
                    final List<Damageable> damageables = LocationUtil.getNearbyEntities(Damageable.class, center, 5, 5, ONLY_PARTICIPANTS);
                    for (Damageable damageable : damageables) {
                        if (!damageable.equals(getPlayer())) {
                            if (LocationUtil.isInCircle(center, damageable.getLocation(), 5)) {
                                damageable.damage((damageable instanceof Attributable ? ((Attributable) damageable).getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 0) * (DAMAGE.getValue()/100.0), getPlayer());
                                if (damageable instanceof Player) {
                                    SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound((Player) damageable);
                                }
                            }
                        }
                    }

                    for (Damageable damageable : damageables) {
                        Location teleport = damageable.getLocation().clone().add(0, BLOCK.getValue(), 0);
                        damageable.teleport(teleport);
                    }
                    cooldownTimer.start();
                }
            }
        }
    }
}
