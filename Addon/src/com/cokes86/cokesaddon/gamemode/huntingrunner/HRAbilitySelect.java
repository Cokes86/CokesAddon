package com.cokes86.cokesaddon.gamemode.huntingrunner;

import com.cokes86.cokesaddon.gamemode.huntingrunner.effects.RunnerEffect;
import com.cokes86.cokesaddon.gamemode.huntingrunner.effects.RunnerEffectFactory;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.AbilitySelect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HRAbilitySelect extends AbilitySelect {
    private List<Class<? extends RunnerEffect>> abilities;

    public HRAbilitySelect(HuntingRunner game, Collection<? extends HRParticipant> selectors) {
        super(game, selectors, 1);
        abilities = new ArrayList<>(RunnerEffectFactory.getRunnerEffects());
    }

    @Override
    protected void drawAbility(Collection<? extends Participant> collection) {

    }

    @Override
    protected boolean changeAbility(Participant participant) {
        return false;
    }
}
