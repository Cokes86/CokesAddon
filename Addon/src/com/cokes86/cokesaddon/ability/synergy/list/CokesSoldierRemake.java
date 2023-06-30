package com.cokes86.cokesaddon.ability.synergy.list;

import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.CokesUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import org.bukkit.Material;

@AbilityManifest(name = "코크스<군인>리", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "패시브 - 계급과 전역: 계급이 존재하며, 각 특정한 시간대에 진급을 합니다.",
        "  총 360초가 지나면 전역을 합니다.",
        "  진급 및 전역 시 각 스킬의 효과가 점점 강화됩니다.",
        "검 우클릭 - K2 소총: 전방을 향해 소총 3발을 발사합니다.",
        "  계급에 따라 대미지와 사거리가 변합니다.",
        "철괴 우클릭 - BTCS A1: 가장 가까운 플레이어에게 화력을 유도합니다.",
        "  화력의 중앙에서 부터 반경 10블럭 내 플레이어들은 거리에 비례한 고정대미지를 입습니다.",
        "  계급에 따라 화력유도에 응하지 않을 확률이 존재합니다.",
        "  화력에 응하지 못했을 경우, 그 횟수에 따라 징계를 받습니다."
})
public class CokesSoldierRemake extends CokesSynergy implements ActiveHandler {
    public CokesSoldierRemake(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        return false;
    }

    @Override
    public boolean usesMaterial(Material material) {
        return super.usesMaterial(material) || CokesUtil.getSwords().contains(material);
    }

    private enum SoldierRank {
        PRIVATE,
        PRIVATE_FIRST,
        CORPORAL,
        SERGEANT,
        REVERSE
    }
}
