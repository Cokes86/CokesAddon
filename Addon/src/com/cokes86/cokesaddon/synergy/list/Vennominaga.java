package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.function.Predicate;

@AbilityManifest(name = "베노미너거", rank = Rank.L, species = Species.ANIMAL, explain = {
        "§7패시브 §8- §2독사신§f: $[DELAY]마다 $[RANGE]블럭 이내 플레이어에게 §2맹독§f을 2개 부여합니다.",
        "  죽을 위기에 처할 때, $[RANGE]블럭 이내 플레이어의 §2맹독§f을 전부 제거하고",
        "  제거한 §2맹독§f의 1/2에 해당하는 만큼 §b체력을 회복합니다.",
        "§7패시브 §8- §2베놈 번§f: §2맹독§f을 가진 플레이어가 다른 플레이어를 공격할 시",
        "  그 플레이어 역시 §2맹독§f을 부여합니다.",
        "  베노미너거는 이 효과를 받지 않습니다.",
        "§2맹독§f: 매 $[VENOM_ATTACK_DELAY]마다 §2맹독§f의 수만큼 대미지를 받습니다.",
        "  §2맹독§f으로 준 대미지 역시 §2맹독§f을 부여합니다.",
        "  §2맹독§f은 최대 $[MAX_VENOM_STACK]개까지 올라가며, $[COUNT_OF_VENOM_ATTACK]회까지 대미지를 줍니다."
})
public class Vennominaga extends CokesSynergy {
    public static final Config<Integer> MAX_VENOM_STACK = Config.of(Vennominaga.class, "max-venom-stack", 10, FunctionalInterfaces.positive(),
            "# 맹독의 최대 스택",
            "# 기본값: 10 (개)");
    public static final Config<Integer> VENOM_ATTACK_DELAY = Config.of(Vennominaga.class, "venom-attack-delay", 7, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 맹독의 공격 주기",
            "# 기본값: 7 (초)");
    public static final Config<Integer> COUNT_OF_VENOM_ATTACK = Config.of(Vennominaga.class, "count-of-venon-attack", 15, FunctionalInterfaces.positive(),
            "# 맹독의 최대 공격 횟수",
            "# 기본값: 15 (회)");
    public static final Config<Integer> RANGE = Config.of(Vennominaga.class, "range", 10, FunctionalInterfaces.positive(),
            "# 독사신 범위",
            "# 기본값: 10 (블럭)");
    public static final Config<Integer> DELAY = Config.of(Vennominaga.class, "delay", 5, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
            "# 독사신 발동 주기",
            "# 기본값: 5 (초)");

    public Vennominaga(Participant participant) {
        super(participant);
    }

    private final HashMap<Participant, Venom> venomMap = new HashMap<>();
    private final GodOfVenom gOfVenom = new GodOfVenom();
    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
            }
            if (getGame() instanceof Teamable) {
                Teamable game = (Teamable) getGame();
                return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
            }
            return target.attributes().TARGETABLE.getValue();
        }
        return true;
    };

    @SubscribeEvent(priority = 999)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
            int health = 0;
            for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), RANGE.getValue(), RANGE.getValue(), predicate)) {
                Participant participant = getGame().getParticipant(player.getPlayer());
                Venom venomTimer = venomMap.remove(participant);
                if (venomTimer == null) continue;
                health += venomTimer.stack;
                venomTimer.stop(true);
            }
            Healths.setHealth(getPlayer(), health / 2.0);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        Entity damager = CokesUtil.getDamager(e.getDamager());
        if (damager != null && getGame().isParticipating(damager.getUniqueId()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            Participant damagerParticipant = getGame().getParticipant(damager.getUniqueId());
            if (venomMap.containsKey(damagerParticipant)) {
                Participant participant = getGame().getParticipant(e.getEntity().getUniqueId());
                Venom venomTimer = venomMap.get(participant);
                if (venomTimer == null) {
                    venomTimer = new Venom(participant);
                    venomMap.put(participant, venomTimer);
                }
                venomTimer.addStack();
            }
        }
    }

    @Override
    public void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            gOfVenom.start();
        }
    }

    private class GodOfVenom extends AbilityTimer {
        public GodOfVenom() {
            super();
            setPeriod(TimeUnit.SECONDS, DELAY.getValue());
            setInitialDelay(TimeUnit.SECONDS, DELAY.getValue());
        }

        @Override
        protected void run(int count) {
            for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), RANGE.getValue(), RANGE.getValue(), predicate)) {
                Participant participant = getGame().getParticipant(player.getPlayer());
                Venom venomTimer = venomMap.get(participant);
                if (venomTimer == null) {
                    venomTimer = new Venom(participant);
                    venomMap.put(participant, venomTimer);
                }
                venomTimer.addStack();
                venomTimer.addStack();
            }
        }
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
}
