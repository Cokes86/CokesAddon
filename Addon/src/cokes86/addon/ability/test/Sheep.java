package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "양", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.ANIMAL, explain = {
        "불대미지를 제외한 모든 대미지가 $[DAMAGE_REDUCE]% 감소합니다.",
        "불대미지는 1 상승합니다."
})
public class Sheep extends CokesAbility {
    private static final Config<Double> DAMAGE_REDUCE = new Config<>(Sheep.class, "damage_reduce", 20.0, PredicateUnit.between(0.0, 100.0, false),
            "# 불대미지를 제외한 모든 대미지 감소량",
            " # 기본값: 20.0 (%)");

    public Sheep(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            e.setDamage(e.getDamage() + 1);
        } else {
            e.setDamage(e.getDamage() * (1 - DAMAGE_REDUCE.getValue()/100.0));
        }
    }
}
