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
    "§7패시브 §8- §c테이킹§f: 자신이 최근에 공격받은 대미지의 종류에 따라",
    "  §b인핸스§f 스킬의 효과가 뒤바뀝니다.",
    "§7철괴 우클릭 §8- §b인핸스§f: 바라보고 있는 플레이어에게",
    "  대미지를 주고 효과를 부여합니다. $[COOLDOWN]",
    "  [§a근거리§f] $[MELEE_DAMAGE]의 근거리 대미지, 속박 $[ROOTED_DURATION]초 부여",
    "  [§e원거리§f] $[DISTANCE_DAMAGE]의 원거리 대미지, 대상 주변 $[AOE_RANGE]블럭 이내 플레이어에게",
    "    $[AOE_DAMAGE]의 원거리 대미지를 부여",
    "  [§b마법§f] $[MAGIC_DAMAGE]의 마법 대미지, 랜덤한 나쁜 포션 효과 1단계 $[POTION_DURATION]간 부여.",
    "    §7※나쁜 포션 효과: 독, 구속, 나약함, 실명",
    "  [§7이외§f] $[OTHER_DAMAGE]의 근거리 대미지. 특수 효과 없음."
})
public class EnergyTaker extends CokesAbility implements TargetHandler {
    private Measurement measurement = Measurement.OTHER;
    private final ActionbarChannel channel = newActionbarChannel();

    private final Config<Integer> COOLDOWN = Config.of(EnergyTaker.class, "cooldown", 30, FunctionalInterfaces.COOLDOWN,
    "# 인핸스 쿨타임",
    "# 기본값: 30(초)");

    private final Config<Double> MELEE_DAMAGE = Config.of(EnergyTaker.class, "melee-damage", 5.0, FunctionalInterfaces.positive(),
    "# 인핸스 중 근거리일 시 대미지",
    "# 기본값: 5.0");
    private final Config<Double> DISTANCE_DAMAGE = Config.of(EnergyTaker.class, "distance-damage", 7.0, FunctionalInterfaces.positive(),
    "# 인핸스 중 원거리일 시 대미지",
    "# 기본값: 7.0");
    private final Config<Double> MAGIC_DAMAGE = Config.of(EnergyTaker.class, "magic-damage", 3.0, FunctionalInterfaces.positive(),
    "# 인핸스 중 마법일 시 대미지",
    "# 기본값: 3.0");
    private final Config<Double> OTHER_DAMAGE = Config.of(EnergyTaker.class, "other-damage", 6.0, FunctionalInterfaces.positive(),
    "# 인핸스 중 이외일 시 대미지",
    "# 기본값: 6.0");

    private final Config<Double> ROOTED_DURATION = Config.of(EnergyTaker.class, "rooted-duration", 2.5, FunctionalInterfaces.lessThanOrEqual(5.0).and(FunctionalInterfaces.positive()),
    "# 인핸스 중 근거리 시 속박 지속시간",
    "# 기본값: 2.5 (초)");


    private final Config<Integer> AOE_RANGE = Config.of(EnergyTaker.class, "aoe-range", 5, FunctionalInterfaces.positive(),
    "# 인핸스 중 원거리일 시 주변 플레이어에게 줄 대미지",
    "# 기본값: 5");
    private final Config<Double> AOE_DAMAGE = Config.of(EnergyTaker.class, "aoe-damage", 3.5, FunctionalInterfaces.positive(),
    "# 인핸스 중 원거리일 시 주변 플레이어에게 줄 대미지",
    "# 기본값: 3.5");

    private final Config<Integer> POTION_DURATION = Config.of(EnergyTaker.class, "potion-duration", 5, FunctionalInterfaces.TIME,
    "# 인핸스 중 마법일 시 포션 효과 지속시간",
    "# 기본값: 5 (초)");

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

    public EnergyTaker(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().equals(getPlayer())) {
            switch (e.getCause()) {
                case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK: {
                    measurement = Measurement.MELEE;
                    channel.update("상태: §a근거리");
                    break;
                }
                case PROJECTILE: {
                    measurement = Measurement.DISTANCE;
                    channel.update("상태: §e원거리");
                    break;
                }
                case POISON: case WITHER: case MAGIC: {
                    measurement = Measurement.MAGIC;
                    channel.update("상태: §b마법");
                    break;
                }
                default: {
                    measurement = Measurement.OTHER;
                    channel.update("상태: §7이외");
                }
            }
        }
    }

    @Override
    public void TargetSkill(Material arg0, LivingEntity arg1) {
        if (arg0.equals(Material.IRON_INGOT) && getGame().isParticipating(arg1.getUniqueId()) && !cooldown.isCooldown()) {
            switch (measurement) {
                case MELEE: {
                    arg1.damage(MELEE_DAMAGE.getValue());
                    Rooted.apply(getGame().getParticipant(arg1.getUniqueId()), TimeUnit.TICKS, (int) (ROOTED_DURATION.getValue()*20));
                    break;
                }
                case DISTANCE: {
                    Damages.damageArrow(arg1, getPlayer(), DISTANCE_DAMAGE.getValue().floatValue());
                    Predicate<Entity> predicate = entity -> {
                        if (entity == null || entity.equals(getPlayer()) || entity.equals(arg1)) return false;
                        if (entity instanceof Player) {
                            if (!getGame().isParticipating(entity.getUniqueId())) return false;
                            if (getGame() instanceof DeathManager.Handler) {
                                DeathManager.Handler game = (DeathManager.Handler) getGame();
                                return !game.getDeathManager().isExcluded(entity.getUniqueId());
                            }
                        }
                        return true;
                    };
                    for (Player player : LocationUtil.getNearbyEntities(Player.class, arg1.getLocation(), AOE_RANGE.getValue(), AOE_RANGE.getValue(), predicate)) {
                        Damages.damageArrow(player, getPlayer(), AOE_DAMAGE.getValue().floatValue());
                    }
                    break;
                }
                case MAGIC: {
                    Damages.damageMagic(arg1, getPlayer(), false, MAGIC_DAMAGE.getValue().floatValue());
                    List<PotionEffects> effect = ImmutableList.of(PotionEffects.POISON, PotionEffects.WEAKNESS, PotionEffects.SLOW, PotionEffects.BLINDNESS);
                    new Random().pick(effect).addPotionEffect(arg1, POTION_DURATION.getValue()*20, 0, false);
                    break;
                }
                case OTHER: {
                    arg1.damage(OTHER_DAMAGE.getValue());
                    break;
                }
            }
            cooldown.start();
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            switch (measurement) {
                case MELEE: {
                    channel.update("상태: §a근거리");
                    break;
                }
                case DISTANCE: {
                    channel.update("상태: §e원거리");
                    break;
                }
                case MAGIC: {
                    channel.update("상태: §b마법");
                    break;
                }
                default: {
                    channel.update("상태: §7이외");
                }
            }
        } else {
            channel.update(null);
        }
    }

    private enum Measurement {
        MELEE, DISTANCE, MAGIC, OTHER
    }
}
