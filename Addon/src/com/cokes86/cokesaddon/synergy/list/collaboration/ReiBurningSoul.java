package com.cokes86.cokesaddon.synergy.list.collaboration;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.AttributeUtil;
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
        "패시브 - 타오르는 영혼: 체력 재생 속도가 2배가 되지만, 항상 불타는 상태입니다.",
        "  받는 화염 피해가 (§c버닝 소울§f × 10)% 증가합니다.",
        "적 처치 - 수거: §c버닝 소울§f을 하나 얻습니다. 최대 5개까지 보유 가능합니다.",
        "근거리 공격 - 한 맺힌 힘: 대상을 §7(§e1 + §c버닝 소울§7)§f초간 추가로 불태웁니다.",
        "  만약, 대상이 이미 5초 이상 발화중이라면, 추가로 발화하지 않고",
        "  남은 발화시간에 비례하여 추가 피해를 준 후 불을 끕니다.",
        "  추가 피해량: (남아있는 발화 시간(틱) - 80)/20 * 1.2",
        "철괴 우클릭 - 영혼 해방: 자신의 최대체력의 10%을 지불하고, 7초간 유지되는 임시 §c버닝 소울§f을 하나 얻습니다.",
        "  이후, 모든 §c버닝 소울§f을 해방해 근처 적을 3의 마법 대미지를 입힌 뒤, 2초간 추가로 불태웁니다.",
        "  영혼 해방으로 얻은 §c버닝 소울§f은 수거의 §c버닝 소울§f의 제약에 속하지 않습니다.",
        "치명적 공격을 받을 시 - 라스트 버닝: §c버닝 소울§f이 존재한 상태일 경우",
        "  §c버닝 소울§f을 1개가 해방하여 5블럭 이내 플레이어를 5초간 추가로 불태우고 체력이 1로 변경됩니다."
}, summarize = {
        "항상 불타고 화염 피해가 증가하는 대신, 체력 회복 속도가 빠릅니다."
})
public class ReiBurningSoul extends CokesSynergy implements ActiveHandler {
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

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            double max_health = AttributeUtil.getMaxHealth(getPlayer());
            double health = getPlayer().getHealth();
            if (health > max_health / 10.0) {
                Healths.setHealth(getPlayer(), health - max_health/10.0);
                burningSoul.addBurningSoul(true);

                List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate);
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
        if (damageCauseList.contains(e.getCause())) {
            e.setDamage(e.getDamage() * (1 + burningSoul.getTotalBurningSoul()/10.0));
        }

        if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && burningSoul.getTotalBurningSoul() > 0) {
            e.setDamage(0);
            getPlayer().setHealth(1);
            burningSoul.removeBurningSoul();
            List<Player> nearby = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate);
            for (Player player : nearby) {
                player.setFireTicks(player.getFireTicks() + 100);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        if (e.getDamager().equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            if (e.getEntity().getFireTicks() >= 100) {
                int firetick = e.getEntity().getFireTicks();
                e.getEntity().setFireTicks(0);
                double damageIncrement = ((firetick-80)/20.0) * 1.2;
                e.setDamage(e.getDamage() + damageIncrement);
            } else {
                e.getEntity().setFireTicks(e.getEntity().getFireTicks() + 20 + burningSoul.getTotalBurningSoul()*20);
            }
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        e.setAmount(e.getAmount() * 2.0);
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
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
            temporary_soul.removeIf(mill -> mill + 5000 >= System.currentTimeMillis());
            channel.update("§c버닝 소울: "+soul + "(+ "+temporary_soul.size()+")");
        }

        public void addBurningSoul(boolean temporary) {
            if(temporary) temporary_soul.add(System.currentTimeMillis());
            else if (soul < 5) soul += 1;
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
                            Damages.damageMagic(damageable, getPlayer(), false, 3);
                            setCount(30);
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
