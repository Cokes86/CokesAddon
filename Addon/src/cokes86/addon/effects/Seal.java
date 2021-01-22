package cokes86.addon.effects;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

@EffectManifest(displayName = "§6능력봉인", name = "능력봉인", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.ABILITY_RESTRICTION
})
public class Seal extends AbstractGame.Effect {
    public static final EffectRegistry.EffectRegistration<Seal> registry = EffectRegistry.registerEffect(Seal.class);
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private int stack = 0;
    private boolean direction = true;

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        registry.apply(participant, timeunit, duration);
    }

    public Seal(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        participant.getGame().super(registry, participant, timeunit.toTicks(duration)/4);
        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§6능력봉인됨");
        setPeriod(TimeUnit.TICKS, 4);
    }

    public void onStart() {
        if (participant.getAbility() != null) participant.getAbility().setRestricted(true);
        else {
            stop(true);
            return;
        }

        participant.getPlayer().sendMessage("당신의 능력이 봉인되었습니다.");
        SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(participant.getPlayer());
    }

    @Override
    protected void run(int arg0) {
        super.run(arg0);
        if (arg0 % 20 == 0) SoundLib.BLOCK_ANVIL_PLACE.playSound(participant.getPlayer());
        if (hologram.isValid()) {
            if (++stack > 30) {
                this.direction = !direction;
                this.stack = 0;
            }
            hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
        }
    }

    @Override
    protected void onEnd() {
        if (participant.getAbility() != null) participant.getAbility().setRestricted(false);
        hologram.remove();
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        hologram.remove();
        super.onSilentEnd();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getEntity().equals(participant.getPlayer())) {
            this.stop(true);
        }
    }
}
