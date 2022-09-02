package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AbilityManifest(name = "팬쉽리메이크", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭 - 팬텀 쇼: 자신 위치에 그림자를 3초간 소환하고 자신은 3초간 은신합니다.",
        "  그림자를 공격한 플레이어는 폭발과 함께",
        "  그 사람의 능력의 등급을 §c1단계 내려 재배정§f합니다.",
        "[HIDDEN] 불쌍한 아이로구나"
})

/*
철괴 우클릭 시 자신 체력의 반이 되는 그림자를 소환
그림자를 죽일 시 그 자리에 폭발이 일어나며 그 사람이 가지고 있던 아이템 절반을 훔침.
그림자 => npc, 그림자 만들면 3초간 본체 은신.
*/
public class PhantomThiefRemake extends CokesAbility implements ActiveHandler {
    private IDummy phantom = null;
    private final Cooldown cooldown = new Cooldown(90);
    private final PhantomShow phantomShow = new PhantomShow();

    public PhantomThiefRemake(Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && phantom == null && !cooldown.isCooldown() && !phantomShow.isDuration()) {
            return phantomShow.start();
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update != Update.RESTRICTION_CLEAR && phantom != null) {
            phantom.remove();
            phantom = null;
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (phantomShow.isRunning()) {
            NMSUtil.onPlayerJoin(getPlayer(), e);
        }
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (phantomShow.isRunning()) {
            NMSUtil.onPlayerQuit(getPlayer(), e);
        }
    }

    private class PhantomShow extends Duration {

        public PhantomShow() {
            super(60,cooldown);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onDurationStart() {
            phantom = NMSUtil.createDummy(getPlayer().getLocation(), getPlayer());
            for (Participant participant : getGame().getParticipants()) {
                phantom.display(participant.getPlayer());
            }
            NMSUtil.hidePlayer(getPlayer());
            getParticipant().attributes().TARGETABLE.setValue(false);
        }

        @Override
        protected void onDurationEnd() {
            phantom.remove();
            phantom = null;
            NMSUtil.showPlayer(getPlayer());
            getParticipant().attributes().TARGETABLE.setValue(true);
        }

        @Override
        protected void onDurationSilentEnd() {
            phantom.remove();
            phantom = null;
            NMSUtil.showPlayer(getPlayer());
            getParticipant().attributes().TARGETABLE.setValue(true);
        }

        @Override
        protected void onDurationProcess(int i) {}
    }
}
