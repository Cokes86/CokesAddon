package com.cokes86.cokesaddon.game.module.roulette.wizard;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import com.cokes86.cokesaddon.game.module.roulette.RouletteRegister;

import daybreak.abilitywar.AbilityWar;

import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;

public class RouletteMainWizard extends RouletteWizard {

    public RouletteMainWizard(Player player, Plugin plugin) {
        super(player, 5, "§2§l룰렛", plugin);
    }

    @Override
    void openGUI(Inventory gui) {
        gui.setItem(0, getBooleanSetting(RouletteRegister.enable));
        gui.setItem(1, DECO);
        gui.setItem(2, getIntegerSetting(RouletteRegister.period));
        gui.setItem(3, DECO);
        gui.setItem(4, new ItemBuilder(MaterialX.HOPPER).displayName("§b룰렛 설정")
        .lore("§f룰렛 별 등장 확률을 설정하는 공간입니다.", "§f숫자가 높을 수록 자주 등장하며, 0이면 등장하지 않습니다.").build());
        getPlayer().openInventory(gui);
    }

    @Override
    void onClick(InventoryClickEvent e, Inventory gui) {
        if (e.getInventory().equals(gui)) {
			e.setCancelled(true);
			if (e.getRawSlot()==0) {
                RouletteRegister.enable.setValue(!RouletteRegister.enable.getValue());
                show();
            } else if (e.getRawSlot()==2) {
                onIntegerClick(RouletteRegister.period, e.getClick());
                show();
            } else if(e.getRawSlot() == 4) {
                getPlayer().closeInventory();
                new RouletteListWizard(getPlayer(), AbilityWar.getPlugin()).show();
            }
        }
    }
    
}
