package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@AbilityManifest(name = "베노미논", rank = Rank.S, species = Species.ANIMAL, explain = {
        ""
})
/* 베노미논
패시브 - 독사왕: 일정 주기마다 주변 플레이어에게 맹독 스택을 1 부여합니다.
  죽을 위기에 처할 때, 주변 플레이어의 맹독 스택을 전부 제거하고,
  그의 1/3에 해당하는 만큼의 체력으로 부활합니다.
  부활 이후 잠시동안 독사왕의 효과를 발동할 수 없습니다.
철괴 우클릭 - 사신강림: 게임 중 자신이 상대에게 준 맹독 스택이
  누적 75개 이상 되었을 시 사용가능합니다.
  자신의 시너지 능력이 <베노미너거>로 진화합니다.

베노미너거
패시브 - 독사신: 일정 주기마다 주변 플레이어에게 맹독 스택을 2 부여합니다.
  죽을 위기에 처할 때, 주변 플레이어의 맹독 스택을 전부 제거하고,
  그의 1/2에 해당하는 만큼의 체력으로 부활합니다.
패시브 - 포이즌 체인: 맹독을 가진 플레이어가 타 플레이어를 공격할 시
  그 플레이어에게 맹독 스택을 1 부여합니다.

맹독: 오비스니의 그 맹독 맞다.
*/
public class Vennominon extends CokesSynergy implements ActiveHandler {
    public static final Config<Integer> MAX_VENOM_STACK = Config.of(Vennominon.class, "max-venom-stack", 10, FunctionalInterfaces.positive(),
            "# 맹독의 최대 스택",
            "# 기본값: 10 (개)");
    public static final Config<Integer> VENOM_ATTACK_DELAY = Config.of(Vennominon.class, "venom-attack-delay", 7, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 맹독의 공격 주기",
            "# 기본값: 7 (초)");
    public static final Config<Integer> COUNT_OF_VENOM_ATTACK = Config.of(Vennominon.class, "count-of-venon-attack", 15, FunctionalInterfaces.positive(),
            "# 맹독의 최대 공격 횟수",
            "# 기본값: 15 (회)");

    public Vennominon(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        return false;
    }

    private class Venom extends AbilityTimer {
        private final Participant target;
        private final IHologram hologram;
        private final int maxCounter = MAX_VENOM_STACK.getValue();
        private int stack;
        private int damageCount = 0;
        private final String icon = "☣";

        public Venom(Participant target) {
            super();
            this.setPeriod(TimeUnit.TICKS, 1);
            this.target = target;
            final Player targetPlayer = target.getPlayer();
            this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
            this.hologram.setText(CokesUtil.repeatWithTwoColor(icon, '2', stack, 'f', maxCounter-stack));
            this.hologram.display(getPlayer());
            this.stack = 1;
            this.start();
        }
        @Override
        protected void run(int arg0) {
            this.hologram.setText(CokesUtil.repeatWithTwoColor(icon, '2', stack, 'f', maxCounter-stack));
            final Player targetPlayer = target.getPlayer();
            hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
            if (arg0 % (20 * VENOM_ATTACK_DELAY.getValue()) == 0 && damageCount <= COUNT_OF_VENOM_ATTACK.getValue()) {
                targetPlayer.damage(stack, getPlayer());
                damageCount++;
            }
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            hologram.hide(getPlayer());
            hologram.unregister();
        }

        private void addStack() {
            if (stack < maxCounter) {
                stack++;
            }
        }
    }

    public class Veniminaga extends CokesSynergy {
        public Veniminaga(Participant participant) {
            super(participant);
        }
    }
}
