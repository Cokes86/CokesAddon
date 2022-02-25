package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.CokesAbility.Config.Condition;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "휘트니", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭시 $[DURATION]간 유지되고 최대 6번 중첩가능한 버프를 부여합니다. $[COOLDOWN_ONE]",
        "중첩사용 시 기존 효과와 더불어 새로운 효과를 부여받습니다.",
        "지속시간이 끝나거나 6번 중첩 후 쿨타임은 $[COOLDOWN_TWO]로 적용합니다.",
        "* 1회 사용 시: 신속 부여",
        "* 2회 사용 시: 상태 이상 적용 시 지속 시간 1초 감소",
        "* 3회 사용 시: 상대 공격 시 출혈 2초 부여",
        "* 4회 사용 시: 상대 공격 시 주었던 최종 대미지의 25% 회복.",
        "* 5회 사용 시: 상대에게 주는 대미지 $[FIVE_DAMAGE] 증가",
        "* 6회 사용 시: 상대에게 받는 대미지 $[SIX_PERCENT]% 감소",
})
public class Whitney extends CokesAbility {
    private static final Config<Integer> DURATION = new Config<>(Whitney.class, "duration", 25, Condition.TIME,
            "# 휘트니 버프 지속시간",
            "# 기본값: 25 (초)");
    private static final Config<Integer> COOLDOWN_ONE = new Config<>(Whitney.class, "cooldown-one", 30, Condition.COOLDOWN,
            "# 휘트니 기본 쿨타임",
            "# 기본값: 30 (초)");
    private static final Config<Integer> COOLDOWN_TWO = new Config<>(Whitney.class, "cooldown-two", 90, Condition.TIME,
            "# 휘트니 추가 쿨타임",
            "# 기본값: 90 (초)");

    public Whitney(Participant arg0) {
        super(arg0);
    }
}
