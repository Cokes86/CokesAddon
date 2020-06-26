package cokes86.addon.gamemodes.addon.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.MatchResult;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mixability.Mix;
import daybreak.abilitywar.game.list.mixability.synergy.Synergy;
import daybreak.abilitywar.game.list.mixability.synergy.SynergyFactory;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.RegexReplacer;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import daybreak.abilitywar.utils.library.item.ItemBuilder;
import daybreak.google.common.base.Function;

public class DebugAbilityGui implements Listener {
	private static final ItemStack PREVIOUS_PAGE = (new ItemBuilder()).type(Material.ARROW)
			.displayName(ChatColor.AQUA + "이전 페이지").build();

	private static final ItemStack NEXT_PAGE = (new ItemBuilder()).type(Material.ARROW)
			.displayName(ChatColor.AQUA + "다음 페이지").build();

	private static final ItemStack REMOVE_ABILITY = (new ItemBuilder()).type(Material.BARRIER)
			.displayName(ChatColor.RED + "능력 제거").build();

	private static final RegexReplacer SQUARE_BRACKET = new RegexReplacer("\\$\\[([^\\[\\]]+)\\]");
	private static final RegexReplacer ROUND_BRACKET = new RegexReplacer("\\$\\(([^\\(\\)]+)\\)");

	private final Player p;
	private final AbstractGame.Participant target;
	private final Map<String, AbilityFactory.AbilityRegistration> values;
	private int currentPage = 1;
	private Inventory abilityGUI;

	public DebugAbilityGui(Player p, Plugin Plugin) {
		this.p = p;
		this.target = null;
		Bukkit.getPluginManager().registerEvents(this, Plugin);
		values = new TreeMap<>();
		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonAbilities()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}

		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonSynergies()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}
	}

	public DebugAbilityGui(Player p, Participant target, Plugin Plugin) {
		this.p = p;
		this.target = target;
		Bukkit.getPluginManager().registerEvents(this, Plugin);
		values = new TreeMap<>();
		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonAbilities()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}
		for (Class<? extends AbilityBase> ab : AddonAbilityFactory.getAddonSynergies()) {
			values.put(ab.getAnnotation(AbilityManifest.class).name(), AbilityFactory.getRegistration(ab));
		}
	}

	public void openGUI(int page) {
		int maxPage = (this.values.size() - 1) / 36 + 1;
		if (maxPage < page)
			page = 1;
		if (page < 1)
			page = 1;
		this.abilityGUI = Bukkit.createInventory(null, 54,
				ChatColor.translateAlternateColorCodes('&', "&cAbilityWar 코크스 애드온 &e능력 목록"));
		this.currentPage = page;
		int count = 0;
		for (Map.Entry<String, AbilityFactory.AbilityRegistration> entry : this.values.entrySet()) {
			if (count / 36 == page - 1) {
				final AbilityFactory.AbilityRegistration registration = entry.getValue();
				AbilityManifest manifest = registration.getManifest();
				ItemStack itemStack = new ItemStack(Material.GOLD_BLOCK);
				StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", ");
				if (registration.hasFlag(Flag.ACTIVE_SKILL))
					joiner.add(ChatColor.GREEN + "액티브");
				if (registration.hasFlag(Flag.TARGET_SKILL))
					joiner.add(ChatColor.GOLD + "타겟팅");
				if (registration.getAbilityClass().isAnnotationPresent(Test.class))
					joiner.add(ChatColor.RED + "테스트");
				if (registration.getAbilityClass().getSuperclass().equals(Synergy.class)) {
					joiner.add(ChatColor.YELLOW + "시너지");
					itemStack.setType(Material.DIAMOND_BLOCK);
				}

				ItemMeta itemMeta = itemStack.getItemMeta();
				itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b" + manifest.name()));

				List<String> lore = Messager.asList(new String[] {
						ChatColor.translateAlternateColorCodes('&', "&f" + manifest.rank().getRankName()),
						ChatColor.translateAlternateColorCodes('&', "&f" + manifest.species().getSpeciesName()),
						joiner.toString(), "" });
				Function<MatchResult, String> valueProvider = new Function<MatchResult, String>() {
					public String apply(MatchResult matchResult) {
						Field field = (Field) registration.getFields().get(matchResult.group(1));
						if (field != null && Modifier.isStatic(field.getModifiers()))
							try {
								return String.valueOf(((Field) ReflectionUtil.setAccessible(field)).get(null));
							} catch (IllegalAccessException illegalAccessException) {
							}
						return "?";
					}
				};
				for (String explain : manifest.explain())
					lore.add(ChatColor.WHITE.toString().concat(ROUND_BRACKET
							.replaceAll(SQUARE_BRACKET.replaceAll(explain, valueProvider), valueProvider)));
				lore.add("");
				lore.add(ChatColor.translateAlternateColorCodes('&', "&2>> &f이 능력을 부여하려면 클릭하세요."));
				itemMeta.setLore(lore);
				itemStack.setItemMeta(itemMeta);
				this.abilityGUI.setItem(count % 36, itemStack);
			}
			count++;
		}
		this.abilityGUI.setItem(45, REMOVE_ABILITY);
		if (page > 1)
			this.abilityGUI.setItem(48, PREVIOUS_PAGE);
		if (page != maxPage)
			this.abilityGUI.setItem(50, NEXT_PAGE);
		ItemStack stack = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6페이지 &e" + page + " &6/ &e" + maxPage));
		stack.setItemMeta(meta);
		this.abilityGUI.setItem(49, stack);
		this.p.openInventory(this.abilityGUI);
	}

	@EventHandler
	private void onInventoryClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(this.abilityGUI))
			HandlerList.unregisterAll(this);
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent e) {
		if (e.getPlayer().getUniqueId().equals(this.p.getUniqueId()))
			HandlerList.unregisterAll(this);
	}

	@EventHandler
	private void onInventoryClick(InventoryClickEvent e) {
		if (e.getInventory().equals(this.abilityGUI)) {
			Player p = (Player) e.getWhoClicked();
			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				if (e.getCurrentItem().getType() == Material.GOLD_BLOCK) {
					String AbilityName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

					Class<? extends AbilityBase> abilityClass = AddonAbilityFactory.getByString(AbilityName);
					try {
						if (abilityClass != null && GameManager.isGameRunning()) {
							AbstractGame game = GameManager.getGame();
							if (this.target != null) {
								target.removeAbility();
								this.target.setAbility(abilityClass);
								Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e" + p.getName()
										+ "&a님이 &f" + this.target.getPlayer().getName() + "&a님에게 능력을 임의로 부여하였습니다."));
							} else {
								for (AbstractGame.Participant participant : game.getParticipants()) {
									participant.removeAbility();
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
				} else if (e.getCurrentItem().getType() == Material.DIAMOND_BLOCK) {
					String AbilityName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

					@SuppressWarnings("unchecked")
					Class<? extends Synergy> abilityClass = (Class<? extends Synergy>) AddonAbilityFactory
							.getByString(AbilityName);
					try {
						if (abilityClass != null && GameManager.isGameRunning()) {
							AbstractGame game = GameManager.getGame();
							AbilityRegistration ab = AbilityFactory.getRegistration(abilityClass);
							if (this.target != null) {
								target.removeAbility();
								Mix mix = (Mix) AbilityBase.create(Mix.class, target);
								mix.setAbility(SynergyFactory.getSynergyBase(ab).getLeft().getAbilityClass(),
										SynergyFactory.getSynergyBase(ab).getRight().getAbilityClass());
								target.setAbility(mix);
								Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e" + p.getName()
										+ "&a님이 &f" + this.target.getPlayer().getName() + "&a님에게 능력을 임의로 부여하였습니다."));
							} else {
								for (AbstractGame.Participant participant : game.getParticipants()) {
									participant.removeAbility();
									Mix mix = (Mix) AbilityBase.create(Mix.class, participant);
									mix.setAbility(SynergyFactory.getSynergyBase(ab).getLeft().getAbilityClass(),
											SynergyFactory.getSynergyBase(ab).getRight().getAbilityClass());
									participant.setAbility(mix);
								}
								Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
										"&e" + p.getName() + "&a님이 &f전체 유저&a에게 능력을 임의로 부여하였습니다."));
							}
						}
					} catch (SecurityException | InstantiationException | IllegalAccessException
							| IllegalArgumentException | java.lang.reflect.InvocationTargetException
							| NoSuchFieldException ex) {

						if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
							Messager.sendErrorMessage(p, ex.getMessage());
						} else {
							Messager.sendErrorMessage(p, "설정 도중 오류가 발생하였습니다.");
						}
					}
				}
				if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "다음 페이지")) {
					openGUI(this.currentPage - 1);
				} else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "이전 페이지")) {
					openGUI(this.currentPage + 1);
				} else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "능력 제거")) {
					if (GameManager.isGameRunning()) {
						AbstractGame game = GameManager.getGame();
						if (this.target != null) {
							this.target.removeAbility();
							Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e" + p.getName()
									+ "&a님이&f " + this.target.getPlayer().getName() + "&a님의 능력을 제거하였습니다."));
						} else {
							for (AbstractGame.Participant participant : game.getParticipants()) {
								participant.removeAbility();
							}
							Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + p.getName() + "&a님이 &f전체 유저&a의 능력을 제거하였습니다."));
						}
					}
				}
				p.closeInventory();
			}
		}
	}
}
