package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

@AbilityManifest(name = "팬쉽리메이크", rank = Rank.A, species = Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c팬텀 쇼§f: 자신 위치에 §7그림자§f를 3초간 소환하고 자신은 §a은신§f합니다. $[COOLDOWN]",
        "  그림자를 공격한 플레이어는 폭발과 함께 $[DAMAGE]의 대미지를 주고",
        "  그 사람의 능력의 등급을 §b1단계 내려 재배정§f합니다.",
        "  재배정한 플레이어는 3초간 무적시간이 부여합니다.",
        "  그림자가 사라지면 §a은신§f또한 중간에 해제됩니다.",
        "[HIDDEN] 어 이게 아닌데"
})
@NotAvailable({AbstractMix.class, AbstractTripleMix.class})
/*
철괴 우클릭 시 그림자를 소환 후 은신
그림자 공격 시 그 자리에 폭발, 등급 1단계 하락
그림자가 사라지면 은신 풀림.
*/
public class PhantomThiefRemake extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = Config.of(PhantomThiefRemake.class, "cooldown", 90, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 팬텀 쇼 쿨타임",
            "# 기본 값: 90 (초)");

    private IDummy phantom = null;
    private final Cooldown cooldown = new Cooldown(90);
    private final PhantomShow phantomShow = new PhantomShow();

    private final List<AbilityBase> rank_c = new ArrayList<>();
    private final List<AbilityBase> rank_b = new ArrayList<>();
    private final List<AbilityBase> rank_a = new ArrayList<>();
    private final List<AbilityBase> rank_s = new ArrayList<>();
    private final List<AbilityBase> rank_l = new ArrayList<>();
    private final List<AbilityBase> rank_special = new ArrayList<>();

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
            super(60, cooldown);
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
        protected void onDurationProcess(int i) {
        }
    }

    private class InvTimer extends GameTimer {
        private final ActionbarChannel channel;
        private final Participant target;
        public InvTimer(AbstractGame game, Participant target) {
            game.super(TaskType.REVERSE, 3);
            channel = target.actionbar().newChannel();
            this.target = target;
        }

        @Override
        protected void onStart() {
            if (target.getAbility() != null) {
                Rank rank = target.getAbility().getRank();
                List<AbilityBase> returnAbilities = null;
                switch(rank) {
                    case SPECIAL: {
                        returnAbilities = rank_l;
                        break;
                    }
                    case L: {
                        returnAbilities = rank_s;
                        break;
                    }
                    case S: {
                        returnAbilities = rank_a;
                        break;
                    }
                    case A: {
                        returnAbilities = rank_b;
                        break;
                    }
                    case B: {
                        returnAbilities = rank_c;
                        break;
                    }
                    case C: {
                        List<AbilityBase> tmp = rank_special;
                        tmp.addAll(rank_l);
                        tmp.addAll(rank_s);
                        tmp.addAll(rank_a);
                        returnAbilities = tmp;
                        target.getPlayer().sendMessage("[팬텀 시프] 당신의 낮은 등급에 가여움을 느꼈는지 새로운 힘이 솟구치는 기분입니다.");
                        break;
                    }
                }
                AbilityBase newOne = new Random().pick(returnAbilities);
                try {
                    target.setAbility(newOne.getRegistration());
                    target.getPlayer().sendMessage("[팬텀 시프] 능력이 재배정되었습니다. 당신의 능력은 §e"+newOne.getName()+"§f입니다.");
                } catch (ReflectiveOperationException e) {
                    getPlayer().sendMessage("능력을 재배정하는 도중 오류가 발생하였습니다.");
                    e.printStackTrace();
                    stop(true);
                }
            }
        }

        @Override
        protected void run(int count) {
            channel.update("무적: "+ TimeUtil.parseTimeAsString(count));
            SoundLib.PIANO.broadcastInstrument(Note.natural(1, Tone.C));
        }
    }
}
