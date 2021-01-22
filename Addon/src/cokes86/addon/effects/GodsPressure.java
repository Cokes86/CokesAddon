package cokes86.addon.effects;

import cokes86.addon.ability.list.Seth;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.*;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
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
    private final Player player;
    private final AbilityBase owner;
    public static final EffectRegistry.EffectRegistration<GodsPressure> pressure = EffectRegistry.registerEffect(GodsPressure.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration, AbilityBase owner) {
        pressure.apply(participant, timeunit, duration, "set-owner", owner);
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
            decrease = 3.0;
        }
        this.damage = Seth.DEBUFF_MAX.getValue() - decrease;
        this.player = participant.getPlayer();

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    public void onStart() {
        player.teleport(owner.getPlayer().getLocation());
        SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(player);
        SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(owner.getPlayer());
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }

    @Override
    protected void run(int arg0) {
        super.run(arg0);
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        super.onSilentEnd();
    }

    @EventHandler
    private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        e.setDamage(Math.max(0, e.getDamage() - damage));
    }
}
