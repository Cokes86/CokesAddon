package cokes86.addon.effect.list;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@EffectManifest(displayName = "§8뒤틀림", name = "뒤틀림", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.SIGHT_RESTRICTION,
        EffectType.COMBAT_RESTRICTION
},  description = {
        "멀미 효과를 받으며, 30%의 확률로 자신의 공격에 빗나갑니다."
})
public class Warp extends AbstractGame.Effect implements Listener {
    public static final EffectRegistry.EffectRegistration<Warp> registration = EffectRegistry.registerEffect(Warp.class);
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        registration.apply(participant, timeunit, duration);
    }

    public Warp(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
        setPeriod(TimeUnit.TICKS, 1);

        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = participant.getPlayer().getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§8뒤틀림");
    }

    @Override
    protected void run(int arg0) {
        super.run(arg0);
        if (arg0 % 140 == 0) {
            PotionEffects.CONFUSION.addPotionEffect(participant.getPlayer(), 140, 0, true);
        }
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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (NMS.isArrow(damager)) {
            Projectile arrow = (Projectile) e.getDamager();
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(participant.getPlayer())) {
            final Random random = new Random();
            if (random.nextInt(10)<3) {
                e.setCancelled(true);
            }
        }
    }
}
