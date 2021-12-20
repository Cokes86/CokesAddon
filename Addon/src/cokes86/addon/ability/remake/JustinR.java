package cokes86.addon.ability.remake;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import org.bukkit.Material;

@AbilityManifest(name = "져스틴R", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 두가지 인격: 약 45초 간격으로 자신의 인격이 뒤바뀝니다.",
        "  인격에 따라 각기 다른 효과를 부여받습니다.",
        "근거리 공격 시 - ???: 공격 시 인격에 따라 각기 다른 효과를 가집니다.",
        "  [일반] 대미지가 0.7배로 공격됩니다. 공격 이후 0.5초 이내 검을 들고 우클릭 시",
        "    남은 0.3배의 대미지가 들어가면서 상대방을 멀리 튕겨냅니다.",
        "  [광기] 절단 상태이상을 5초 부여하고, 준 대미지의 10%를 회복합니다.",
        "철괴 우클릭 - 탈출: 자신의 인격을 ",
        "상태이상 - 절단: 구속 1효과를 받습니다. 매 1초마다 2의 고정대미지를 받습니다.",
        "  이 고정대미지는 공격무적을 무시합니다."
})
public class JustinR extends CokesAbility implements ActiveHandler {

    public JustinR(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        return false;
    }
}
