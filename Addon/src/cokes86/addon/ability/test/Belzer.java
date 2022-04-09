package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "벨져", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭 시 주변 10블럭 이내 플레이어에게 2틱마다 3의 대미지를 10회 주면서 공중에 띄웁니다.",
        "이후, 0.5초 뒤 10의 대미지를 주며 바깥으로 튕겨냅니다. $[COOLDOWN]",
        "스킬 시전동안 자신은 움직일 수 없습니다."
})
public class Belzer extends CokesAbility {
    public Belzer(Participant arg0) {
        super(arg0);
    }
}
