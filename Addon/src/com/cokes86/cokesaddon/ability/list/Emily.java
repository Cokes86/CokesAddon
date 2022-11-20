package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

@AbilityManifest(name= "에밀리", rank= AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
        "§7패시브 §8-§c 알케미 마스터리§f: 각종 캡슐을 가지고 상대방을 혼란시킵니다.",
        "  자신의§c 알케미 마스터리§f 상태에 따라 능력의 효과가 뒤바뀝니다.",
        "§7검 들고 F키 §8-§c 알케미 캡슐§f: 자신이 바라보고 있는 방향으로 구체를 날려",
        "  블럭이나 플레이어에 맞출 시§a 알케미 에리어§f를 $[ALCHEMY_CAPSULE_DURATION]간 생성시킵니다.",
        "  구체가 플레이어나 땅의 맞은 위치의 $[ALCHEMY_CAPSULE_AREA_RANGE]블럭 이내 플레이어가 있을 경우 ",
        "  $[ALCHEMY_CAPSULE_DAMAGE]의 대미지를 줍니다. $[ALCHEMY_CAPSULE_COOL]",
        "  사용 시 약간의 경직이 생깁니다.",
        "§a알케미 에리어§f: 해당 영역에 존재하는 에밀리를 제외한 플레이어",
        "  §c 알케미 마스터리§f 상태에 따라 아래의 효과를 받습니다.",
        "  §4화상§f: 에리어에 불장판을 설치합니다. 에밀리는 에리어 내에서 화상 대미지를 입지 않습니다.",
        "    영역 내 화상 대미지가 $[ALCHEMY_AREA_BONUS_DAMAGE] 증가합니다.",
        "  §7둔화§f: 0.5초마다 쿨타임이 $[ALCHEMY_AREA_SLOWDOWN_COOL] 증가하고",
        "    빙결 효과 1.5초를 각 플레이어마다 최대 $[ALCHEMY_AREA_FROST_MAX_COUNT]번 받습니다.",
        "  §a폭발§f: 1초마다 $[ALCHEMY_AREA_EXPLOSION_DAMAGE]의 대미지를 주는 폭발을 일으킵니다.",
        "§7철괴 우클릭 §8-§c 알케미 체인지§f: 자신의§c 알케미 마스터리§f 상태를 바꿉니다. §c쿨타임 §7: §f0.25초"
}, summarize = {
        "철괴 우클릭으로 §c알케미 마스터리§f를 변경합니다.",
        "검 들고 F키를 누르면 §c알케미 캡슐§f이 날라가 플레이어, 땅에 맞으면",
        "주변 반경에 대미지를 주고 §c알케미 마스터리§f에 맞는 §a알케미 에리어§f를 만듭니다.",
        "  §4화상§f: 불장판, 영역 내 에밀리 화상대미지 무시",
        "  §7둔화§f: 0.5초마다 쿨타임 증가, 1.5초마다 빙결 효과 (최대 $[ALCHEMY_AREA_FROST_MAX_COUNT]번)",
        "  §a폭발§f: 1초마다 영역 내 폭발 대미지"
})
public class Emily extends CokesAbility implements ActiveHandler {
    private static final Set<Material> swords = CokesUtil.getSwords();

    private static final Config<Integer> ALCHEMY_CAPSULE_COOL = Config.of(Emily.class, "알케미_캡슐_쿨타임", 15, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
    private static final Config<Integer> ALCHEMY_CAPSULE_DURATION = Config.of(Emily.class, "알케미_캡슐_지속시간", 7, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
    private static final Config<Integer> ALCHEMY_AREA_SLOWDOWN_COOL = Config.of(Emily.class, "알케미_에리어_둔화_쿨타임_증가량", 1, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
    private static final Config<Integer> ALCHEMY_AREA_FROST_MAX_COUNT = Config.of(Emily.class, "알케미_에리어_둔화_빙결_횟수", 3, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_CAPSULE_POWER = Config.of(Emily.class, "알케미_캡슐_힘", 1.35, new String[] {
            "# 알케미 캡슐이 날아가는 힘을 조절합니다.",
            "# 1을 기준으로 값이 커질수록 더욱 강한 힘으로 날아갑니다.",
            "# 기본값: 1.35"
    }, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_CAPSULE_SPEED = Config.of(Emily.class, "알케미_캡슐_속도", 1.5, new String[] {
            "# 알케미 캡슐이 날아가는 속도를 조절합니다.",
            "# 1을 기준으로 값이 커질수록 더욱 빠르게 날아갑니다.",
            "# 기본값: 1.5"
    }, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_CAPSULE_DAMAGE = Config.of(Emily.class, "알케미_캡슐_대미지", 5.0, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_CAPSULE_AREA_RANGE = Config.of(Emily.class, "알케미_캡슐_에리어_범위", 5.5, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_AREA_EXPLOSION_DAMAGE = Config.of(Emily.class, "알케미_에리어_폭발_대미지", 3.5, FunctionalInterfaces.positive());
    private static final Config<Double> ALCHEMY_AREA_BONUS_DAMAGE = Config.of(Emily.class, "알케미_에리어_화상_추가대미지", 3.0, FunctionalInterfaces.positive());

    private final Cooldown cooldown = new Cooldown(ALCHEMY_CAPSULE_COOL.getValue(), CooldownDecrease._25);
    private final AbilityTimer alchemyChangeCooldown = new AbilityTimer(1){}.setPeriod(TimeUnit.TICKS, 4);

    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();

    private CapsuleMastery capsuleType = null;

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

    public Emily(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (!alchemyChangeCooldown.isRunning()) {
                capsuleType = capsuleType.next();
                channel.update("알케미 마스터리: " + capsuleType.getName());
                alchemyChangeCooldown.start();
            }
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR && capsuleType == null) {
            capsuleType = CapsuleMastery.FIRE;
            channel.update("알케미 마스터리: " + capsuleType.getName());
        }
        super.onUpdate(update);
    }

    public static Vector getForwardVector(Location location) {
        final float yaw = location.getYaw(), pitch = location.getPitch();
        final double radYaw = Math.toRadians(yaw), radPitch = Math.toRadians(pitch);
        final double cosPitch = FastMath.cos(radPitch);

        final double x = -FastMath.sin(radYaw) * cosPitch;
        final double y = -FastMath.sin(radPitch);
        final double z = FastMath.cos(radYaw) * cosPitch;

        return new Vector(x, y, z).normalize();
    }

    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() != null && swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
            if (!cooldown.isCooldown()) {
                new AbilityTimer(7) {
                    @Override
                    public void run(int i) {
                        getPlayer().setVelocity(new Vector(0,0,0));
                        if (i == getMaximumCount()/2) {
                            Vector velocity = getForwardVector(getPlayer().getLocation().clone());
                            Location startArrow = getPlayer().getLocation().clone().add(velocity.multiply(.25)).add(0, getPlayer().getEyeHeight(), 0);

                            new AlchemyCapsule(getPlayer(), startArrow).start();
                            SoundLib.ENTITY_SPLASH_POTION_THROW.playSound(getPlayer());
                            cooldown.start();
                        }
                    }
                }.setPeriod(TimeUnit.TICKS, 1).start();
                AbilityActiveSkillEvent event = new AbilityActiveSkillEvent(this, e.getOffHandItem().getType(), null);
                Bukkit.getPluginManager().callEvent(event);
            }
            e.setCancelled(true);
        }
    }

    private class AlchemyCapsule extends AbilityTimer implements Listener {
        private final LivingEntity shooter;
        private final AbstractGame.CustomEntity entity;
        private final double sx, sy, sz, vx, vy, vz;

        private AlchemyCapsule(LivingEntity shooter, Location startLocation) {
            super(TaskType.INFINITE, -1);
            this.shooter = shooter;
            this.entity = new CapsuleEntity(getPlayer().getWorld(), startLocation.getX(), startLocation.getY(),
                    startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
            setPeriod(TimeUnit.TICKS, 1);
            sx = startLocation.getX(); sy = startLocation.getY(); sz = startLocation.getZ();
            Vector direction = shooter.getLocation().getDirection();
            vx = direction.getX(); vy = direction.getY(); vz = direction.getZ();
        }

        private class CapsuleEntity extends AbstractGame.CustomEntity {
            public CapsuleEntity(World world, double x, double y, double z) {
                getGame().super(world, x, y, z);
            }

            @Override
            protected void onRemove() {
                AlchemyCapsule.this.stop(false);
            }
        }

        @Override
        protected void run(int i) {
            double m = ALCHEMY_CAPSULE_POWER.getValue();
            double t = i * ALCHEMY_CAPSULE_SPEED.getValue();
            Location before = entity.getLocation().clone();
            Location after = new Location(entity.getWorld(),
                    sx + vx * m * t,
                    sy + vy * m * t - (0.04 * t * t),
                    sz + vz * m * t);
            for (Location location : Line.between(before, after, 5).toLocations(before)) {
                entity.setLocation(location);
                ParticleLib.REDSTONE.spawnParticle(location, RGB.of(150,20,100));

                Material type = entity.getLocation().getBlock().getType();
                if (type.isSolid()) {
                    stop(false);
                    new AlchemyArea(ALCHEMY_CAPSULE_DURATION.getValue(), entity.getLocation(), capsuleType).start();
                    for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, entity.getLocation(), ALCHEMY_CAPSULE_AREA_RANGE.getValue(), ALCHEMY_CAPSULE_AREA_RANGE.getValue(), predicate)) {
                        damageable.damage(ALCHEMY_CAPSULE_DAMAGE.getValue(), shooter);
                    }
                    SoundLib.ENTITY_SPLASH_POTION_BREAK.playSound(entity.getLocation().getBlock().getLocation());
                    return;
                }

                for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
                    if (!shooter.equals(damageable) && !damageable.isDead()) {
                        damageable.damage(ALCHEMY_CAPSULE_DAMAGE.getValue(), shooter);
                        stop(false);
                        new AlchemyArea(ALCHEMY_CAPSULE_DURATION.getValue(), entity.getLocation(), capsuleType).start();
                        for (Damageable damageable2 : LocationUtil.getNearbyEntities(Damageable.class, entity.getLocation(), ALCHEMY_CAPSULE_AREA_RANGE.getValue(), ALCHEMY_CAPSULE_AREA_RANGE.getValue(), predicate)) {
                            if (damageable == damageable2) continue;
                            damageable2.damage(ALCHEMY_CAPSULE_DAMAGE.getValue(), shooter);
                        }
                        SoundLib.ENTITY_SPLASH_POTION_BREAK.playSound(entity.getLocation().getBlock().getLocation());
                        return;
                    }
                }
            }
        }
    }

    private class AlchemyArea extends AbilityTimer implements Listener {
        private final CapsuleMastery type;
        private final Location center;
        private final Map<Block, IBlockSnapshot> blockData = new HashMap<>();
        private final LinkedList<Block> fires = new LinkedList<>();
        private final Map<UUID, Integer> frostCount = new HashMap<>();

        public AlchemyArea(int duration, Location center, CapsuleMastery type) {
            super(TaskType.NORMAL,duration * 20 + 1);
            this.type = type;
            this.center = center;
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onStart() {
            AbilityWar.getPlugin().getServer().getPluginManager().registerEvents(this, AbilityWar.getPlugin());
            if (type == CapsuleMastery.FIRE) {
                int radius = (int) Math.round(ALCHEMY_CAPSULE_AREA_RANGE.getValue());
                for (int a = 1; a <= radius; a++) {
                    for (Block block : LocationUtil.getBlocks2D(center, a, true, false, true)) {
                        block = block.getWorld().getBlockAt(block.getX(), LocationUtil.getFloorYAt(block.getWorld(), center.getY(), block.getX(), block.getZ()), block.getZ());
                        Block belowBlock = block.getRelative(BlockFace.DOWN);
                        Material type = belowBlock.getType();

                        if (type == Material.WATER) {
                            blockData.putIfAbsent(belowBlock, daybreak.abilitywar.utils.base.minecraft.block.Blocks.createSnapshot(belowBlock));
                            belowBlock.setType(Material.LAVA);
                        } else {
                            blockData.putIfAbsent(belowBlock, daybreak.abilitywar.utils.base.minecraft.block.Blocks.createSnapshot(belowBlock));
                            BlockX.setType(belowBlock, MaterialX.MAGMA_BLOCK);

                            fires.add(block);
                            BlockX.setType(block, MaterialX.FIRE);
                        }
                    }
                }
            }
        }

        public void run(int i) {
            for (Location location : Circle.iteratorOf(center, ALCHEMY_CAPSULE_AREA_RANGE.getValue(), 20).iterable()) {
                location.setY(location.getY() + 0.1);
                ParticleLib.REDSTONE.spawnParticle(location, type.getRGB());
            }

            switch (type) {
                case FIRE: {
                    if (LocationUtil.isInCircle(center, getPlayer().getLocation(), ALCHEMY_CAPSULE_AREA_RANGE.getValue())) {
                        getPlayer().setFireTicks(0);
                    }
                    break;
                }
                case SLOWDOWN: {
                    if ( i % 10 == 0) {
                        for (Player player : LocationUtil.getNearbyEntities(Player.class, center, ALCHEMY_CAPSULE_AREA_RANGE.getValue(), 255, predicate)) {
                            AbstractGame.Participant participant = getGame().getParticipant(player);
                            if (participant.getAbility() != null) {
                                for (AbstractGame.GameTimer timer : participant.getAbility().getTimers()) {
                                    if (timer instanceof Cooldown.CooldownTimer) {
                                        timer.setCount(timer.getCount() + ALCHEMY_AREA_SLOWDOWN_COOL.getValue());
                                    }
                                }
                            }
                            int frost = frostCount.getOrDefault(player.getUniqueId(), 0);
                            if (frost < ALCHEMY_AREA_FROST_MAX_COUNT.getValue()) {
                                Frost.apply(participant, TimeUnit.TICKS, 30);
                                frostCount.put(player.getUniqueId(), frost +1);
                            }
                        }
                    }
                    break;
                }
                case EXPLOSION:{
                    if ( i % 20 == 0) {
                        SoundLib.ENTITY_GENERIC_EXPLODE.playSound(center);
                        double damage = ALCHEMY_AREA_EXPLOSION_DAMAGE.getValue();
                        for (Player player : LocationUtil.getNearbyEntities(Player.class, center, ALCHEMY_CAPSULE_AREA_RANGE.getValue(), 255, predicate)) {
                            Damages.damageMagic(player, getPlayer(), false, (float) damage);
                        }
                        for (Location location : LocationUtil.getRandomLocations(new Random(), center, ALCHEMY_CAPSULE_AREA_RANGE.getValue(), 5)) {
                            ParticleLib.EXPLOSION_LARGE.spawnParticle(location);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        protected void onSilentEnd() {
            for (Map.Entry<Block, IBlockSnapshot> entry : blockData.entrySet()) {
                Block key = entry.getKey();
                if (MaterialX.MAGMA_BLOCK.compare(key) || key.getType() == Material.LAVA || MaterialX.LAVA.compare(key) || key.getType() == Material.FIRE) {
                    entry.getValue().apply();
                }
            }
            for (Block block : fires) {
                if (block.getType() == Material.FIRE) block.setType(Material.AIR);
            }
            getPlayer().setFireTicks(0);
            HandlerList.unregisterAll(this);
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent e) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FIRE) {
                if (LocationUtil.isInCircle(center, e.getEntity().getLocation(), ALCHEMY_CAPSULE_AREA_RANGE.getValue())) {
                    if (e.getEntity().equals(getPlayer())) {
                        getPlayer().setFireTicks(0);
                        e.setCancelled(true);
                        e.setDamage(0.0);
                    } else {
                        e.setDamage(e.getDamage() + ALCHEMY_AREA_BONUS_DAMAGE.getValue());
                    }
                }
            }
        }
    }

    private enum CapsuleMastery {
        FIRE("§4화상", RGB.of(255,0,0)) {
            @Override
            public CapsuleMastery next() {
                return CapsuleMastery.SLOWDOWN;
            }
        }, SLOWDOWN("§7둔화", RGB.of(195,195,195)) {
            @Override
            public CapsuleMastery next() {
                return CapsuleMastery.EXPLOSION;
            }
        }, EXPLOSION("§a폭발", RGB.of(0,255,0)) {
            @Override
            public CapsuleMastery next() {
                return CapsuleMastery.FIRE;
            }
        };

        private final String name;
        private final RGB rgb;

        CapsuleMastery(String name, RGB rgb) {
            this.name = name;
            this.rgb = rgb;
        }

        public String getName() {
            return name;
        }
        public RGB getRGB() { return rgb; }

        public abstract CapsuleMastery next();
    }
}
