package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import org.bukkit.Material;

@AbilityManifest(name = "코크스테스트",rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL)
@Beta
@Materials(materials = {Material.IRON_INGOT, Material.GOLD_INGOT})
public class Test extends CokesAbility{
    public Test(AbstractGame.Participant participant) throws IllegalStateException {
        super(participant);
    }

    @Override
    protected void onUpdate(Update update) {
    }
}
