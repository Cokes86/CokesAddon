package com.cokes86.cokesaddon.effect.list;

import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

@EffectManifest(name = "디버깅", displayName = "§4디버깅", method = ApplicationMethod.UNIQUE_IGNORE, description = {
        "액티브 스킬 감지 시 해당 상태이상은 기절로 변합니다.",
})
public class Debuging extends AbstractGame.Effect implements Listener {
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;
    private static final EffectRegistry.EffectRegistration<Debuging> registration = AddonEffectRegistry.getRegistration(Debuging.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        registration.apply(participant, timeunit, duration);
    }

    public Debuging(AbstractGame.Participant participant, TimeUnit timeunit, int duration) {
        participant.getGame().super(registration, participant, timeunit.toTicks(duration));
        this.participant = participant;

        final Location location = participant.getPlayer().getLocation();
        this.hologram = participant.getPlayer().getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        NMS.removeBoundingBox(hologram);
        hologram.setCustomNameVisible(true);
        hologram.setCustomName("§4디버깅");

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    @EventHandler
    public void onAbilityPreActiveSkill(AbilityPreActiveSkillEvent e) {
        if (e.getParticipant().equals(getParticipant())) {
            Stun.apply(getParticipant(), TimeUnit.TICKS, getCount());
            this.stop(true);
        }
    }

    public void onStart() {
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
}
