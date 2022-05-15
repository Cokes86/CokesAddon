package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

@AbilityManifest(name = "코크스테스트",rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL)
@Beta
@Materials(materials = {Material.IRON_INGOT, Material.GOLD_INGOT})
public class Test extends CokesAbility implements ActiveHandler {
    public Test(AbstractGame.Participant participant) throws IllegalStateException {
        super(participant);
    }

    @Override
    protected void onUpdate(Update update) {
    }

    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            getPlayer().sendMessage("damage: "+e.getDamage());
            getPlayer().sendMessage("cause: "+e.getCause().name());
            getPlayer().sendMessage("finalDamage: "+e.getFinalDamage());
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            Damages.damageMagic(getPlayer(), getPlayer(), false, 10);
            Damages.damageFixed(getPlayer(), getPlayer(), 10);
        }
        return false;
    }
}
