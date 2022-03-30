package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "능력이름", rank = Rank.A, species = Species.HUMAN, explain = {
    "이곳은 능력의 설명입니다.",
    "/aw check 나 /aw abilities를 통해 볼 수 있는 내용입니다.",
    "$[변수이름]을 여기에 입력하면 자동으로 그 값을 한 번 불러옵니다.",
    "${변수이름}도 같으나 []와는 달리 계속 불러옵니다."
}, summarize = {
    "이곳은 능력의 요약입니다.",
    "/aw sum 으로 볼 수 있는 내용입니다."
})
public class Zark extends CokesAbility {

    public Zark(Participant arg0) {
        super(arg0);
    }
    
}
