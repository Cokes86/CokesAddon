package cokes86.addon.synergy.list;

import cokes86.addon.synergy.CokesSynergy;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@AbilityManifest(name = "코크스<군인>", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.OTHERS, explain = {
        "§7철괴 우클릭 §8- §cK2§f: 전방을 향해 점사 3발을 연속으로 발사합니다.",
        "  해당 총알을 맞은 플레이어는 각각 $[K2_DAMAGE]만큼의 대미지를 입습니다. $[K2_COOLDOWN]",
        "  발사 시, 반동에 의하여 잠시 움직일 수 없게 됩니다.",
        "§7금괴 우클릭 §8- §e§l사단장 방문§f: §e§l사단장님§f이 부대를 방문해 군기를 잡습니다.",
        "  장성에 대한 경례곡이 모두에게 울려퍼지며, 울려퍼지는동안 자신은",
        "  움직일 수 없는 대신 모든 대미지가 99% 감소합니다. $[SALUTE_COOLDOWN]",
        "§8[§7HIDDEN§8] §c즉각 처형§f: 무슨 일이 일어났길레 즉각 처형 당했을까요?"
})
@Materials(materials = {Material.GOLD_INGOT, Material.IRON_INGOT})
public class CokesSoldier extends CokesSynergy implements ActiveHandler {
    private static final Config<Integer> SALUTE_COOLDOWN = new Config<>(CokesSoldier.class, "사단장_쿨타임", 60, Config.Condition.COOLDOWN);

    private static final Config<Integer> K2_COOLDOWN = new Config<>(CokesSoldier.class, "K2_쿨타임", 10, Config.Condition.COOLDOWN);
    private static final Config<Double> K2_DAMAGE = new Config<>(CokesSoldier.class, "K2_대미지", 5.0, a -> a > 0);

    public CokesSoldier(AbstractGame.Participant participant) {
        super(participant);
    }

    private final Cooldown salute_cooldown = new Cooldown(SALUTE_COOLDOWN.getValue(), "사단장 방문");
    private final Cooldown k2_cooldown = new Cooldown(K2_COOLDOWN.getValue(), "K2");
    private final VisitGeneral duration = new VisitGeneral();

    private boolean isSalute() {
        return duration.isRunning();
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.GOLD_INGOT && clickType == ClickType.RIGHT_CLICK && !duration.isDuration() && !salute_cooldown.isCooldown()) {
            return duration.start();
        }

        else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !k2_cooldown.isCooldown()) {
            for (AbstractGame.Participant participant : getGame().getParticipants()) {
                if (participant.getAbility() instanceof Mix) {
                    Mix mix = (Mix) participant.getAbility();
                    if (mix.getSynergy() != null && mix.getSynergy() instanceof CokesSoldier) {
                        CokesSoldier soldier = (CokesSoldier) mix.getSynergy();
                        if (soldier.isSalute()) {
                            Bukkit.broadcastMessage("§8[§7HIDDEN§8] §f감히 §e§l사단장님 §f앞에 총기를 들이밀어???");
                            getPlayer().setHealth(0.0);
                            SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer());
                            return false;
                        }
                    }
                }
            }
            new AbilityTimer(3) {
                @Override
                protected void run(int count) {
                    new K2Bullet(getPlayer(), getPlayer().getEyeLocation().clone(), getPlayer().getLocation().clone().getDirection().multiply(1.25)).start();
                    Stun.apply(getParticipant(), TimeUnit.TICKS, 2);
                    SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer(), 0.8f, 1);
                }
            }.setPeriod(TimeUnit.TICKS, 2).start();
            return k2_cooldown.start();
        }

        return false;
    }

    private class K2Bullet extends AbilityTimer {
        private final Material GLASS_PANE = ServerVersion.getVersion() > 12 ? Material.valueOf("GLASS_PANE") : Material.valueOf("THIN_GLASS");
        private final LivingEntity shooter;
        private final AbstractGame.CustomEntity entity;
        private final Vector forward;
        private final Predicate<Entity> predicate;
        private Location lastLocation;

        private K2Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity) {
            super(160);
            setPeriod(TimeUnit.TICKS, 1);
            this.shooter = shooter;
            this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
            this.forward = arrowVelocity.multiply(10);
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
        }

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
                    assert location.getWorld() != null;
                    if (ItemLib.STAINED_GLASS.compareType(type) || Material.GLASS == type || ItemLib.STAINED_GLASS_PANE.compareType(type) || type == GLASS_PANE) {
                        block.breakNaturally();
                        SoundLib.BLOCK_GLASS_BREAK.playSound(block.getLocation(), 3, 1);
                    } else if (RayTrace.hitsBlock(location.getWorld(), lastLocation.getX(), lastLocation.getY(), lastLocation.getZ(), location.getX(), location.getY(), location.getZ())) {
                        stop(false);
                        return;
                    }
                }
                for (Player damageable : LocationUtil.getConflictingEntities(Player.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
                    if (!shooter.equals(damageable)) {
                        damageable.setNoDamageTicks(0);
                        double damage = K2_DAMAGE.getValue();
                        Damages.damageArrow(damageable, shooter, (float) damage);
                        stop(false);
                        return;
                    }
                }
                ParticleLib.REDSTONE.spawnParticle(location, RGB.GRAY);
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

        public class ArrowEntity extends AbstractGame.CustomEntity {

            public ArrowEntity(World world, double x, double y, double z) {
                getGame().super(world, x, y, z);
            }

            @Override
            protected void onRemove() {
                K2Bullet.this.stop(false);
            }

        }
    }

    private class VisitGeneral extends Duration implements Listener {
        private final SaluteToTheGeneral radio = new SaluteToTheGeneral();

        VisitGeneral() {
            super(387, salute_cooldown);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void onDurationStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
            radio.start();
        }

        @Override
        protected void onDurationEnd() {
            HandlerList.unregisterAll(this);
            radio.stop(false);
        }

        @Override
        protected void onDurationSilentEnd() {
            HandlerList.unregisterAll(this);
            radio.stop(true);
        }

        @Override
        protected void onDurationProcess(int i) {

        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent e) {
            if (e.getPlayer().equals(getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setDamage(e.getDamage() * 0.01);
            }
        }

        class SaluteToTheGeneral extends AbilityTimer {
            private final Note note16 = new Note(16),
                    note20 = new Note(20),
                    note11 = new Note(11),
                    note18 = new Note(18),
                    note21 = new Note(21),
                    note23 = new Note(23),
                    note15 = new Note(15),
                    note13 = new Note(13),
                    note6 = new Note(6);

            public SaluteToTheGeneral() {
                super(TaskType.NORMAL, 387);
                setPeriod(TimeUnit.TICKS, 1);
            }

            private void BigSnare() {
                SoundLib.SNARE_DRUM.broadcastInstrument(new Note(13));
                SoundLib.SNARE_DRUM.broadcastInstrument(new Note(4));
            }

            private void SmallSnare() {
                SoundLib.SNARE_DRUM.broadcastInstrument(new Note(8));
            }

            private void Bass() {
                SoundLib.BASS_DRUM.broadcastInstrument(note6);
            }

            @Override
            protected void run(int count) {
                //intro
                if (count < 114) {
                    switch (count - (count < 65 ? 1 : 65)) {
                        case 0: case 32:{
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            Bass();
                            break;
                        }
                        case 4: case 6: case 8: case 12:{
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            break;
                        }
                        case 16: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            Bass();
                            break;
                        }
                        case 24: case 30: {
                            SoundLib.FLUTE.broadcastInstrument(note11);
                            break;
                        }
                        case 40: {
                            SoundLib.BASS_DRUM.broadcastInstrument(note6);
                            break;
                        }
                        case 48: {
                            SoundLib.SNARE_DRUM.broadcastInstrument(note13);
                            Bass();
                            break;
                        }
                    }
                }
                //main
                else if ((count-65-57)%2 == 0){
                    switch((count-65-57)/2) {
                        case 0: case 16: case 32: case 123: {
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            break;
                        }
                        case 1: case 17: case 33: case 83: case 107: {
                            SoundLib.FLUTE.broadcastInstrument(note18);
                            break;
                        }
                        case 2: case 18: case 34: case 103: case 99: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            break;
                        }
                        case 3: case 19: case 35: case 79: {
                            SoundLib.FLUTE.broadcastInstrument(note21);
                            break;
                        }
                        case 4: case 20: case 36:{
                            SoundLib.FLUTE.broadcastInstrument(note23);
                            BigSnare();
                            Bass();
                            break;
                        }
                        case 8: case 24: case 64: {
                            SmallSnare();
                            Bass();
                            break;
                        }
                        case 12: case 28:{
                            BigSnare();
                            Bass();
                            break;
                        }
                        case 40: case 43: {
                            SoundLib.FLUTE.broadcastInstrument(note23);
                            Bass();
                            SmallSnare();
                            break;
                        }
                        case 44: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            Bass();
                            BigSnare();
                            break;
                        }
                        case 48: case 51: case 56: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            Bass();
                            SmallSnare();
                            break;
                        }
                        case 52: {
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            Bass();
                            BigSnare();
                            break;
                        }
                        case 59: {
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            Bass();
                            SmallSnare();
                            break;
                        }
                        case 60: {
                            SoundLib.FLUTE.broadcastInstrument(note11);
                            Bass();
                            BigSnare();
                            break;
                        }
                        case 68: case 72: case 76: case 100: {
                            SoundLib.FLUTE.broadcastInstrument(note18);
                            BigSnare();
                            Bass();
                            break;
                        }
                        case 80: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            SmallSnare();
                            Bass();
                            break;
                        }
                        case 84: case 88: case 92: {
                            SoundLib.FLUTE.broadcastInstrument(note20);
                            BigSnare();
                            Bass();
                            break;
                        }
                        case 95: {
                            SoundLib.FLUTE.broadcastInstrument(note23);
                            break;
                        }
                        case 96: {
                            SoundLib.FLUTE.broadcastInstrument(note21);
                            SmallSnare();
                            break;
                        }
                        case 104: {
                            SoundLib.FLUTE.broadcastInstrument(note21);
                            SmallSnare();
                            Bass();
                            break;
                        }
                        case 108: case 116: case 120: case 124: {
                            SoundLib.FLUTE.broadcastInstrument(note16);
                            BigSnare();
                            Bass();
                            break;
                        }
                        case 112: {
                            SoundLib.FLUTE.broadcastInstrument(note15);
                            SmallSnare();
                            Bass();
                            break;
                        }
                    }
                }
            }
        }
    }
}
