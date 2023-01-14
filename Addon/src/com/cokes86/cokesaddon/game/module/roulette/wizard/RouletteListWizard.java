package com.cokes86.cokesaddon.game.module.roulette.wizard;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.cokes86.cokesaddon.game.module.roulette.Roulette;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;

import daybreak.abilitywar.config.wizard.setter.Setter;

public class RouletteListWizard extends RouletteWizard {

    RouletteListWizard(Player player, Plugin plugin) {
        super(player, 27, "§2§l룰렛 리스트", plugin);
    }

    @Override
    void openGUI(Inventory gui) {
        int count = 0;
        int page = 1;
        for (SettingObject<?> settingObject : Roulette.list) {
            if (count / 18 == page - 1) {
                gui.setItem(count % 18, Setter.getInstance(settingObject).getItem(settingObject));
            }
            count++;
        }
    }

    @Override
    void onClick(InventoryClickEvent e, Inventory gui) {
        if (e.getInventory().equals(gui)) {
			e.setCancelled(true);
            final ItemStack clicked = e.getCurrentItem();
			if (clicked != null && !clicked.getType().equals(Material.AIR) && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
				final String stripName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

                final SettingObject<?> settingObject = Roulette.config.getSetting(stripName.split(".")[0], stripName);
				if (settingObject != null) {
					if (Setter.getInstance(settingObject).onClick(settingObject, e.getClick()))
						openGUI(gui);
                }
            }
        }
    }
    
}
