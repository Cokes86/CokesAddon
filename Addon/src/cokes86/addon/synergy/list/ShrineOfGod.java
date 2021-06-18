package cokes86.addon.synergy.list;

import cokes86.addon.effect.list.ArmorBroken;
import cokes86.addon.synergy.CokesSynergy;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

@AbilityManifest(name = "신궁", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "§7활 조준 §8- §c드라그노프§f: 화살 장전 대신 특별한 투사체가 매우 빠르게 날아갑니다.",
        "  투사체에 맞은 적은 $[DRAGUNOV_DAMAGE]의 대미지를 입고 10초간 장비파괴 효과를 받습니다.",
        "  활을 조준하는 동안 이동속도가 매우 느려지며, 조준하는 시간이 3초가 넘을 시",
        "  조준 시간에 비례하여 최대 $[DRAGUNOV_MULTIPLY]배의 추가대미지를 줍니다.",
        "  드라그노프는 $[DRAGUNOV_RELOAD_TIME]마다 1개씩 준비되며, 최대 $[DRAGUNOV_MAX]개 가질 수 있습니다.",
        "§7철괴 우클릭 §8- §c델타 필드§f: $[DELTA_FIELD_DURATION]간 드라그노프의 대미지가 30% 감소합니다.",
        "  드라그노프 사용 시 범위 $[DELTA_FIELD_RANGE]블럭 이내 랜덤한 5인에게 자동으로 조준되어 발사됩니다.",
        "  대신, 자신이 받는 대미지가 1.25베 증가합니다. $[DELTA_FIELD_COOLDOWN]",
        "§7상태이상 §8- §c장비파괴§f: 갑옷의 방어와 방어 강도를 1 감소시킵니다."
})
public class ShrineOfGod extends CokesSynergy implements ActiveHandler {
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel aiming_channel = newActionbarChannel();
    private final Aiming aiming = new Aiming();
    private final ReloadTimer reloadTimer = new ReloadTimer();

    private static final Config<Double> DRAGUNOV_DAMAGE = new Config<>(ShrineOfGod.class, "드라그노프_대미지", 7.0, a -> a > 0);
    private static final Config<Double> DRAGUNOV_MULTIPLY = new Config<>(ShrineOfGod.class, "드라그노프_추가대미지_배율", 1.5, a -> a > 1);
    private static final Config<Integer> DRAGUNOV_RELOAD_TIME = new Config<>(ShrineOfGod.class, "드라그노프_장전시간", 10, Config.Condition.TIME);
    private static final Config<Integer> DRAGUNOV_MAX = new Config<>(ShrineOfGod.class, "드라그노프_최대_개수", 5, a -> a > 0);
    private static final Config<Integer> DELTA_FIELD_DURATION = new Config<>(ShrineOfGod.class, "델타_필드_지속시간", 15, Config.Condition.TIME);
    private static final Config<Integer> DELTA_FIELD_COOLDOWN = new Config<>(ShrineOfGod.class, "델타_필드_쿨타임", 60, Config.Condition.COOLDOWN);
    private static final Config<Integer> DELTA_FIELD_RANGE = new Config<>(ShrineOfGod.class, "델타_필드_범위", 30, a -> a > 0);

    private int dragunov_count = DRAGUNOV_MAX.getValue();

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())
                    || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                return false;
            }
            if (getGame() instanceof Teamable) {
                final Teamable teamGame = (Teamable) getGame();
                final AbstractGame.Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(getPlayer().getUniqueId());
                if (participant != null) {
                    return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
                }
            }
        }
        return true;
    };

    private final Cooldown deltaField = new Cooldown( DELTA_FIELD_COOLDOWN.getValue(), "델타 필드", CooldownDecrease._75);
    private final Duration duration = new Duration(DELTA_FIELD_DURATION.getValue(), deltaField, "델타 필드") {
        @Override
        protected void onDurationProcess(int i) {

        }
    };

    public ShrineOfGod(AbstractGame.Participant participant) {
        super(participant);
        reloadTimer.register();
        aiming.register();
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            reloadTimer.start();
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getPlayer() == getPlayer() && e.getMaterial() == MaterialX.BOW.getMaterial()) {
            aiming.start();
        }
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && duration.isRunning()) {
            e.setDamage(e.getDamage() * 1.25);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);
    }

    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
        onEntityDamage(e);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType ==ClickType.RIGHT_CLICK) {
            if (!deltaField.isCooldown() && !duration.isDuration()) {
                return duration.start();
            }
        }
        return false;
    }

    class ReloadTimer extends AbilityTimer {
        private int reload_time = 0;
        private final ProgressBar progressBar = new ProgressBar(DRAGUNOV_RELOAD_TIME.getValue() * 20, 15);
        private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel prograss = newActionbarChannel();

        public ReloadTimer() {
            super(TaskType.INFINITE, -1);
            setPeriod(TimeUnit.TICKS, 1);
        }

        public void run(int i) {
            String bullet = ChatColor.AQUA.toString() + dragunov_count +"/" + DRAGUNOV_MAX.getValue();
            if (dragunov_count < DRAGUNOV_MAX.getValue()) {
                progressBar.step();
                reload_time++;
                prograss.update(bullet + " " + progressBar);

                if (reload_time == DRAGUNOV_RELOAD_TIME.getValue() * 20) {
                    dragunov_count++;
                    reload_time = 0;
                    progressBar.setStep(0);
                    prograss.update(null);
                }
            } else {
                prograss.update(bullet);
            }
        }
    }

    class Aiming extends AbilityTimer implements Listener {
        public Aiming() {
            super(TaskType.INFINITE, -1);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
            aiming_channel.update("조준");
        }

        @Override
        protected void run(int c) {
            PotionEffects.SLOW.addPotionEffect(getPlayer(), 2, 5, true);
            getPlayer().setVelocity(getPlayer().getVelocity().setX(0).setY(Math.min(0, getPlayer().getVelocity().getY())).setZ(0));
        }

        @Override
        protected void onEnd() {
            HandlerList.unregisterAll(this);
            aiming_channel.update(null);
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
            aiming_channel.update(null);
        }

        @EventHandler(ignoreCancelled = true)
        public void onProjectileLaunch(ProjectileLaunchEvent e) {
            if (NMS.isArrow(e.getEntity()) && e.getEntity().getShooter() != null && e.getEntity().getShooter().equals(getPlayer())) {
                e.setCancelled(true);
                if (dragunov_count > 0) {
                    this.stop(true);
                    if (getPlayer().getGameMode() != GameMode.CREATIVE) {
                        if (getPlayer().getInventory().getItemInMainHand().getType() == Material.BOW && !getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.ARROW_INFINITE)) {
                            ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
                        } else if (getPlayer().getInventory().getItemInOffHand().getType() == Material.BOW && !getPlayer().getInventory().getItemInOffHand().containsEnchantment(Enchantment.ARROW_INFINITE)) {
                            ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
                        }
                    }
                    final Arrow arrow = (Arrow) e.getEntity();
                    int level = 0;
                    if (getPlayer().getInventory().getItemInMainHand().getType() == Material.BOW) {
                        level = getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
                    }
                    else if (getPlayer().getInventory().getItemInOffHand().getType() == Material.BOW) {
                        level = getPlayer().getInventory().getItemInOffHand().getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
                    }
                    if (duration.isRunning()) {
                        ArrayList<Player> players = (ArrayList<Player>) LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), DELTA_FIELD_RANGE.getValue(), DELTA_FIELD_RANGE.getValue(), predicate);
                        Collections.shuffle(players);

                        for (int a = 0; a < 5; a++) {
                            if (a >= players.size()) continue;
                            Player target = players.get(a);
                            Vector velocity = target.getLocation().subtract(getPlayer().getLocation()).toVector().normalize();
                            new DragunovBullet(getPlayer(), arrow.getLocation(), velocity, getCount(), level, RGB.TEAL, true).start();
                        }
                    } else {
                        new DragunovBullet(getPlayer(), arrow.getLocation(), arrow.getVelocity(), getCount(), level, RGB.TEAL, false).start();
                    }
                    dragunov_count--;
                } else {
                    getPlayer().sendMessage("장전중입니다.");
                }
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent e) {
            if (e.getPlayer() == ShrineOfGod.this.getPlayer() && e.getMaterial() != MaterialX.BOW.getMaterial()) {
                this.stop(true);
            }
        }

        @EventHandler
        public void onPlayerItemHeld(PlayerItemHeldEvent e) {
            if (e.getPlayer() == getPlayer()) {
                if (e.getPlayer().getInventory().getItemInOffHand().getType() != Material.BOW) {
                    this.stop(true);
                }
            }
        }

        @EventHandler
        public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
            if (e.getPlayer() == getPlayer()) {
                if (e.getMainHandItem() != null && e.getMainHandItem().getType() != Material.BOW) {
                    this.stop(true);
                } else if (e.getOffHandItem() != null && e.getOffHandItem().getType() != Material.BOW) {
                    this.stop(true);
                }
            }
        }
    }

    class DragunovBullet extends AbilityTimer {
        private final Material GLASS_PANE = ServerVersion.getVersion() > 12 ? Material.valueOf("GLASS_PANE") : Material.valueOf("THIN_GLASS");
        private final LivingEntity shooter;
        private final AbstractGame.CustomEntity entity;
        private final Vector forward;
        private final int powerEnchant, aimingTime;
        private final Predicate<Entity> predicate;
        private final boolean delta;

        private final RGB color;

        private DragunovBullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int aimingTime, int powerEnchant, RGB color, boolean delta) {
            super(160);
            setPeriod(TimeUnit.TICKS, 1);
            this.shooter = shooter;
            this.entity = new DragunovArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
            this.forward = arrowVelocity.multiply(10);
            this.powerEnchant = powerEnchant;
            this.color = color;
            this.aimingTime = aimingTime;
            this.lastLocation = startLocation;
            this.predicate = entity -> {
                if (entity.equals(shooter)) return false;
                if (entity instanceof Player) {
                    if (!getGame().isParticipating(entity.getUniqueId())
                            || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                            || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                        return false;
                    }
                    if (getGame() instanceof Teamable) {
                        final Teamable teamGame = (Teamable) getGame();
                        final AbstractGame.Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
                        if (participant != null) {
                            return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
                        }
                    }
                }
                return true;
            };
            this.delta = delta;
        }

        private Location lastLocation;

        @Override
        protected void run(int i) {
            final Location newLocation = lastLocation.clone().add(forward);
            for (Iterator<Location> iterator = new Iterator<Location>() {
                private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.35);
                private final int amount = (int) (vectorBetween.length() / .35);
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < amount;
                }

                @Override
                public Location next() {
                    if (cursor >= amount) throw new NoSuchElementException();
                    cursor++;
                    return lastLocation.clone().add(unit.clone().multiply(cursor));
                }
            }; iterator.hasNext(); ) {
                final Location location = iterator.next();
                entity.setLocation(location);
                if (!isRunning()) {
                    return;
                }
                final Block block = location.getBlock();
                final Material type = block.getType();
                final double y = location.getY();
                if (y < 0 || y > 256 || !location.getChunk().isLoaded()) {
                    stop(false);
                    return;
                }
                if (type.isSolid()) {
                    if (ItemLib.STAINED_GLASS.compareType(type) || Material.GLASS == type || ItemLib.STAINED_GLASS_PANE.compareType(type) || type == GLASS_PANE) {
                        block.breakNaturally();
                        SoundLib.BLOCK_GLASS_BREAK.playSound(block.getLocation(), 3, 1);
                    } else if (RayTrace.hitsBlock(Objects.requireNonNull(location.getWorld()), lastLocation.getX(), lastLocation.getY(), lastLocation.getZ(), location.getX(), location.getY(), location.getZ())) {
                        stop(false);
                        return;
                    }
                }
                for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
                    if (!shooter.equals(damageable)) {
                        Damages.damageArrow(damageable, shooter, (delta ? 0.7f : 1f)*(float)Math.max(1, Math.min(DRAGUNOV_MULTIPLY.getValue(), 1 + (aimingTime-60) / 200.0f))* (float)EnchantLib.getDamageWithPowerEnchantment(Math.min((forward.getX() * forward.getX()) + (forward.getY() * forward.getY()) + (forward.getZ() * forward.getZ()) / 10.0, DRAGUNOV_DAMAGE.getValue()), powerEnchant));
                        if (getGame().isParticipating(damageable.getUniqueId())) {
                            ArmorBroken.apply(getGame().getParticipant(damageable.getUniqueId()), TimeUnit.SECONDS, 10);
                        }
                        stop(false);
                        return;
                    }
                }
                ParticleLib.REDSTONE.spawnParticle(location, color);
            }
            lastLocation = newLocation;
        }

        @Override
        protected void onEnd() {
            entity.remove();
        }

        @Override
        protected void onSilentEnd() {
            entity.remove();
        }

        public class DragunovArrowEntity extends AbstractGame.CustomEntity implements Deflectable {

            public DragunovArrowEntity(World world, double x, double y, double z) {
                getGame().super(world, x, y, z);
            }

            @Override
            public Vector getDirection() {
                return forward.clone();
            }

            @Override
            public void onDeflect(AbstractGame.Participant deflector, Vector newDirection) {
                stop(false);
                final Player deflectedPlayer = deflector.getPlayer();
                new DragunovBullet(deflectedPlayer, lastLocation, newDirection, aimingTime, powerEnchant, color, delta).start();
            }

            @Override
            public ProjectileSource getShooter() {
                return shooter;
            }

            @Override
            protected void onRemove() {
                DragunovBullet.this.stop(false);
            }

        }

    }
}
