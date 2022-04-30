package com.cokes86.cokesaddon.synergy.list.collaboration;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

//레이 + 임페르노
@AbilityManifest(name = "레이<버닝소울>", rank = Rank.S, species = Species.DEMIGOD, explain = {
        "패시브 - 타오르는 영혼: 화염계 대미지를 2배로 받습니다.",
        "  마그마블럭에 의해 대미지를 받을 시 1초간 추가 발화합니다.",
        "근거리 공격 - 자신이 발화된 상태에서 상대방을 공격할 시 §4불꽃§f을 획득하고",
        "  공격 대상에게 발화를 2초 추가 부여합니다.",
        "  자신이 발화상태일 경우, 발화시간이 2초 감소하고 추가 대미지가 발생합니다.",
        "  추가 대미지: [§4불꽃§f 개수 + 남은 발화시간] × 0.15",
        "철괴 우클릭 - 자신의 최대체력의 10%을 지불하고, 자신은 5초간 추가 발화합니다.",
        "  또한, 5블럭 이내의 플레이어에게 §4화상§f을 10초 부여합니다.",
})
public class ReiBurningSoul extends CokesSynergy {
    public ReiBurningSoul(Participant participant) {
        super(participant);
    }
}
