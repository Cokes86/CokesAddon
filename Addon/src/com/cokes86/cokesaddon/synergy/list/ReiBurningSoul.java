package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.synergy.CokesSynergy.Config.Condition;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import com.google.common.collect.ImmutableList;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

//레이 + 임페르노
@AbilityManifest(name = "레이<버닝소울>", rank = Rank.S, species = Species.DEMIGOD, explain = {
        "패시브 - 타오르는 영혼: 체력 회복량이 2배가 되지만, 항상 불타는 상태입니다.",
        "  받는 화염 피해가 (§c버닝 소울§f × $[FIRE_DAMAGE_INCREMENT])% 증가합니다.",
        "적 처치 - 수거: §c버닝 소울§f을 하나 얻습니다. 최대 $[MAX_SOUL]개까지 보유 가능합니다.",
        "근거리 공격 - 한 맺힌 힘: 대상을 §7(§e1 + §c버닝 소울§7)§f초간 추가로 불태웁니다.",
        "  만약, 대상이 이미 $[DAMAGE_INCREMENT_PREDICATE] 이상 발화중이라면, 추가로 발화하지 않고",
        "  남은 발화시간에 비례하여 추가 피해를 준 후 불을 끕니다.",
        "  추가 피해량: 초과한 시간(틱) * 0.06",
        "철괴 우클릭 - 영혼 해방: 자신의 최대 체력의 $[SOUL_LIBERATION_COST]%을 지불하고,",
        "  $[TEMPORARY_SOUL_DURATION]간 유지되는 임시 §c버닝 소울§f을 하나 얻습니다.",
        "  이후, 모든 §c버닝 소울§f을 해방해 $[LIBERATION_SOUL_RANGE]블럭 이내 적 중 하나를 추격해",
        "  $[LIBERATION_SOUL_DAMAGE]의 마법 대미지를 입힌 뒤, $[LIBERATION_SOUL_FIRE_DURATION]간 추가로 불태웁니다.",
        "  영혼 해방으로 얻은 §c버닝 소울§f은 수거의 §c버닝 소울§f의 제약에 속하지 않습니다.",
        "치명적 공격을 받을 시 - 라스트 버닝: §c버닝 소울§f이 1개 이상 존재하면",
        "  §c버닝 소울§f 1개가 해방하여 $[LAST_BURNING_RANGE]블럭 이내 플레이어를",
        "  $[LAST_BURNING_FIRE_DURATION]간 추가로 불태운 다음 자신의 체력이 1로 변경됩니다."
}, summarize = {
        "항상 불타고 화염 피해가 증가하는 대신, 체력 회복 속도가 빠릅니다."
})
public class ReiBurningSoul extends CokesSynergy implements ActiveHandler {
    //Config
    private static final Config<Double> FIRE_DAMAGE_INCREMENT = Config.of(ReiBurningSoul.class, "fire-damage-increment", 10d, FunctionalInterfaceUnit.positive(),
            "# 버닝 소울 개당 증가할 화염피해 증가량", "# 기본값: 10.0(%)");
    private static final Config<Integer> MAX_SOUL = Config.of(ReiBurningSoul.class, "max-soul", 5, FunctionalInterfaceUnit.positive(),
            "# 수거로 획득 가능한 최대 버닝 소울의 개수", "# 기본값: 5(개)");
    private static final Config<Integer> DAMAGE_INCREMENT_PREDICATE = Config.of(ReiBurningSoul.class, "damage-increment-predicate", 5, Condition.TIME,
            "# 발화 시간에 따른 추가 대미지 조건", "# 기본값: 5(초)");
    private static final Config<Double> SOUL_LIBERATION_COST = Config.of(ReiBurningSoul.class, "soul-liveration-cost", 10d, FunctionalInterfaceUnit.between(0d,100d,false),
            "# 영혼 해방 시 필요한 체력 코스트", "# 기본값: 10.0(%)");
    private static final Config<Integer> TEMPORARY_SOUL_DURATION = Config.of(ReiBurningSoul.class, "temporary-soul-duration", 7, Condition.TIME,
            "# 임시 버닝 소울의 지속시간", "# 기본값: 7(초)");
    private static final Config<Integer> LIBERATION_SOUL_RANGE = Config.of(ReiBurningSoul.class, "liveration-soul-range", 10, FunctionalInterfaceUnit.positive(),
            "# 해방된 버닝 소울이 적을 인식하는 범위", "# 기본값: 5(블럭)");
    private static final Config<Float> LIBERATION_SOUL_DAMAGE = Config.of(ReiBurningSoul.class, "liveration-soul-damage", 3f, FunctionalInterfaceUnit.positive(),
            "# 해방된 버닝 소울 적중시 대미지", "# 기본값: 3.0");
    private static final Config<Integer> LIBERATION_SOUL_FIRE_DURATION = Config.of(ReiBurningSoul.class, "liveration-soul-fire-duration", 2, Condition.TIME,
            "# 해방된 버닝 소울 적중시 추가할 발화 시간", "# 기본값: 2(초)");
    private static final Config<Integer> LAST_BURNING_RANGE = Config.of(ReiBurningSoul.class, "last-burning-range", 5, FunctionalInterfaceUnit.positive(),
            "# 라스트 버닝에서 발화를 적용할 플레이어 범위", "# 기본값: 5(블럭)");
    private static final Config<Integer> LAST_BURNING_FIRE_DURATION = Config.of(ReiBurningSoul.class, "last-burning-fire-duration", 5, Condition.TIME,
            "# 라스트 버닝 추가 발화 시간", "# 기본값: 5(초)");

    private final BurningSoul burningSoul = new BurningSoul();
    private final List<DamageCause> damageCauseList = ImmutableList.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.HOT_FLOOR);
    private final Predicate<Player> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (!getGame().isParticipating(entity.getUniqueId())
                || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
            return false;
        }
        if (getGame() instanceof Teamable) {
            final Teamable teamGame = (Teamable) getGame();
            final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(getPlayer().getUniqueId());
            if (participant != null) {
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return true;
    };

    public ReiBurningSoul(Participant participant) {
        super(participant);
    }

    @Override  //영혼 해방
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            double max_health = AttributeUtil.getMaxHealth(getPlayer());
            double health = getPlayer().getHealth();
            if (health > max_health / SOUL_LIBERATION_COST.getValue()) {
                Healths.setHealth(getPlayer(), health - max_health/SOUL_LIBERATION_COST.getValue());
                burningSoul.addBurningSoul(true);

                List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), LIBERATION_SOUL_RANGE.getValue(), LIBERATION_SOUL_RANGE.getValue(), predicate);
                if (nearby.size() <= 0) {
                    getPlayer().sendMessage("[레이<버닝소울>] 주변 플레이어가 없어 버닝 소울을 해방할 수 없습니다.");
                    return false;
                }
                int soul = burningSoul.soul, temp_soul = burningSoul.temporary_soul.size();
                for (int i = 0 ; i < soul; i++) {
                    Player pick = new Random().pick(nearby);
                    new LiberatedBurningSoul(pick, false, 0);
                    burningSoul.removeBurningSoul();
                }
                for (int i = 0 ; i < temp_soul; i++) {
                    Player pick = new Random().pick(nearby);
                    new LiberatedBurningSoul(pick, true, burningSoul.temporary_soul.get(0));
                    burningSoul.removeBurningSoul();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            burningSoul.start();
        }
    }

    @SubscribeEvent(childs = {EntityDamageByBlockEvent.class})
    public void onEntityDamage(EntityDamageEvent e) {
        if (damageCauseList.contains(e.getCause())) { //타오르는 영혼[화염피해 추댐]
            e.setDamage(e.getDamage() * (1 + burningSoul.getTotalBurningSoul()* FIRE_DAMAGE_INCREMENT.getValue()));
        }

        //라스트 버닝
        if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && burningSoul.getTotalBurningSoul() > 0) {
            e.setDamage(0);
            getPlayer().setHealth(1);
            burningSoul.removeBurningSoul();
            List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), LAST_BURNING_RANGE.getValue(), LAST_BURNING_RANGE.getValue(), predicate);
            for (Player player : nearby) {
                player.setFireTicks(player.getFireTicks() + LAST_BURNING_FIRE_DURATION.getValue()*20);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        //한 맺힌 힘
        if (e.getDamager().equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            if (e.getEntity().getFireTicks() >= DAMAGE_INCREMENT_PREDICATE.getValue()) {
                int firetick = e.getEntity().getFireTicks();
                e.getEntity().setFireTicks(0);
                double damageIncrement = (firetick - DAMAGE_INCREMENT_PREDICATE.getValue()*20) * 0.06;
                e.setDamage(e.getDamage() + damageIncrement);
            } else {
                e.getEntity().setFireTicks(e.getEntity().getFireTicks() + 20 + burningSoul.getTotalBurningSoul()*20);
            }
        }
    }

    @SubscribeEvent(onlyRelevant = true)  //타오르는 영혼[회복량 2배]
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        e.setAmount(e.getAmount() * 2.0);
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {  //수거
        if (!e.getParticipant().equals(getParticipant()) && e.getPlayer().getKiller() != null && e.getPlayer().getKiller().equals(getPlayer())) {
            burningSoul.addBurningSoul(false);
        }
    }

    private class BurningSoul extends AbilityTimer {
        private int soul = 0;
        private final List<Long> temporary_soul = new ArrayList<>();
        private final ActionbarChannel channel = newActionbarChannel();

        public BurningSoul() {
            super();
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void run(int count) {
            getPlayer().setFireTicks(21);
            temporary_soul.removeIf(mill -> mill + TEMPORARY_SOUL_DURATION.getValue()*1000 >= System.currentTimeMillis());
            channel.update("§c버닝 소울: "+soul + "(+ "+temporary_soul.size()+")");
        }

        public void addBurningSoul(boolean temporary) {
            if(temporary) temporary_soul.add(System.currentTimeMillis());
            else if (soul < MAX_SOUL.getValue()) soul += 1;
        }

        public void addBurningSoul(long time) {
            temporary_soul.add(time);
        }

        public void removeBurningSoul() {
            if (soul == 0) temporary_soul.remove(0);
            else soul -= 1;
        }

        public int getTotalBurningSoul() {
            return soul + temporary_soul.size();
        }
    }

    private class LiberatedBurningSoul extends AbilityTimer {
        private final Player target;
        private Location lastLocation;
        private final LiberatedBurningSoulEntity entity;
        private final boolean temporary;
        private final long period;
        private int tick = 0;

        public LiberatedBurningSoul(Player target, boolean temporary, long period) {
            super(60);  // 1.5초 공격, 1.5초 복귀
            setPeriod(TimeUnit.TICKS, 1);

            this.target = target;
            this.lastLocation = getPlayer().getEyeLocation().clone();
            this.entity = new LiberatedBurningSoulEntity(getPlayer().getWorld(), lastLocation.getX(), lastLocation.getY(), lastLocation.getZ());
            this.temporary = temporary;
            this.period = period;
        }

        @Override
        protected void run(int count) {
            Location newLocation;
            tick++;
            if (count < 30) {
                newLocation = lastLocation.add(target.getEyeLocation().clone().toVector().subtract(entity.getLocation().toVector()).multiply(30));
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
                    final double y = location.getY();
                    if (y < 0 || y > 256 || !location.getChunk().isLoaded()) {
                        stop(false);
                        return;
                    }
                    for (Damageable damageable : LocationUtil.getConflictingEntities(Player.class, getPlayer().getWorld(), entity.getBoundingBox(), predicate)) {
                        if (!getPlayer().equals(damageable)) {
                            Damages.damageMagic(damageable, getPlayer(), false, LIBERATION_SOUL_DAMAGE.getValue());
                            setCount(LIBERATION_SOUL_FIRE_DURATION.getValue()*20);
                            return;
                        }
                    }
                    ParticleLib.REDSTONE.spawnParticle(location, RGB.RED);
                }
            } else {
                newLocation = lastLocation.add(getPlayer().getEyeLocation().clone().toVector().subtract(entity.getLocation().toVector()).multiply(60));
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
                    final double y = location.getY();
                    if (y < 0 || y > 256 || !location.getChunk().isLoaded()) {
                        stop(false);
                        return;
                    }
                    ParticleLib.REDSTONE.spawnParticle(location, RGB.RED);
                }
            }
            lastLocation = newLocation;
        }

        @Override
        protected void onEnd() {
            if (temporary) burningSoul.addBurningSoul(period + tick * 50L);
            else burningSoul.addBurningSoul(false);
        }

        @Override
        protected void onSilentEnd() {
            super.onSilentEnd();
        }

        private class LiberatedBurningSoulEntity extends CustomEntity{

            public LiberatedBurningSoulEntity(World world, double x, double y, double z) {
                getGame().super(world, x, y, z);
            }

            @Override
            protected void onRemove() {
                LiberatedBurningSoul.this.stop(false);
            }

        }
    }
}
