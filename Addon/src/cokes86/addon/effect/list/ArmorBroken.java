package cokes86.addon.effect.list;

import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.UUID;

@EffectManifest(name = "갑옷파괴", displayName = "§b갑옷 파괴", method = ApplicationMethod.MULTIPLE, type = {
        EffectType.COMBAT_RESTRICTION
}, description = {
        "방어와 방어 강도를 1씩 감소합니다.",
        "0 이하로는 감소하지 않으며, 최대 5회까지 적용됩니다.",
        "이상 적용될 경우, 먼저 적용된 §b갑옷 파괴 §f효과는 사라집니다."
})
public class ArmorBroken extends AbstractGame.Effect implements Listener {
    private static final EffectRegistry.EffectRegistration<ArmorBroken> broken = EffectRegistry.registerEffect(ArmorBroken.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        broken.apply(participant, timeUnit, duration);
    }

    private final AbstractGame.Participant participant;
    private AttributeModifier modifier;

    public ArmorBroken(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(broken, participant, timeUnit.toTicks(duration));
        this.participant = participant;

        this.setPeriod(TimeUnit.TICKS, 1);
    }

    @EventHandler
    private void onParticipantEffectApply(ParticipantEffectApplyEvent e) {
        if (e.getParticipant().equals(participant)) {
            if (e.getEffectType().equals(broken)) {
                if (e.getParticipant().getEffects(broken).size() >= 5) {
                    this.stop(true);
                }
            }
        }
    }

    public void onStart() {
        super.onStart();
        modifier = new AttributeModifier(UUID.randomUUID(), "brokenArmor", -1, AttributeModifier.Operation.ADD_NUMBER);
        AttributeUtil.getInstance(participant.getPlayer(), Attribute.GENERIC_ARMOR).addModifier(modifier);
        AttributeUtil.getInstance(participant.getPlayer(), Attribute.GENERIC_ARMOR_TOUGHNESS).addModifier(modifier);
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }

    public void onEnd() {
        onSilentEnd();
    }

    public void onSilentEnd() {
        super.onSilentEnd();

        AttributeUtil.getInstance(participant.getPlayer(), Attribute.GENERIC_ARMOR).removeModifier(modifier);
        AttributeUtil.getInstance(participant.getPlayer(), Attribute.GENERIC_ARMOR_TOUGHNESS).removeModifier(modifier);
        HandlerList.unregisterAll(this);
    }
}
