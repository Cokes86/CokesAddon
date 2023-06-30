package com.cokes86.cokesaddon.effect.list;

import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

@EffectManifest(name = "어지럼증", displayName = "§3어지럼증", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.MOVEMENT_INTERRUPT,
}, description = {
        "지속시간동안 구속1이 부여됩니다.",
        "지속시간동안 받는 대미지가 10% 증가합니다."
})
public class Dizziness extends AbstractGame.Effect implements Listener {
    private static final EffectRegistry.EffectRegistration<Dizziness> registration = AddonEffectRegistry.getRegistration(Dizziness.class);
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;

    public static Dizziness apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        return registration.apply(participant, timeUnit, duration);
    }

    public Dizziness(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());

        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = participant.getPlayer().getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§3어지럼증");
        setPeriod(TimeUnit.TICKS, 1);
    }

    @Override
    protected void run(int count) {
        PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 30, 0, true);
        if (hologram.isValid()) {
            hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
        }
    }

    @Override
    protected void onEnd() {
        hologram.remove();
        HandlerList.unregisterAll(this);
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        hologram.remove();
        HandlerList.unregisterAll(this);
        super.onSilentEnd();
    }

    @EventHandler
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getEntity().equals(getParticipant().getPlayer())) {
            e.setDamage(e.getDamage() * 1.1);
        }
    }
}
