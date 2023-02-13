package com.cokes86.cokesaddon.game.module.roulette.wizard;

import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.cokes86.cokesaddon.game.module.roulette.Roulette;
import com.cokes86.cokesaddon.game.module.roulette.RouletteConfig.SettingObject;

public abstract class RouletteWizard {
    static final Logger logger = Logger.getLogger(RouletteWizard.class.getName());

	protected static final ItemStack DECO = new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE)
			.displayName(ChatColor.WHITE.toString())
			.build();

	protected Inventory gui = null;
	private final int inventorySize;
	private final String inventoryName;
	final Player player;

	RouletteWizard(final Player player, int inventorySize, String inventoryName, Plugin plugin) {
		this.inventorySize = inventorySize;
		this.inventoryName = inventoryName;
		this.player = player;
		new Listener() {
			{
				Bukkit.getPluginManager().registerEvents(this, plugin);
			}

			@EventHandler
			private void onInventoryClose(InventoryCloseEvent e) {
				if (e.getInventory().equals(gui)) {
					HandlerList.unregisterAll(this);
					onUnregister(gui);
				}
			}

			@EventHandler
			private void onQuit(PlayerQuitEvent e) {
				if (e.getPlayer().getUniqueId().equals(player.getUniqueId())) {
					HandlerList.unregisterAll(this);
					onUnregister(gui);
				}
			}

			@EventHandler
			private void onInventoryClick(InventoryClickEvent e) {
				if (e.getInventory().equals(gui)) {
					onClick(e, gui);
				}
			}

		};
	}

	public ItemStack getBooleanSetting(SettingObject<Boolean> settingObject) {
		ItemStack wool = settingObject.getValue() ? MaterialX.LIME_WOOL.createItem() : MaterialX.RED_WOOL.createItem();
		ItemMeta woolMeta = wool.getItemMeta();
		woolMeta.setDisplayName(ChatColor.WHITE + settingObject.getDisplayName());
		String[] comments = settingObject.getComments();
		List<String> lore = new ArrayList<>(comments.length + 1);
		for (String comment : comments) {
			lore.add(ChatColor.GRAY + comment);
		}
		lore.add("§9값§f: " + settingObject.getValue());
		woolMeta.setLore(lore);
		wool.setItemMeta(woolMeta);
		return wool;
	}

	public ItemStack getIntegerSetting(SettingObject<Integer> settingObject) {
		ItemStack wool = MaterialX.BLUE_WOOL.createItem();
		ItemMeta woolMeta = wool.getItemMeta();
		woolMeta.setDisplayName(ChatColor.WHITE + settingObject.getDisplayName());
		String[] comments = settingObject.getComments();
		List<String> lore = new ArrayList<>(comments.length + 6);
		for (String comment : comments) {
			lore.add(ChatColor.GRAY + comment);
		}
		lore.add("§9값§f: " + settingObject.getValue());
		lore.addAll(Arrays.asList(
				"",
				"§c우클릭         §6» §e+ 1",
				"§cSHIFT + 우클릭 §6» §e+ 20",
				"§c좌클릭         §6» §e- 1",
				"§cSHIFT + 좌클릭 §6» §e- 20"
		));
		woolMeta.setLore(lore);
		wool.setItemMeta(woolMeta);
		return wool;
	}

	public boolean onIntegerClick(SettingObject<Integer> settingObject, ClickType clickType) {
		switch (clickType) {
			case RIGHT:
				if (!settingObject.setValue(settingObject.getValue() + 1)) return false;
				break;
			case SHIFT_RIGHT:
				if (!settingObject.setValue(settingObject.getValue() + 20)) return false;
				break;
			case LEFT:
				if (!settingObject.setValue(settingObject.getValue() - 1)) return false;
				break;
			case SHIFT_LEFT:
				if (!settingObject.setValue(settingObject.getValue() - 20)) return false;
				break;
			default:
				return true;
		}
		return false;
	}

	public void show() {
		if (inventorySize == 5) {
			this.gui = Bukkit.createInventory(null, InventoryType.HOPPER, inventoryName);
		} else {
			this.gui = Bukkit.createInventory(null, inventorySize, inventoryName);
		}
		openGUI(gui);
	}

	abstract void openGUI(Inventory gui);
	abstract void onClick(InventoryClickEvent e, Inventory gui);

	void onUnregister(final Inventory gui) {
		Roulette.config.update();
	}

	public Player getPlayer() {
		return player;
	}
}
