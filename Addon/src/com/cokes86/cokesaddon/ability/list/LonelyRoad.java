package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

@AbilityManifest(name = "나 홀로 외길", rank = Rank.B, species = Species.HUMAN, explain = {
        "블럭을 밟을 때 마다 1스택이 오릅니다.",
        "$[STACK_BLOCK]스택 당 상대에게 주는 대미지가 $[DAMAGE] 증가합니다.",
        "밟은 블럭에 대해서는 $[COOLDOWN]간 스택이 다시 쌓이지 않습니다."
})
public class LonelyRoad extends CokesAbility {
    private static final Config<Integer> STACK_BLOCK = Config.of(LonelyRoad.class, "stack-block", 75, FunctionalInterfaces.positive(),
            "# 대미지가 상승하기 위해 밟아야할 블럭의 수",
            "# 기본값: 75 (블럭)");
    private static final Config<Integer> COOLDOWN = Config.of(LonelyRoad.class, "cooldown", 60, FunctionalInterfaces.TIME,
            "# 밟은 블럭이 다시 측정되기위한 대기시간",
            "# 기본값: 60 (초)");
    private static final Config<Double> DAMAGE = Config.of(LonelyRoad.class, "damage", 1d, FunctionalInterfaces.positive(),
            "# 스택 당 증가하는 대미지",
            "# 기본값: 1.0");
    private int stack = 0;
    private final List<BlockData> blockData = new ArrayList<>();
    private final ActionbarChannel channel = newActionbarChannel();

    public LonelyRoad(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        Entity damager = CokesUtil.getDamager(e.getDamager());
        if (damager != null && damager.equals(getPlayer())) {
            //noinspection IntegerDivisionInFloatingPointContext
            e.setDamage(e.getDamage() + (stack / STACK_BLOCK.getValue().intValue()) * DAMAGE.getValue());
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer().equals(getPlayer())) {
            Block down = getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
            BlockData tempBlockData = new BlockData(down.getX(), down.getY(), down.getZ());
            for (BlockData data : blockData) {
                if (data.equals(tempBlockData)) return;
            }
            blockData.add(tempBlockData);
            tempBlockData.start();
            channel.update(String.format("%s스택", stack));
            if (stack % STACK_BLOCK.getValue() == 0) {
                SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
            }
        }
    }

    private class BlockData extends AbilityTimer {
        private final double x, y, z;
        public BlockData(double x, double y, double z) {
            super(COOLDOWN.getValue());
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        protected void onStart() {
            stack++;
        }

        @Override
        protected void onEnd() {
            blockData.remove(this);
        }

        @Override
        protected void onSilentEnd() {
            blockData.remove(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlockData blockData = (BlockData) o;

            if (Double.compare(blockData.x, x) != 0) return false;
            if (Double.compare(blockData.y, y) != 0) return false;
            return Double.compare(blockData.z, z) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(z);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}