package com.cokes86.cokesaddon.ability.murdermystery.innocent;

import com.cokes86.cokesaddon.ability.Config;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@AbilityManifest(name = "시민: 택시기사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "항상 신속1 효과를 받습니다.",
        "금 좌클릭으로 금 $[ARROW_COST]개를 소모해 활과 화살을 얻을 수 있습니다.",
        "[택시] 금 우클릭으로 금 $[TAXI_COST]개를 소모해 $[RANGE]블럭 이내 플레이어를",
        "  $[DURATION]간 신속4와 함께 자신의 손 위로 태웁니다. $[COOLDOWN]",
        "  중도에 다시 금 우클릭으로 멈출 수 있으며",
        "  지속동안 자신과 태워진 플레이어는 무적판정을 받습니다.",
        "  이 동안 본인은 아이템을 바꿀 수 없습니다.",
        "[부스팅] 금 F키로 10개를 소모해 [택시]로",
        "  태운 플레이어 수 * 4초간 신속 5를 얻습니다.",
        "  사용 시 [택시]로 태운 플레이어 수는 리셋됩니다."
})
public class TaxiDriver extends AbstractInnocent {
    private static final Config<Integer> COOLDOWN = Config.cooldown(TaxiDriver.class, "cooldown", 10);
    private static final Config<Integer> DURATION = Config.time(TaxiDriver.class, "duration", 5);
    private static final Config<Integer> ARROW_COST = Config.positive(TaxiDriver.class, "arrow-cost", 8);
    private static final Config<Integer> TAXI_COST = Config.positive(TaxiDriver.class, "taxi-cost", 5);
    private static final Config<Integer> RANGE = Config.positive(TaxiDriver.class, "range", 5);

    private Entity target = null;

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
                && !((MurderMystery) getGame()).isDead(entity.getUniqueId())
                && getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
    };

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final Duration skill = new Duration(DURATION.getValue() * 20, cooldown) {
        @Override
        protected void onDurationStart() {
            if (target != null && target.isValid()) {
                target.setInvulnerable(true);
                target.setGravity(false);
            } else {
                stop(true);
            }
        }

        @Override
        protected void onDurationProcess(int seconds) {
            if (target.getVehicle() != null) {
                target.eject();
            }
            final Vector direction = getPlayer().getLocation().getDirection().setY(0).normalize();
            final Location teleportLocation = getPlayer().getEyeLocation();

            teleportLocation.setYaw(getPlayer().getLocation().getYaw());
            teleportLocation.setPitch(0);
            teleportLocation.add(direction.clone().multiply(-0.5));
            teleportLocation.add(VectorUtil.rotateAroundAxisY(direction.clone(), 90).multiply(0.65));
            target.teleport(teleportLocation, TeleportCause.PLUGIN);
            PotionEffects.SPEED.addPotionEffect(getPlayer(), 25, 3, true);
            if (target instanceof Player) {
                final Player targetPlayer = (Player) target;
                targetPlayer.setGliding(true);
            }
        }

        @Override
        protected void onDurationEnd() {
            if (target != null && target.isValid()) {
                target.setInvulnerable(false);
                target.setGravity(true);
                Bukkit.getPluginManager().callEvent(new TaxiEndEvent(target));
                target = null;
            }
        }

        @Override
        protected void onDurationSilentEnd() {
            if (target != null && target.isValid()) {
                target.setInvulnerable(false);
                target.setGravity(true);
                Bukkit.getPluginManager().callEvent(new TaxiEndEvent(target));
                target = null;
            }
        }
    }.setPeriod(TimeUnit.TICKS, 1);

    boolean boosting = false;
    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        protected void run(int count) {
            if (!skill.isRunning()) {
                if (!boosting) PotionEffects.SPEED.addPotionEffect(getPlayer(), 25, 0, true);
                else PotionEffects.SPEED.addPotionEffect(getPlayer(), 25, 4, true);
            }
        }
    }.setPeriod(TimeUnit.TICKS, 1);

    public TaxiDriver(Participant participant) {
        super(participant);
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onToggleSneakEvent(PlayerToggleSneakEvent e) {
        if (e.getPlayer().equals(target)) e.setCancelled(true);
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getPlayer().equals(target)) e.setCancelled(true);
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getPlayer().equals(target) && e.getCause() != TeleportCause.PLUGIN) e.setCancelled(true);
    }

    @SubscribeEvent
    public void onTaxiStart(TaxiStartEvent e) {
        if (skill.isRunning()) {
            if (e.getEntity().equals(target)) {
                e.setCancelled(true);
                e.setCancelMessage("§c이미 다른 플레이어가 납치 중인 대상입니다.");
            } else if (e.getKidnapper().getPlayer().equals(target)) {
                e.setCancelMessage("§c지금 납치할 수 없는 대상입니다.");
            }
        }
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onPlayerHeld(PlayerItemHeldEvent e) {
        if (e.getPlayer().equals(getPlayer()) && skill.isRunning()) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onMurder(MurderEvent e) {
        if (skill.isRunning()) {
            if (e.getTarget().equals(getParticipant()) || e.getTarget().getPlayer().equals(target)) {
                e.setCancelled(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        NMS.broadcastEntityEffect(e.getTarget().getPlayer(), (byte) 35);
                    }
                }.runTaskLater(AbilityWar.getPlugin(), 3L);
            }
        }
    }

    @SubscribeEvent(ignoreCancelled = true)
    public void onArrowKill(ArrowKillEvent e) {
        if (skill.isRunning()) {
            if (e.getTarget().equals(getParticipant()) || e.getTarget().getPlayer().equals(target)) {
                e.setCancelled(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        NMS.broadcastEntityEffect(e.getTarget().getPlayer(), (byte) 35);
                    }
                }.runTaskLater(AbilityWar.getPlugin(), 3L);
            }
        }
    }

    int taxing = 0;

    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        final MurderMystery murderMystery = (MurderMystery) getGame();
        if (Items.isGold(e.getOffHandItem()) && murderMystery.consumeGold(getParticipant(), 10)) {
            e.setCancelled(true);
            new AbilityTimer(4 * taxing) {
                @Override
                protected void onStart() {
                    boosting = true;
                }

                @Override
                protected void onEnd() {
                    boosting = false;
                }

                @Override
                protected void onSilentEnd() {
                    boosting = false;
                }
            }.start();
        }
    }

    @Override
    protected void onUpdate(Update update) {
        super.onUpdate(update);
        if (update == Update.RESTRICTION_CLEAR) {
            NMS.sendTitle(getPlayer(), "§e직업§f: §a택시기사", "§f시민을 안전하게 운반해주세요!", 10, 80, 10);
            new AbilityTimer(1) {
                @Override
                protected void run(int count) {
                }

                @Override
                protected void onEnd() {
                    NMS.clearTitle(getPlayer());
                }
            }.setInitialDelay(TimeUnit.SECONDS, 5).start();
            passive.start();
        }
    }

    long latest = 0;

    @SubscribeEvent(ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        final MurderMystery murderMystery = (MurderMystery) getGame();
        long current = System.currentTimeMillis();
        if (predicate.test(e.getRightClicked()) && e.getPlayer().equals(getPlayer()) && Items.isGold(getPlayer().getInventory().getItemInMainHand()) && !skill.isRunning() && !cooldown.isCooldown() && current - latest >= 250) {
            if (e.getRightClicked().getLocation().distanceSquared(e.getPlayer().getLocation()) <= RANGE.getValue() * RANGE.getValue()) {
                TaxiStartEvent startEvent = new TaxiStartEvent(e.getRightClicked());
                Bukkit.getPluginManager().callEvent(startEvent);
                if (!startEvent.isCancelled()  && murderMystery.consumeGold(getParticipant(), TAXI_COST.getValue())) {
                    this.target = e.getRightClicked();
                    skill.start();
                    e.setCancelled(true);
                    latest = current;
                    taxing++;
                } else {
                    getPlayer().sendMessage(String.valueOf(startEvent.cancelMessage));
                }
            }
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    public void onInteract(PlayerInteractEvent e) {
        if (Items.isGold(e.getPlayer().getInventory().getItemInMainHand())) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                final MurderMystery murderMystery = (MurderMystery) getGame();
                long current = System.currentTimeMillis();
                if (skill.isRunning() && current - latest >= 250) {
                    skill.stop(false);
                    latest = current;
                }
            } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                final MurderMystery murderMystery = (MurderMystery) getGame();
                if (murderMystery.consumeGold(getParticipant(), ARROW_COST.getValue())) {
                    if (!addArrow()) {
                        murderMystery.addGold(getParticipant());
                    } else {
                        if (!hasBow()) {
                            getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
                        }
                    }
                }
            }
        }
    }

    public static class TaxiEvent extends EntityEvent {

        private static final HandlerList handlers = new HandlerList();

        public static HandlerList getHandlerList() {
            return handlers;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        private final Participant kidnapper;

        private TaxiEvent(TaxiDriver kidnap, Entity victim) {
            super(victim);
            this.kidnapper = kidnap.getParticipant();
        }

        public Participant getKidnapper() {
            return kidnapper;
        }

    }

    public class TaxiStartEvent extends TaxiEvent implements Cancellable {

        private TaxiStartEvent(Entity victim) {
            super(TaxiDriver.this, victim);
        }

        private boolean cancelled = false;
        private String cancelMessage = null;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public void setCancelMessage(String cancelMessage) {
            this.cancelMessage = cancelMessage;
        }
    }

    public class TaxiEndEvent extends TaxiEvent {

        private TaxiEndEvent(Entity victim) {
            super(TaxiDriver.this, victim);
        }

    }
}
