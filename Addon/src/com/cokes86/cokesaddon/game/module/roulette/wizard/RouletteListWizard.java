package com.cokes86.cokesaddon.game.module.roulette.wizard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.cokes86.cokesaddon.game.module.roulette.RouletteEffect;
import com.cokes86.cokesaddon.game.module.roulette.RouletteRegister;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;

import daybreak.abilitywar.utils.base.collect.Pair;

public class RouletteListWizard extends RouletteWizard {
    private final Player player;

    RouletteListWizard(Player player, Plugin plugin) {
        super(player, 27, "§2§l룰렛 리스트", plugin);
        this.player = player;
    }
    int page = 0;

    @Override
    void openGUI(Inventory gui) {
        openGUI(gui, 1);
    }

    void openGUI(Inventory gui, int page) {
        int count = 0;
        this.page = page;
        for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer>> cell : RouletteRegister.getMapPairs()) {
            if (count / 18 == page - 1) {
                gui.setItem(count % 18, getIntegerSetting(cell.getRight()));
            }
            count++;
        }
        player.openInventory(gui);
    }

    @Override
    void onClick(InventoryClickEvent e, Inventory gui) {
        if (e.getInventory().equals(gui)) {
			e.setCancelled(true);
            final ItemStack clicked = e.getCurrentItem();
			if (clicked != null && !clicked.getType().equals(Material.AIR) && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
				final String stripName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

                if (e.getRawSlot()>=0 && e.getRawSlot() < 18) {
                    for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer>> cell : RouletteRegister.getMapPairs()) {
                        if (cell.getRight().getDisplayName().equals(stripName)) {
                            onIntegerClick(cell.getRight(), e.getClick());
                            show(page);
                        }
                    }
                }
            }
        }
    }
    
    void show(int page) {
        this.gui = Bukkit.createInventory(null, 27, "§2§l룰렛 리스트");
		openGUI(gui, page);
    }
}
