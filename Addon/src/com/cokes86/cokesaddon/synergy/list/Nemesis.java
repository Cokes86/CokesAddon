package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "", rank = Rank.S, species = Species.GOD, explain = {
    "패시브 - 람누시아: 자신이 받은 최종대미지의 10%을 저장합니다.",
    "  저장한 대미지는 최대 1분까지 유지됩니다.",
    "  상대방을 공격할 시 람누시아로 저장한 대미지를",
    "  고정 대미지로 추가로 부여합니다.",
    "철괴 우클릭 - 아드라스테이아: 저장한 대미지를 모두 소모합니다.",
    "  주변 플레이어에게 대미지를 주고 밀어냅니다.",
    "  주변 플레이어의 범위와 대미지, 넉백거리는 저장된 대미지에 비례합니다."
})
public class Nemesis extends CokesSynergy {

    public Nemesis(Participant participant) {
        super(participant);
    }
    
}
