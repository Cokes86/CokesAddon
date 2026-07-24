package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

@AbilityManifest(name = "에너지 테이커", rank = Rank.A, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c테이킹§f: 받은 피해를 종류에 따라 저장합니다.",
        "  근거리 피해는 §a근거리 스택§f, 투사체 피해는 §e원거리 스택§f,",
        "  마법·독·위더·화염 등의 특수 피해는 §b마법 스택§f으로 저장됩니다.",
        "  각 스택은 받은 피해 2당 1씩 저장되며 최대 $[MAX_STACK]까지 저장됩니다.",
        "  비전투상태에서는 $[PERIOD]마다 모든 스택이 1씩 감소합니다.",
        "§7철괴 우클릭 §8- §b인핸스§f: §c테이킹§f으로 받은 모든 스택을 소모하여",
        "  $[DURATION] 동안 공격을 강화합니다. $[COOLDOWN]",
        "  [검/도끼] §a근거리 스택§f/2 + §b마법 스택§f/2",
        "  [활] §e원거리 스택§f/2 + §b마법 스택§f/2",
})
@Beta
public class EnergyTaker extends CokesAbility {
    private final ActionbarChannel channel = newActionbarChannel();

    private int melee = 0;
    private int distance = 0;
    private int magic = 0;

    private final Config<Integer> COOLDOWN = Config.of(EnergyTaker.class, "cooldown", 30, FunctionalInterfaces.COOLDOWN,
    "# 인핸스 쿨타임",
    "# 기본값: 30(초)");

    private final Config<Integer> DURATION = Config.of(EnergyTaker.class, "duration", 15, FunctionalInterfaces.TIME,
            "# 인핸스 지속시간",
            "# 기본값: 15(초)");

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

    public EnergyTaker(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().equals(getPlayer())) {
            switch (e.getCause()) {
                case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK: {
                    channel.update("상태: §a근거리");
                    break;
                }
                case PROJECTILE: {
                    channel.update("상태: §e원거리");
                    break;
                }
                case POISON: case WITHER: case MAGIC: {
                    channel.update("상태: §b마법");
                    break;
                }
                default: {
                    channel.update("상태: §7이외");
                }
            }
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {


        } else {
            channel.update(null);
        }
    }
}
