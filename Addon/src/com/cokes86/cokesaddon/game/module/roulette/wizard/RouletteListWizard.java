package com.cokes86.cokesaddon.game.module.roulette.wizard;

import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteEffect;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.cokes86.cokesaddon.game.module.roulette.RouletteRegister;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;

import daybreak.abilitywar.utils.base.collect.Pair;

public class RouletteListWizard extends RouletteWizard {
    private final Player player;

    RouletteListWizard(Player player, Plugin plugin) {
        super(player, 36, "§2§l룰렛 리스트", plugin);
        this.player = player;
    }
    int page = 1;
    int maxPage = (RouletteRegister.getMapPairs().size()-1) / 18 + 1;

    @Override
    void openGUI(Inventory gui) {
        openGUI(gui, 1);
    }

    void openGUI(Inventory gui, int page) {
        int count = 0;
        this.page = Math.min(Math.max(1, page), maxPage);
        for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer>> cell : RouletteRegister.getMapPairs()) {
            if (count / 18 == page - 1) {
                gui.setItem(count % 18, getIntegerSetting(cell.getRight()));
            }
            count++;
        }
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, DECO);
        }
        gui.setItem(27, new ItemBuilder(MaterialX.REDSTONE_BLOCK)
                .displayName("§c초기화")
                .lore("§f클릭 시 모든 룰렛 우선도가 초기화됩니다.")
                .build());
        if (page != 1) {
            gui.setItem(33, new ItemBuilder(MaterialX.ARROW)
                    .displayName(ChatColor.AQUA + "이전 페이지")
                    .build());
        }
        gui.setItem(34, new ItemBuilder(MaterialX.PAPER)
                .displayName("§6페이지 §e" + page + " §6/ §e" + maxPage)
                .build());
        if (page != maxPage) {
            gui.setItem(35, new ItemBuilder(MaterialX.ARROW)
                    .displayName(ChatColor.AQUA + "다음 페이지")
                    .build());
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

                if (e.getRawSlot() >= 0 && e.getRawSlot() < 18) {
                    for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer>> cell : RouletteRegister.getMapPairs()) {
                        if (cell.getRight().getDisplayName().equals(stripName)) {
                            onIntegerClick(cell.getRight(), e.getClick());
                            show(page);
                        }
                    }
                }
                else if (e.getRawSlot() == 27) {
                    for (Pair<Class<? extends RouletteEffect>, SettingObject<Integer>> cell : RouletteRegister.getMapPairs()) {
                        cell.getRight().setValue(cell.getRight().getDefaultValue());
                    }
                    show(page);
                } else if (clicked.getType().equals(Material.ARROW)) {
                    if (e.getRawSlot() == 33) {
                        show(page - 1);
                    }
                    else if (e.getRawSlot() == 35) {
                        show(page + 1);
                    }
                }
            }
        }
    }
    
    void show(int page) {
        this.gui = Bukkit.createInventory(null, 36, "§2§l룰렛 리스트");
		openGUI(gui, page);
    }
}
