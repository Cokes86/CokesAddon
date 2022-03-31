package cokes86.addon.gamemode.huntingrunner;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantAbilitySetEvent;

public class HRParticipant extends Participant {

    private AbilityBase ability = null;
    private final Attributes attributes = new Attributes();

    protected HRParticipant(AbstractGame abstractGame, @NotNull Player arg0) {
        abstractGame.super(arg0);
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public @Nullable AbilityBase getAbility() {
        return ability;
    }

    @Override
    public boolean hasAbility() {
        return ability != null;
    }

    @Override
    public @Nullable AbilityBase removeAbility() {
        final AbilityBase ability = this.ability;
        if (ability != null) {
            ability.destroy();
            this.ability = null;
        }
        return ability;
    }

    @Override
    public void setAbility(AbilityRegistration registration) throws ReflectiveOperationException, UnsupportedOperationException {
        final AbilityBase oldAbility = removeAbility();
        final AbilityBase ability = AbilityBase.create(registration, this);
        ability.setRestricted(false);
        this.ability = ability;
        Bukkit.getPluginManager().callEvent(new ParticipantAbilitySetEvent(this, oldAbility, ability));
    }

}
