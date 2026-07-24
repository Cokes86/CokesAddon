package com.cokes86.cokesaddon.ability.list;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;

@AbilityManifest(name = "아인", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8- §c망각의 저주§f: 쿨타임이 있는 적을 공격 시 (쿨타임) * $[INCREASE]%의 피해를 더 줍니다.",
        "  또한 작동중인 쿨타임의 시간을 $[TIME] 늘립니다."
}, summarize = {
        "쿨타임이 있는 능력자들에게 더욱 큰 피해를 줍니다."
})
@Lite
public class Ain extends CokesAbility {
    private static final Config<Double> INCREASE = Config.of(Ain.class, "increase", 2.0, FunctionalInterfaces.positive(),
            "# 망각의 저주 피해 증가량", "# 기본값: 2.0 (%)");
    private static final Config<Integer> TIME = Config.time(Ain.class, "time", 3,
            "# 망각의 저주로 인해 늘어날 쿨타임", "# 기본값: 3");

    public Ain(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onCEntityDamage(CEntityDamageEvent e) {
        Entity victim = e.getEntity();
        if (e.isDamager(getPlayer()) && victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            if (getGame().isParticipating(victimPlayer)) {
                AbstractGame.Participant participant = getGame().getParticipant(victimPlayer);
                if (participant.getAbility() != null && participant.hasAbility()) {
                    AbilityBase abilityBase = participant.getAbility();
                    int cooldown = 0;
                    for (AbstractGame.GameTimer timer : abilityBase.getRunningTimers()){
                        if (timer instanceof Cooldown.CooldownTimer) {
                            cooldown += timer.getFixedCount();
                            timer.setCount(timer.getCount() + TIME.getValue());
                        }
                    }

                    e.setDamage(e.getDamage() * (1 + cooldown*INCREASE.getValue()/100.0));
                }
            }
        }
    }
}
