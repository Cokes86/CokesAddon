package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Material;
import org.bukkit.entity.Damageable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "져스틴R", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 두가지 인격: 약 $[PERIOD] 간격으로 자신의 인격이 뒤바뀝니다.",
        "  인격에 따라 각기 다른 효과를 부여받습니다.",
        "검으로 공격 시 - ???: 공격 시 인격에 따라 각기 다른 효과를 가집니다.",
        "  [일반] 대미지가 $[DAMAGE]로 공격됩니다. 공격 이후 0.5초 이내 검을 들고 우클릭 시",
        "    감소했던 대미지를 주면서 상대방을 멀리 튕겨냅니다.",
        "  [광기] 절단 상태이상을 5초 부여하고, 준 대미지의 10%를 회복합니다.",
        "철괴 우클릭 - 탈출: 자신의 인격을 강제로 변경합니다.",
        "  이때, 바뀐 인격의 주기는 반으로 감소합니다.",
        "상태이상 - 절단: 구속 1효과를 받습니다. 매 1초마다 2의 고정대미지를 받습니다.",
        "  이 고정대미지는 공격무적을 무시합니다."
})
@Beta
public class JustinR extends CokesAbility implements ActiveHandler {
    private NormalTimer normalTimer = new NormalTimer(getPlayer(), 0);
    private boolean madness = false;
    private final Random r = new Random();
    private final AbilityTimer justin = new AbilityTimer(45*20) {
        @Override
        protected void onStart() {
            int back = r.nextInt(45*10) - 45*5;
            setCount(getMaximumCount() + back);
        }

        @Override
        protected void onEnd() {
            madness = !madness;
            start();
        }
    }.setPeriod(TimeUnit.TICKS, 1).setBehavior(RestrictionBehavior.PAUSE_RESUME);

    public JustinR(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        return false;
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!madness) {
            if (normalTimer.isRunning()) normalTimer.stop(true);
            double damage = e.getDamage();
            e.setDamage(damage/10*7);
            normalTimer = new NormalTimer((Damageable) e.getEntity(), damage/10*3);
            normalTimer.start();
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            justin.start();
        }
    }

    class NormalTimer extends AbilityTimer {
        final Damageable damageable;
        final double damage;

        public NormalTimer(Damageable damageable, double damage){
            super(10);
            this.damageable = damageable;
            this.damage = damage;
            setPeriod(TimeUnit.TICKS, 1);
        }
    }
}
