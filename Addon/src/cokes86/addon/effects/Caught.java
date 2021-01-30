package cokes86.addon.effects;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@EffectManifest(displayName = "§c붙잡힘", name = "붙잡힘", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.MOVEMENT_RESTRICTION
})
public class Caught extends AbstractGame.Effect implements Listener {
    private static final EffectRegistry.EffectRegistration<Caught> caught = EffectRegistry.registerEffect(Caught.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        caught.apply(participant, timeUnit, duration);
    }

    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private int stack = 0;
    private boolean direction = true;

    protected Caught(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(caught, participant, timeUnit.toTicks(duration));
        this.participant = participant;
        final Location location = participant.getPlayer().getLocation();
        this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§c붙잡힘!");
        setPeriod(TimeUnit.TICKS, 1);
    }

    @Override
    protected void run(int count) {
        super.run(count);
        if (hologram.isValid()) {
            if (++stack > 30) {
                this.direction = !direction;
                this.stack = 0;
            }
            hologram.teleport(hologram.getLocation().clone().add(0, direction ? .008 : -.008, 0));
        }
        if (count % 40 == 0 && participant.getPlayer().getHealth() > 1) {
            Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() - 1);
        }
    }

    @Override
    protected void onEnd() {
        super.onEnd();
        hologram.remove();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void onSilentEnd() {
        super.onSilentEnd();
        hologram.remove();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }

    @EventHandler
    private void onPlayerMove(final PlayerMoveEvent e) {
        if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onParticipantActive(AbilityPreActiveSkillEvent e) {
        if (e.getParticipant().equals(participant)) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        onEntityDamage(e);

        Entity attacker = e.getDamager();
        if (attacker instanceof Projectile) {
            Projectile projectile = (Projectile) attacker;
            if (projectile.getShooter() instanceof Player) {
                attacker = (Entity) projectile.getShooter();
            }
        }

        if (attacker.getUniqueId().equals(participant.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
        onEntityDamage(e);
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }
}
