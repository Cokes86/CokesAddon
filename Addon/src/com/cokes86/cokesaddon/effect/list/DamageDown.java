package com.cokes86.cokesaddon.effect.list;

import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@EffectManifest(displayName = "§e대미지 감소", name = "대미지 감소", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.COMBAT_RESTRICTION
}, description = {
        "자신이 상대방을 때릴 때 주는 대미지가 감소합니다.",
        "따로 표기되지 않는 것은 0.5의 대미지 감소지만,",
        "능력에 따라 대미지 감소량은 달라집니다."
})
public class DamageDown extends AbstractGame.Effect implements Listener {
    private final double damage;
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private static final EffectRegistration<DamageDown> registration = AddonEffectRegistry.getRegistration(DamageDown.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        registration.apply(participant, timeunit, duration);
    }

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration, double damage) {
        registration.apply(participant, timeunit, duration, "set-damage", damage);
    }

    @EffectConstructor(name = "set-damage")
    public DamageDown(AbstractGame.Participant participant, TimeUnit timeunit, int duration, double damage) {
        participant.getGame().super(registration, participant, timeunit.toTicks(duration));
        this.damage = damage;
        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = participant.getPlayer().getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§e대미지 감소");

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    public DamageDown(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        this(participant, timeunit, duration, 0.5);
    }

    public void onStart() {
        SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer());
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
