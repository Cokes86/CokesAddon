package cokes86.addon.effects;

import cokes86.addon.ability.list.Seth;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.*;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@EffectManifest(displayName = "§c신의 프레셔", name = "신의 프레셔", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.COMBAT_RESTRICTION
})
public class GodsPressure extends AbstractGame.Effect implements Listener {
    private final double damage;
    private final AbilityBase owner;
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private static final EffectRegistry.EffectRegistration<GodsPressure> pressure = EffectRegistry.registerEffect(GodsPressure.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration, AbilityBase owner) {
        pressure.apply(participant, timeunit, duration, "set-owner", owner);
    }

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        pressure.apply(participant, timeunit, duration);
    }

    @EffectConstructor(name = "set-owner")
    public GodsPressure(AbstractGame.Participant participant, TimeUnit timeunit, int duration, AbilityBase owner) {
        participant.getGame().super(pressure, participant, timeunit.toTicks(duration));
        this.owner = owner;
        double decrease;
        if (owner instanceof Seth) {
            Seth seth = (Seth) owner;
            decrease = seth.getKill() / (((double) seth.getParticipantSize()) * 2);
        } else {
            decrease = 3.5;
        }
        this.damage = Seth.DEBUFF_MAX.getValue() - decrease;
        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§c신의 프레셔");

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    public GodsPressure(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        participant.getGame().super(pressure, participant, timeunit.toTicks(duration));
        this.owner = null;
        this.damage = Seth.DEBUFF_MAX.getValue() - 3;
        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§c신의 프레셔");

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    public void onStart() {
        SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer());
        if (owner != null) {
            participant.getPlayer().teleport(owner.getPlayer().getLocation());
            SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(owner.getPlayer());
        }
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }

    @Override
    protected void run(int arg0) {
        super.run(arg0);
        if (hologram.isValid()) {
            hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
        }
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        hologram.remove();
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        hologram.remove();
        super.onSilentEnd();
    }

    @EventHandler
    private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        Entity attacker = e.getDamager();
        if (attacker instanceof Arrow) {
            Arrow arrow = (Arrow) attacker;
            if (arrow.getShooter() instanceof Entity) {
                attacker = (Entity) arrow.getShooter();
            }
        }

        if (attacker.getUniqueId().equals(participant.getPlayer().getUniqueId())) {
            e.setDamage(Math.max(0.5, e.getDamage() - damage));
        }
    }
}
