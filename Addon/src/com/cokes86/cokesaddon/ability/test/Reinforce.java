package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.timer.NoticeTimeTimer;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;

@AbilityManifest(name = "리인포스", rank = Rank.A, species = Species.HUMAN, explain = {
        "철괴 우클릭 - 강화: 자신의 능력을 강화합니다.",
        "  강화 시 일정 확률로 성공하거나, 하락하거나, 파괴됩니다.",
        "  최대 25단계까지 강화가 가능합니다.",
        "  파괴되었을 시 0강으로 돌아가고서는 강화를 할 수 없으나",
        "  $[REPAIR] 뒤 12성으로 복구되어 다시 강화를 할 수 있습니다.",
        "  1성에서 10성, 15성, 20성에서 실패할 시 하락하지 않습니다.",
        "  2회 실패할 시 다음 강화는 무조건 성공합니다.",
        "  웅크리고 사용할 시 파괴가 방지되며, 쿨타임이 발생합니다. $[COOLDOWN]",
        "  단, 이는 12성에서 17성까지만 유효합니다.",
        "  복구와 파괴 방지 쿨타임은 초기화에 영향을 받지 않습니다."
})
@Beta
public class Reinforce extends CokesAbility implements ActiveHandler {
    private int level = 0;
    private int downgrade = 0;
    private final Random random = new Random();
    private final ActionbarChannel channel = newActionbarChannel();
    public Reinforce(Participant arg0) {
        super(arg0);
    }

    private double[] getPercentage() {
        double success = 0;
        double destroy = 100;
        if (downgrade >= 2) {
            success = 100;
            destroy = 0;
        } else if (level <= 2) {
            success = 95 - 5 * level;
            destroy = 0;
        } else if (level <= 14) {
            success = 100 - 5 * level;
            if (level <= 11) {
                destroy = 0;
            } else if (level == 12) {
                destroy = 0.6;
            } else if (level == 13) {
                destroy = 1.3;
            } else {
                destroy = 1.4;
            }
        } else if (level <= 21) {
            success = 30;
            if (level <= 17) {
                destroy = 2.1;
            } else if (level <= 19) {
                destroy = 2.8;
            }  else {
                destroy = 7.0;
            }
        } else if (level == 22) {
            success = 3;
            destroy = 19.4;
        } else if (level == 23) {
            success = 2;
            destroy = 29.4;
        } else if (level == 24) {
            success = 1;
            destroy = 39.6;
        }

        return new double[] {success, 100-success-destroy, destroy};
    }

    private final RepairTimer repairTimer = new RepairTimer();
    private final AntiDestructionTimer antiDestruction = new AntiDestructionTimer();

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK) && !repairTimer.isRepairing()) {
            double percentage = random.nextDouble() * 100;
            double success = getPercentage()[0];
            double fail = success + getPercentage()[1];
            if (!antiDestruction.isRunning() && (level <= 17 && level >= 12) && getPlayer().isSneaking()) {
                fail += getPercentage()[2];
                antiDestruction.start();
                getPlayer().sendMessage("파괴방지를 적용합니다.");
            }
            if (percentage < success) {
                level ++;
                downgrade = 0;
                getPlayer().sendMessage("강화에 성공하였습니다!");
                getPlayer().sendMessage((level+1)+"단계 도전 확률");
                getPlayer().sendMessage("성공: "+getPercentage()[0]+"%");
                getPlayer().sendMessage("실패: "+getPercentage()[1]+"%");
                getPlayer().sendMessage("파괴: "+getPercentage()[2]+"%");
                SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
            } else if (percentage < fail) {
                if (!(level <= 10 || level == 15 || level == 20)) {
                    level--;
                    downgrade++;
                }
                getPlayer().sendMessage("강화에 실패하였습니다...");
                getPlayer().sendMessage((level+1)+"단계 도전 확률");
                getPlayer().sendMessage("성공: "+getPercentage()[0]+"%");
                getPlayer().sendMessage("실패: "+getPercentage()[1]+"%");
                getPlayer().sendMessage("파괴: "+getPercentage()[2]+"%");
            } else {
                level = 0;
                getPlayer().sendMessage("강화에 실패하여 능력이 파괴되었습니다...");
            }
            channel.update(level+"단계");
            return true;
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update.equals(Update.RESTRICTION_CLEAR)) {
            channel.update(level+"단계");
        }
    }

    private class RepairTimer extends NoticeTimeTimer {

        public RepairTimer() {
            super(Reinforce.this, "§3복구", 120);
            this.setBehavior(RestrictionBehavior.PAUSE_RESUME);
        }

        @Override
        protected void run(int count) {
            super.run(count);
            if (count == (getMaximumCount() / 2) || (count <= 5 && count >= 1)) {
                getPlayer().sendMessage("§3복구%s§: " + TimeUtil.parseTimeAsString(getFixedCount()));
                SoundLib.BLOCK_NOTE_BLOCK_HARP.playSound(getPlayer());
            }
        }

        @Override
        protected void onEnd() {
            super.onEnd();
            level = 12;
        }

        public boolean isRepairing() {
            if (isRunning()) {
                getPlayer().sendMessage("능력이 지금 복구중입니다.");
            }
            return isRunning();
        }
    }

    private class AntiDestructionTimer extends NoticeTimeTimer {

        public AntiDestructionTimer() {
            super(Reinforce.this, "§c파괴방지", 60);
            this.setBehavior(RestrictionBehavior.PAUSE_RESUME);
        }

        @Override
        protected void onEnd() {
            super.onEnd();
        }
    }
}
