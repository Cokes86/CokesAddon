package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.effect.list.Dizziness;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

@AbilityManifest(name = "모닝스타", rank = Rank.S, species = Species.OTHERS, explain = {
        "패시브 - 스파이크 어택: 공격 시 $[SPIKE_ATTACK_CHANCE]% 확률로 어지럼증이 $[SPIKE_ATTACK_DIZZINESS_DURATION] 부여됩니다.",
        "철괴 우클릭 - 트리플 브랜디쉬: $[TRIPLE_BRANDISH_DURATION]간 $[TRIPLE_BRANDISH_RANGE]블럭 이내 플레이어에게",
        "  $[TRIPLE_BRANDISH_DAMAGE]의 근거리 대미지와 출혈 $[TRIPLE_BRANDISH_BLEED_DURATION]를 부여합니다. $[TRIPLE_BRANDISH_COOLDOWN]",
        "검 우클릭 - 테이크 다운: 땅과 5블럭 이내 떨어진 상태에서 사용 가능합니다.",
        "  땅으로 내리 꽂으면서 전방 $[TAKE_DOWN_RANGE]블럭 이내 플레이어에게",
        "  $[TAKE_DOWN_DAMAGE]의 근거리 대미지와 기절 $[TAKE_DOWN_STUN_DURATION]를 줍니다. $[TAKE_DOWN_COOLDOWN]",
        "  트리플 브랜디쉬 사용 도중 테이크 다운을 사용할 수 없습니다.",
        "[어지럼증] 구속 1과 함께 적용 도중 받는 대미지가 10% 증가합니다.",
        "[아이디어 제공자 §bmes_seu§f]"
})
/*
[기존 아이디어]
철괴 우클릭(3단 휘두르기):3초간 총 3번 반지름 5만큼 휘두릅니다. (데미지8)(출혈4초)(쿨타임:30초)

칼 우클릭(내려찍기):사거리 6만큼 앞으로 공격합니다.
(출혈3초)(데미지10)(쿨타임9초)

근접 공격(모닝스타):근접 공격마다 30%확률로 어지럼증을 1초 부여합니다.

상태이상(어지럼증):멀미1 및 구속1이 걸리며
모든 데미지에 3의 추가데미지를 받게 됩니다.
 */
@Beta
public class MorningStar extends CokesAbility implements ActiveHandler {
    public static final Config<Double> SPIKE_ATTACK_CHANCE = Config.of(MorningStar.class, "spike-attack-chance", 25.0, FunctionalInterfaces.chance(false, false),
            "스파이크 어택 공격 시 어지럼증을 줄 확률",
            "기본값: 20.0 (%)"
    );
    public static final Config<Integer> SPIKE_ATTACK_DIZZINESS_DURATION = Config.of(MorningStar.class, "spike-attack-dizziness-duration", 1, FunctionalInterfaces.TIME,
            "스파이크 어택 공격 시 어지럼증 시간",
            "기본값: 1 (초)"
    );

    public static final Config<Integer> TRIPLE_BRANDISH_DURATION = Config.of(MorningStar.class, "triple-brandish-duration", 3, FunctionalInterfaces.TIME,
            "트리플 브랜디쉬 지속시간",
            "기본값: 3 (초)"
    );
    public static final Config<Integer> TRIPLE_BRANDISH_BLEED_DURATION = Config.of(MorningStar.class, "triple-brandish-bleed-duration", 2, FunctionalInterfaces.TIME,
            "트리플 브랜디쉬 출혈 지속시간",
            "기본값: 2 (초)"
    );
    public static final Config<Integer> TRIPLE_BRANDISH_RANGE = Config.of(MorningStar.class, "triple-brandish-range", 5, FunctionalInterfaces.positive(),
            "트리플 브랜디쉬 범위",
            "기본값: 5.0"
    );
    public static final Config<Double> TRIPLE_BRANDISH_DAMAGE = Config.of(MorningStar.class, "triple-brandish-damage", 5.0, FunctionalInterfaces.positive(),
            "트리플 브랜디쉬 대미지",
            "기본값: 5.0"
    );
    public static final Config<Integer> TRIPLE_BRANDISH_COOLDOWN = Config.of(MorningStar.class, "triple-brandish-cooldown", 30, FunctionalInterfaces.COOLDOWN,
            "트리플 브랜디쉬 쿨타임",
            "기본값: 30 (초)"
    );

    public static final Config<Integer> TAKE_DOWN_COOLDOWN = Config.of(MorningStar.class, "take-down-cooldown", 30, FunctionalInterfaces.COOLDOWN,
            "테이크 다운 쿨타임",
            "기본값: 10 (초)"
    );
    public static final Config<Double> TAKE_DOWN_DAMAGE = Config.of(MorningStar.class, "take-down-damage", 5d, FunctionalInterfaces.positive(),
            "테이크 다운 대미지",
            "기본값: 5.0"
    );
    public static final Config<Integer> TAKE_DOWN_STUN_DURATION = Config.of(MorningStar.class, "take-down-stun-duration", 2, FunctionalInterfaces.TIME,
            "테이크 다운 스턴 지속시간",
            "기본값: 2 (초)"
    );
    public static final Config<Integer> TAKE_DOWN_RANGE = Config.of(MorningStar.class, "take-down-range", 6, FunctionalInterfaces.positive(),
            "테이크 다운 범위",
            "기본값: 6 (블럭)"
    );

    public MorningStar(Participant arg0) {
        super(arg0);
    }

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
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

    private final Cooldown tripleBrandishCooldown = new Cooldown(TRIPLE_BRANDISH_COOLDOWN.getValue());
    private final Cooldown takeDownCooldown = new Cooldown(TAKE_DOWN_COOLDOWN.getValue());
    private final TripleBrandish tripleBrandish = new TripleBrandish();

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (clickType.equals(ClickType.RIGHT_CLICK)) {
            if (material.equals(Material.IRON_INGOT) && !tripleBrandish.isDuration() && !tripleBrandishCooldown.isCooldown()) {
                return tripleBrandish.start();
            }
            else if (CokesUtil.getSwords().contains(material) && !getPlayer().isOnGround() && !takeDownCooldown.isCooldown() && !tripleBrandish.isDuration()) {
                Location l = getPlayer().getLocation().clone();
                double newY = LocationUtil.getFloorYAt(getPlayer().getWorld(), l.getY(), l.getBlockX(), l.getBlockZ());
                if (l.getY() - newY <= 5) {
                    l.setY(newY);
                    getPlayer().teleport(l);
                    SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation());

                    for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), TAKE_DOWN_RANGE.getValue(), 2, predicate)) {
                        Stun.apply(getGame().getParticipant(player), TimeUnit.SECONDS, TAKE_DOWN_STUN_DURATION.getValue());
                        player.damage(TAKE_DOWN_DAMAGE.getValue(), getPlayer());
                    }
                    return takeDownCooldown.start();
                }
            }
        }
        return false;
    }

    @Override
    public boolean usesMaterial(Material material) {
        return material.equals(Material.IRON_INGOT) || CokesUtil.getSwords().contains(material);
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getDamager() != null && e.getDamager().equals(getPlayer())) {
            Entity entity = e.getEntity();
            if (getGame().isParticipating(entity.getUniqueId())) {
                final Random random = new Random();
                if (random.nextDouble() < SPIKE_ATTACK_CHANCE.getValue() / 100.0) {
                    Dizziness.apply(getGame().getParticipant(e.getEntity().getUniqueId()), TimeUnit.SECONDS, 1);
                }
            }
        }
    }

    public class TakeDownEntity extends AbstractGame.CustomEntity {

        public TakeDownEntity(World world, double x, double y, double z) {
            getGame().super(world, x, y, z);
        }

        @Override
        protected void onRemove() {
            this.remove();
        }

    }

    private class TripleBrandish extends Duration {
        private final Map<Player, Integer> whenHitMap = new HashMap<>();
        private TripleBrandish() {
            super (TRIPLE_BRANDISH_DURATION.getValue() * 20, tripleBrandishCooldown);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onDurationStart() {
            super.onDurationStart();
            whenHitMap.clear();
        }

        @Override
        protected void onDurationEnd() {
            super.onDurationEnd();
        }

        @Override
        protected void onDurationSilentEnd() {
            super.onDurationSilentEnd();
        }

        @Override
        protected void onDurationProcess(int i) {
            for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), TRIPLE_BRANDISH_RANGE.getValue(), 2, predicate)) {
                int hitCount = whenHitMap.getOrDefault(player, TRIPLE_BRANDISH_DURATION.getValue() * 20 + 20);
                if (hitCount - i >= 20) {
                    player.damage(TRIPLE_BRANDISH_DAMAGE.getValue(), getPlayer());
                    Bleed.apply(getGame().getParticipant(player), TimeUnit.SECONDS, TRIPLE_BRANDISH_BLEED_DURATION.getValue());
                    whenHitMap.put(player, i);
                }
            }
        }
    }
}
