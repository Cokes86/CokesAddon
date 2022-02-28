package cokes86.addon.effect.list;

import cokes86.addon.effect.AddonEffectRegistry;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

@EffectManifest(name = "파괴의 신", displayName = "§e파괴의 §c신", method = ApplicationMethod.UNIQUE_LONGEST,  description = {
        "자신이 상대방에게 주는 대미지가 3 증가합니다.",
        "해당 효과를 가진 상태에서 상대방을 살해할 시 대미지가 1 추가 증가합니다.",
        "해당 효과가 종료될 시 해당 효과로 증가한 대미지의 5배에 해당하는 대미지를 받습니다."
})
public class GodOfBreak extends AbstractGame.Effect implements Listener {
    private static final EffectRegistry.EffectRegistration<GodOfBreak> registration = AddonEffectRegistry.getRegistration(GodOfBreak.class);

    public static GodOfBreak apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        return registration.apply(participant, timeUnit, duration);
    }

    private final TimeUnit timeUnit;
    private final int duration;
    private final AbstractGame.Participant participant;
    private int addDamage = 3;

    public GodOfBreak(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));

        this.timeUnit = timeUnit;
        this.duration = duration;
        this.participant = participant;

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    @Override
    protected void onEnd() {
        participant.getPlayer().damage(addDamage * 5, participant.getPlayer());
        HandlerList.unregisterAll(this);
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        super.onSilentEnd();
    }

    @Override
    protected void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        super.onStart();
    }

    @Override
    protected void onCountSet() {
        super.onCountSet();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getEntity().getKiller() != null && e.getEntity().getKiller().equals(participant.getPlayer())) {
            this.setCount(timeUnit.toTicks(duration));
            addDamage++;
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity attacker = e.getDamager();
        if (attacker instanceof Arrow) {
            Arrow arrow = (Arrow) attacker;
            if (arrow.getShooter() instanceof Entity) {
                attacker = (Entity) arrow.getShooter();
            }
        }

        if (attacker.equals(participant.getPlayer()) && !e.getEntity().equals(participant.getPlayer())) {
            e.setDamage(e.getDamage()+addDamage);
        }
    }
}
