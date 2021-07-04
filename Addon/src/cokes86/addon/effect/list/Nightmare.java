package cokes86.addon.effect.list;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@EffectManifest(name = "악몽", displayName = "§8악몽", method = ApplicationMethod.UNIQUE_LONGEST, type = {
        EffectType.COMBAT_RESTRICTION,
        EffectType.MOVEMENT_RESTRICTION,
        EffectType.SIGHT_RESTRICTION,
        EffectType.ABILITY_RESTRICTION
}, description = {
        "지속시간동안 움직일 수 없고 시야가 봉인됩니다.",
        "받는 대미지가 1.5배 증가하고 자신의 액티브, 타겟팅 능력을 사용할 수 없습니다."
})
public class Nightmare extends AbstractGame.Effect implements Listener {
    private static final EffectRegistry.EffectRegistration<Nightmare> registration = EffectRegistry.registerEffect(Nightmare.class);
    private final AbstractGame.Participant participant;
    private final ArmorStand hologram;

    public static Nightmare apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        return registration.apply(participant, timeUnit, duration);
    }

    public Nightmare(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
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
        hologram.setCustomName("§8악몽");
        setPeriod(TimeUnit.TICKS, 1);
    }

    @Override
    protected void run(int count) {
        PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), 30, 0, true);
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
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer().equals(participant.getPlayer())) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler
    public void onAbilityUse(AbilityPreActiveSkillEvent e) {
        if (e.getParticipant().equals(participant)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAbilityUse(AbilityPreTargetEvent e) {
        if (e.getParticipant().equals(participant)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity().equals(participant.getPlayer())) {
            e.setDamage(e.getDamage() * 1.5);
        }
    }
}
