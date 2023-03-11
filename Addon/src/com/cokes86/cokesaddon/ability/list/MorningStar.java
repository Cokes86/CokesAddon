package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;
import org.bukkit.Material;

@AbilityManifest(name = "모닝스타", rank = Rank.S, species = Species.OTHERS, explain = {
        "패시브 - 스파이크 어택: 공격 시 $[SPIKE_ATTACK_CHANCE]% 확률로 어지럼증이 $[SPIKE_ATTACK_DIZZINESS_DURATION] 부여됩니다.",
        "철괴 우클릭 - 트리플 브랜디쉬: $[TRIPLE_BRANDISH_DURATION]간 $[TRIPLE_BRANDISH_RANGE]블럭 이내 플레이어에게",
        "  $[TRIPLE_BRANDISH_DAMAGE]의 근거리 대미지를 줍니다. $[TRIPLE_BRANDISH_COOLDOWN]",
        "검 우클릭 - 테이크 다운: 점프한 상태에서 사용 가능합니다.",
        "  땅으로 내리 꽂으면서 전방 $[TAKE_DOWN_RANGE]블럭 이내 플레이어에게",
        "  $[TAKE_DOWN_DAMAGE]의 근거리 대미지와 기절 $[TAKE_DOWN_STUN_DURATION]를 줍니다. $[TAKE_DOWN_COOLDOWN]",
        "[어지럼증] "
})
@Beta
public class MorningStar extends CokesAbility implements ActiveHandler {
    public static final Config<Double> SPIKE_ATTACK_CHANGE = Config.of(MorningStar.class, "spike-attack-chance", 20.0, FunctionalInterfaces.upper(0.0).and(FunctionalInterfaces.lower(100.0)),
            "스파이크 어택 공격 시 어지럼증을 줄 확률",
            "기본값: 20.0 (%)"
    );
    public static final Config<Integer> SPIKE_ATTACK_DIZZINESS_DURATION = Config.of(MorningStar.class, "spike-attack-dizziness-duration", 1, FunctionalInterfaces.TIME,
            "스파이크 어택 공격 시 어지럼증 시간",
            "기본값: 1 (초)"
    );

    public MorningStar(Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        return false;
    }

    @Override
    public boolean usesMaterial(Material material) {
        return material.equals(Material.IRON_INGOT) || CokesUtil.getSwords().contains(material);
    }
}
