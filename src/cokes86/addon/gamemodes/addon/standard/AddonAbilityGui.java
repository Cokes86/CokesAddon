package cokes86.addon.gamemodes.addon.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.MatchResult;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.ability.Test;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.RegexReplacer;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import daybreak.abilitywar.utils.library.MaterialX;

public class AddonAbilityGui implements Listener {
	private final Player p;
	private final AbstractGame.Participant target;
	private int PlayerPage;
	private Inventory AbilityGUI;
	private final Map<String, AbilityRegistration> values;
	
	private static final RegexReplacer SQUARE_BRACKET = new RegexReplacer("\\$\\[([^\\[\\]]+)\\]");
	private static final RegexReplacer ROUND_BRACKET = new RegexReplacer("\\$\\(([^\\(\\)]+)\\)");

	public AddonAbilityGui(Player p, Plugin Plugin) {
		this.PlayerPage = 1;
		this.p = p;
		this.target = null;
		Bukkit.getPluginManager().registerEvents(this, Plugin);
		values = new TreeMap<>();
		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonAbilities()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}
	}

	public AddonAbilityGui(Player p, AbstractGame.Participant target, Plugin Plugin) {
		this.PlayerPage = 1;
		this.p = p;
		this.target = target;
		Bukkit.getPluginManager().registerEvents(this, Plugin);
		values = new TreeMap<>();
		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonAbilities()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}
	}

	public void openAbilityGUI(int page) {
		int MaxPage = (this.values.size() - 1) / 36 + 1;
		if (MaxPage < page)
			page = 1;
		if (page < 1)
			page = 1;
		this.AbilityGUI = Bukkit.createInventory(null, 54,
				ChatColor.translateAlternateColorCodes('&', "&cAbilityWar 코크스 애드온 &e능력 목록"));
		this.PlayerPage = page;
		int Count = 0;

		for (AbilityRegistration registration : values.values()) {
			AbilityManifest manifest = registration.getManifest();
			
			ItemStack is = MaterialX.GREEN_WOOL.parseItem();
			ItemMeta im = is.getItemMeta();
			im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b" + manifest.name()));
			
			StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", ");
			if (registration.hasFlag(Flag.ACTIVE_SKILL)) joiner.add(ChatColor.GREEN + "액티브");
			if (registration.hasFlag(Flag.TARGET_SKILL)) joiner.add(ChatColor.GOLD + "타겟팅");
			if (registration.getAbilityClass().isAnnotationPresent(Test.class)) joiner.add(ChatColor.RED + "테스트");
			
			List<String> lore = Messager.asList(
					ChatColor.translateAlternateColorCodes('&', "&f등급: " + manifest.rank().getRankName()),
					ChatColor.translateAlternateColorCodes('&', "&f종류: " + manifest.species().getSpeciesName()),
					joiner.toString(),
					"");
			
			Function<MatchResult, String> valueProvider = new Function<MatchResult, String>() {
				@Override
				public String apply(MatchResult matchResult) {
					Field field = registration.getFields().get(matchResult.group(1));
					if (field != null) {
						if (Modifier.isStatic(field.getModifiers())) {
							try {
								return String.valueOf(ReflectionUtil.setAccessible(field).get(null));
							} catch (IllegalAccessException ignored) {
							}
						}
					}
					return "?";
				}
			};
			
			for (String explain : manifest.explain()) {
				lore.add(ChatColor.WHITE.toString().concat(ROUND_BRACKET.replaceAll(SQUARE_BRACKET.replaceAll(explain, valueProvider), valueProvider)));
			}
			
			im.setLore(lore);
			is.setItemMeta(im);

			if (Count / 36 == page - 1) {
				this.AbilityGUI.setItem(Count % 36, is);
			}
			Count++;
		}

		if (page > 1) {
			ItemStack previousPage = MaterialX.ARROW.parseItem();
			ItemMeta previousMeta = previousPage.getItemMeta();
			previousMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b이전 페이지"));
			previousPage.setItemMeta(previousMeta);
			this.AbilityGUI.setItem(48, previousPage);
		}

		if (page != MaxPage) {
			ItemStack nextPage = MaterialX.ARROW.parseItem();
			ItemMeta nextMeta = nextPage.getItemMeta();
			nextMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b다음 페이지"));
			nextPage.setItemMeta(nextMeta);
			this.AbilityGUI.setItem(50, nextPage);
		}

		ItemStack Page = MaterialX.PAPER.parseItem();
		ItemMeta PageMeta = Page.getItemMeta();
		PageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6페이지 &e" + page + " &6/ &e" + MaxPage));
		Page.setItemMeta(PageMeta);
		this.AbilityGUI.setItem(49, Page);

		this.p.openInventory(this.AbilityGUI);
	}

	@EventHandler
	private void onInventoryClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(this.AbilityGUI)) {
			HandlerList.unregisterAll(this);
		}
	}

	@EventHandler
	private void onInventoryClick(InventoryClickEvent e) {
		if (e.getInventory().equals(this.AbilityGUI)) {
			Player p = (Player) e.getWhoClicked();
			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				if (e.getCurrentItem().getItemMeta().getDisplayName()
						.equals(ChatColor.translateAlternateColorCodes('&', "&b이전 페이지"))) {
					openAbilityGUI(this.PlayerPage - 1);
				} else if (e.getCurrentItem().getItemMeta().getDisplayName()
						.equals(ChatColor.translateAlternateColorCodes('&', "&b다음 페이지"))) {
					openAbilityGUI(this.PlayerPage + 1);
				}
			}
			if (e.getCurrentItem() != null && e.getCurrentItem().getType().equals(MaterialX.GREEN_WOOL.parseMaterial())
					&& e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName()) {
				String AbilityName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

				Class<? extends AbilityBase> abilityClass = AddonAbilityFactory.getByString(AbilityName);
				try {
					if (abilityClass != null && GameManager.isGameRunning()) {
						AbstractGame game = GameManager.getGame();
						if (this.target != null) {
							this.target.setAbility(abilityClass);
							Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e" + p.getName()
									+ "&a님이 &f" + this.target.getPlayer().getName() + "&a님에게 능력을 임의로 부여하였습니다."));
						} else {
							for (AbstractGame.Participant participant : game.getParticipants()) {
								participant.setAbility(abilityClass);
							}
							Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + p.getName() + "&a님이 &f전체 유저&a에게 능력을 임의로 부여하였습니다."));
						}

					}
				} catch (SecurityException | InstantiationException | IllegalAccessException
						| IllegalArgumentException | java.lang.reflect.InvocationTargetException ex) {

					if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
						Messager.sendErrorMessage(p, ex.getMessage());
					} else {
						Messager.sendErrorMessage(p, "설정 도중 오류가 발생하였습니다.");
					}
				}

				p.closeInventory();
			}
		}
	}
}
