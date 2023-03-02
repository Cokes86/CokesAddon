package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@AbilityManifest(name = "콬테",rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.SPECIAL)
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
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getCause() == DamageCause.VOID) return;
        getPlayer().sendMessage("damage: "+e.getDamage()+" / finaldamage: "+e.getFinalDamage());
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            Villager villager = getPlayer().getWorld().spawn(getPlayer().getLocation(), Villager.class);
            getPlayer().damage(15, villager);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));
            getPlayer().sendMessage("------");
            getPlayer().setNoDamageTicks(0);
            getPlayer().damage(5, villager);
            getPlayer().setNoDamageTicks(0);
            getPlayer().damage(5, villager);
            getPlayer().setNoDamageTicks(0);
            getPlayer().damage(5, villager);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));
            villager.remove();
        }
        return false;
    }
}
