package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
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
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.stream.Collectors;

@AbilityManifest(name = "팬쉽리메이크", rank = Rank.A, species = Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c팬텀 쇼§f: 자신 위치에 §7그림자§f를 3초간 소환하고 자신은 §a은신§f합니다. $[COOLDOWN]",
        "  그림자를 공격한 플레이어는 $[DAMAGE]의 대미지를 주고",
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
    private static final Config<Double> DAMAGE = Config.of(PhantomThiefRemake.class, "damage", 7.0, FunctionalInterfaces.positive(),
            "# 팬텀 쇼 도중 분신 공격 시 대미지",
            "# 기본 값: 7");

    private IDummy phantom = null;
    private final Cooldown cooldown = new Cooldown(90);
    private final PhantomShow phantomShow = new PhantomShow();

    private final List<AbilityRegistration> rank_c = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.C))
                .collect(Collectors.toList());
    private final List<AbilityRegistration> rank_b = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.B))
                .collect(Collectors.toList());
    private final List<AbilityRegistration> rank_a = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.A))
                .collect(Collectors.toList());
    private final List<AbilityRegistration> rank_s = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.S))
                .collect(Collectors.toList());
    private final List<AbilityRegistration> rank_l = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.L))
                .collect(Collectors.toList());
    private final List<AbilityRegistration> rank_special = AbilityList.values().stream()
                .filter(registration -> registration.getManifest().rank().equals(Rank.SPECIAL))
                .collect(Collectors.toList());

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
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity damagerEntity = CokesUtil.getDamager(e.getDamager());
        if (phantomShow.isRunning() && e.getEntity().equals(phantom) && !damagerEntity.equals(getPlayer()) && getGame().isParticipating(damagerEntity.getUniqueId())) {
            Participant damager = getGame().getParticipant(damagerEntity.getUniqueId());
            damager.getPlayer().damage(DAMAGE.getValue(), getPlayer());
            new InvTimer(getGame(), damager).start();
            phantomShow.stop(false);
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
        protected void onDurationProcess(int i) {}
    }

    private class InvTimer extends GameTimer implements Listener {
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
                Bukkit.getPluginManager().registerEvents(InvTimer.this, AbilityWar.getPlugin());
                Rank rank = target.getAbility().getRank();
                List<AbilityRegistration> returnAbilities = null;
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
                        List<AbilityRegistration> tmp = rank_special;
                        tmp.addAll(rank_l);
                        tmp.addAll(rank_s);
                        tmp.addAll(rank_a);
                        returnAbilities = tmp;
                        target.getPlayer().sendMessage("[팬텀 시프] 당신의 낮은 등급에 가여움을 느꼈는지 새로운 힘이 솟구치는 기분입니다.");
                        break;
                    }
                }
                AbilityRegistration newOne = new Random().pick(returnAbilities);
                try {
                    target.setAbility(newOne);
                    target.getPlayer().sendMessage("[팬텀 시프] 능력이 재배정되었습니다. 당신의 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                } catch (ReflectiveOperationException e) {
                    getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
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

        @Override
        protected void onEnd() {
            HandlerList.unregisterAll(InvTimer.this);
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(InvTimer.this);
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent e) {
            if (e.getEntity().equals(target.getPlayer())) {
                e.setCancelled(true);
            }
        }
    }
}
