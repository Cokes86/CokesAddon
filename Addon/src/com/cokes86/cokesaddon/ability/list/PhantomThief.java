package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import com.cokes86.cokesaddon.util.timer.InvincibilityTimer;
import com.cokes86.cokesaddon.util.nms.IDummy;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.stream.Collectors;

@AbilityManifest(name = "팬텀 시프", rank = Rank.S, species = Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c팬텀 쇼§f: 자신 위치에 §7그림자§f를 3초간 소환하고 자신은 §a은신§f합니다. $[COOLDOWN]",
        "  그림자를 공격한 플레이어는 $[DAMAGE]의 대미지를 주고",
        "  그 사람의 능력의 등급을 §b1단계 내려 재배정§f합니다.",
        "  재배정한 플레이어는 3초간 무적시간이 부여되며, 공격또한 불가능합니다.",
        "  그림자가 사라지면 §a은신§f또한 중간에 해제됩니다.",
        "§8[§7HIDDEN§8] §b구제§f: 누구를 구제하셨나요?"
})
@NotAvailable({AbstractMix.class, AbstractTripleMix.class})
public class PhantomThief extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> COOLDOWN = Config.of(PhantomThief.class, "cooldown", 120, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN,
            "# 팬텀 쇼 쿨타임",
            "# 기본 값: 120 (초)");
    private static final Config<Double> DAMAGE = Config.of(PhantomThief.class, "damage", 3.5, FunctionalInterfaces.positive(),
            "# 팬텀 쇼 도중 분신 공격 시 대미지",
            "# 기본 값: 3.5");

    private IDummy phantom = null;
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final PhantomShow phantomShow = new PhantomShow();

    public PhantomThief(Participant arg0) {
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
        if (phantomShow.isRunning() && e.getEntity().getUniqueId().equals(phantom.getUniqueID()) && !damagerEntity.equals(getPlayer()) && getGame().isParticipating(damagerEntity.getUniqueId())) {
            Participant damager = getGame().getParticipant(damagerEntity.getUniqueId());
            damager.getPlayer().damage(DAMAGE.getValue(), getPlayer());
            phantomShow.stop(false);
            phantom.remove();
            setNewAbility(damager);
            e.setDamage(0);
        }
    }

    private void setNewAbility(Participant target) {
        if (target.getAbility() != null) {
            Rank rank = target.getAbility().getRank();
            List<AbilityRegistration> returnAbilities = AbilityList.values().stream().filter(abilityRegistration -> {
                Rank rank1 = abilityRegistration.getManifest().rank();
                return (rank == Rank.SPECIAL && rank1 == Rank.L) || (rank == Rank.L && rank1 == Rank.S)
                        || (rank == Rank.S && rank1 == Rank.A) || (rank == Rank.A && rank1 == Rank.B)
                        || (rank == Rank.B && rank1 == Rank.C) || (rank == Rank.C && (rank1 == Rank.S || rank1 == Rank.L || rank1 == Rank.SPECIAL));
            }).collect(Collectors.toList());

            AbilityRegistration newOne = new Random().pick(returnAbilities);

            try {
                target.setAbility(newOne);
                target.getPlayer().sendMessage("[팬텀 시프] 능력이 재배정되었습니다. 당신의 능력은 §e"+newOne.getManifest().name()+"§f입니다.");
                getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 능력을 재배정하였습니다.", target.getPlayer().getDisplayName()));
                if (rank == Rank.C) {
                    getPlayer().sendMessage(String.format("[팬텀 시프] §e%s§f님의 능력이 §eC등급§f이기에 <%s구제§f>하였습니다.",
                            target.getPlayer().getDisplayName(),
                            "§"+newOne.getManifest().rank().getRankName().charAt(1)));
                }
                new InvincibilityTimer(target, 3, true).start();
            } catch (ReflectiveOperationException e) {
                getPlayer().sendMessage("[팬텀 시프] 능력을 재배정하는 도중 오류가 발생하였습니다.");
                e.printStackTrace();
            }
        }
    }

    private class PhantomShow extends Duration {
        public PhantomShow() {
            super(60, cooldown);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onDurationStart() {
            if (phantom != null) {
                phantom.remove();
            }
            phantom = NMSUtil.createDummy(getPlayer().getLocation(), getPlayer());
            phantom.getBukkitEntity().getInventory().setStorageContents(getPlayer().getInventory().getStorageContents());
            for (Participant participant : getGame().getParticipants()) {
                phantom.display(participant.getPlayer());
            }
            NMSUtil.hidePlayer(getParticipant());
            getParticipant().attributes().TARGETABLE.setValue(false);
        }

        @Override
        protected void onDurationEnd() {
            phantom.remove();
            phantom = null;
            NMSUtil.showPlayer(getParticipant());
            getParticipant().attributes().TARGETABLE.setValue(true);
        }

        @Override
        protected void onDurationSilentEnd() {
            phantom.remove();
            phantom = null;
            NMSUtil.showPlayer(getParticipant());
            getParticipant().attributes().TARGETABLE.setValue(true);
        }

        @Override
        protected void onDurationProcess(int i) {}
    }
}
