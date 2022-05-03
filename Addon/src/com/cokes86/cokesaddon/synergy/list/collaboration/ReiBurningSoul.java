package com.cokes86.cokesaddon.synergy.list.collaboration;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.google.common.collect.ImmutableList;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.ArrayList;
import java.util.List;

//레이 + 임페르노
@AbilityManifest(name = "레이<버닝소울>", rank = Rank.S, species = Species.DEMIGOD, explain = {
        "패시브 - 타오르는 영혼: 체력 재생 속도가 2배가 되지만, 항상 불타는 상태입니다.",
        "  받는 화염 피해가 (§c버닝 소울§f × 10)% 증가합니다.",
        "적 처치 - 수거: §c버닝 소울§f을 하나 얻습니다. 최대 5개까지 보유 가능합니다.",
        "근거리 공격 - 한 맺힌 힘: 대상을 §7(§e1 + §c버닝 소울§7)§f초간 추가로 불태웁니다.",
        "  만약, 대상이 이미 5초 이상 발화중이라면, 추가로 발화하지 않고",
        "  남은 발화시간에 비례하여 추가 피해를 준 후 불을 끕니다.",
        "  추가 피해량: (남아있는 발화 시간 - 4) * 1.2",
        "철괴 우클릭 - 영혼 해방: 자신의 최대체력의 10%을 지불하고, 7초간 유지되는 임시 §c버닝 소울§f을 하나 얻습니다.",
        "  이후, 모든 §c버닝 소울§f을 해방해 근처 적을 3의 고정 마법 대미지로 공격 후, 6초간 추가로 불태웁니다.",
        "  영혼 해방으로 얻은 §c버닝 소울§f은 수거의 §c버닝 소울§f의 제약에 속하지 않습니다.",
        "치명적 공격을 받을 시 - 라스트 버닝: §c버닝 소울§f이 존재한 상태일 경우",
        "  §c버닝 소울§f을 1개가 해방하여 5블럭 이내 플레이어를 5초간 추가로 불태우고 체력이 1로 변경됩니다."
}, summarize = {
        ""
})
public class ReiBurningSoul extends CokesSynergy implements ActiveHandler {
    private final BurningSoul burningSoul = new BurningSoul();
    private final List<DamageCause> damageCauseList = ImmutableList.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.HOT_FLOOR);

    public ReiBurningSoul(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            double max_health = AttributeUtil.getMaxHealth(getPlayer());
            double health = getPlayer().getHealth();
            if (health > max_health / 10.0) {
                Healths.setHealth(getPlayer(), health - max_health/10.0);
                burningSoul.addBurningSoul(true);

                //해방하여 대미지 주는 구문
            }
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            burningSoul.start();
        }
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (damageCauseList.contains(e.getCause())) {
            e.setDamage(e.getDamage() * (1 + burningSoul.getTotalBurningSoul()/10.0));
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        e.setAmount(e.getAmount() * 2.0);
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (!e.getParticipant().equals(getParticipant()) && e.getPlayer().getKiller() != null && e.getPlayer().getKiller().equals(getPlayer())) {
            burningSoul.addBurningSoul(false);
        }
    }

    private class BurningSoul extends AbilityTimer {
        private int soul = 0;
        private final List<Long> temporary_soul = new ArrayList<>();
        private final ActionbarChannel channel = newActionbarChannel();

        public BurningSoul() {
            super();
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void run(int count) {
            getPlayer().setFireTicks(21);
            temporary_soul.removeIf(mill -> mill + 5000 >= System.currentTimeMillis());
            channel.update("§c버닝 소울: "+soul + "(+ "+temporary_soul.size()+")");
        }

        public void addBurningSoul(boolean temporary) {
            if(temporary) temporary_soul.add(System.currentTimeMillis());
            else if (soul < 5) soul += 1;
        }

        public void removeBurningSoul() {
            if (soul == 0) temporary_soul.remove(0);
            else soul -= 1;
        }

        public int getTotalBurningSoul() {
            return soul + temporary_soul.size();
        }
    }
}
