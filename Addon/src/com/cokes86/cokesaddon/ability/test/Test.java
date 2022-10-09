package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.nms.NMSUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getCause() == DamageCause.VOID) return;
        Player target = (Player) e.getEntity();
        if (!e.isCancelled()) {
            new AbilityTimer(1) {
                public void run(int arg0) {
                    if (!target.isDead() && Damages.canDamage(target, getPlayer(), DamageCause.VOID, 1f)) {
                        target.setNoDamageTicks(0);
                        NMSUtil.damageVoid(target, 1f);
                    }
                }
            }.setPeriod(TimeUnit.TICKS, 1).start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            NMSUtil.damageMagicFixed(getPlayer(), getPlayer(), 10);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));

            Damages.damageFixed(getPlayer(), getPlayer(), 10);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));

            NMSUtil.damageMagicFixed(getPlayer(), getPlayer(), 10);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));

            NMSUtil.damageVoid(getPlayer(), 10);
            Healths.setHealth(getPlayer(), AttributeUtil.getMaxHealth(getPlayer()));
        }
        return false;
    }
}
