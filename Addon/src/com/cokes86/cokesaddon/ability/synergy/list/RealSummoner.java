package com.cokes86.cokesaddon.ability.synergy.list;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.cokes86.cokesaddon.ability.Config;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
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

import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.*;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "진 소환사", rank = Rank.A, species = Species.HUMAN, explain = {
    "철괴를 우클릭하여 자신을 제외한 참가자 한 명을 선택하여",
    "$[DURATION] 뒤에 자신의 위치로 소환합니다. $[COOLDOWN]",
    "소환되는 위치는 능력을 발동한 시점의 위치입니다.",
    "플레이어를 선택하지 않은 경우 3초의 쿨타임이 적용됩니다."
})
public class RealSummoner extends CokesSynergy implements ActiveHandler {
    public static Config<Integer> DURATION = Config.of(RealSummoner.class, "대기시간", 3, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
	public static Config<Integer> COOLDOWN = Config.of(RealSummoner.class, "쿨타임", 90, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private final Predicate<Entity> predicate = entity -> {
		if (entity != null && entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
			}
			if (getGame() instanceof Teamable) {
				Teamable game = (Teamable) getGame();
				return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};
    private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
    private final SummonSelectTimer active = new SummonSelectTimer();
	private final SummonTimer summon = new SummonTimer();

    public RealSummoner(Participant participant) {
        super(participant);
    }

    @Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && !active.isRunning() && !cool.isCooldown() && !summon.isRunning()) {
			active.start();
		}
		return false;
	}
    
    private class SummonSelectTimer extends AbilityTimer implements Listener {
		private final ItemStack PREVIOUS_PAGE = (new ItemBuilder(MaterialX.ARROW))
				.displayName(ChatColor.AQUA + "이전 페이지").build();

		private final ItemStack NEXT_PAGE = (new ItemBuilder(MaterialX.ARROW))
				.displayName(ChatColor.AQUA + "다음 페이지").build();

		private final Map<String, Participant> values;
		private int currentPage = 1;
		private final Inventory gui;
		private int maxPage;

		public SummonSelectTimer() {
			super();
			this.setPeriod(TimeUnit.TICKS, 1);
			values = new TreeMap<>();
			gui = Bukkit.createInventory(null, 54, "§r소환사");
		}

		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
		}

		@Override
		protected void run(int arg0) {
			values.clear();
			for (Participant p : getGame().getParticipants()) {
				if (p.equals(getParticipant())) continue;
				if (predicate.test(p.getPlayer())) values.put(p.getPlayer().getName(), p);
			}
			if (values.size() == 0) {
				getPlayer().sendMessage("소환활 플레이어가 존재하지 않습니다.");
				stop(true);
			}
			maxPage = (this.values.size() - 1) / 36 + 1;
			placeItem(currentPage);
		}

		private void placeItem(int page) {
			gui.clear();
			if (maxPage < page)
				page = 1;
			if (page < 1)
				page = 1;

			this.currentPage = page;
			int count = 0;

			for (Map.Entry<String, Participant> entry : this.values.entrySet()) {
				if (count / 36 == page - 1) {
					ItemStack stack = Skulls.createSkull(entry.getKey());
					final ItemMeta meta = stack.getItemMeta();
					if (meta != null) {
						meta.setDisplayName("§f" + entry.getKey());
						meta.setLore(Messager.asList("§f>> 소환할려면 좌클릭하세요."));
						stack.setItemMeta(meta);
					}
					gui.setItem(count % 36, stack);
					count++;
				}
			}

			if (page > 1)
				gui.setItem(48, PREVIOUS_PAGE);
			if (page != maxPage)
				gui.setItem(50, NEXT_PAGE);

			ItemStack stack = new ItemStack(Material.PAPER, 1);
			ItemMeta meta = stack.getItemMeta();
			assert meta != null;
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6페이지 &e" + page + " &6/ &e" + maxPage));
			stack.setItemMeta(meta);
			gui.setItem(49, stack);
		}

		protected void onEnd() {
			onSilentEnd();
		}

		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		private void onInventoryClose(InventoryCloseEvent e) {
			if (e.getInventory().equals(gui)) {
				stop(false);
				if (!summon.isRunning()) {
					cool.start();
					cool.setCooldown(3);
				}
			}
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			if (e.getPlayer().getUniqueId().equals(getPlayer().getUniqueId())) stop(false);
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				e.setCancelled(true);
				if (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().hasDisplayName()) {
					if (values.containsKey(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()))) {
						Participant target = values.get(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
						summon.setTarget(target).start();
						getPlayer().closeInventory();
					} else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "다음 페이지")) {
						placeItem(currentPage + 1);
					} else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "이전 페이지")) {
						placeItem(currentPage - 1);
					}
				}
			}
		}
	}

	private class SummonTimer extends AbilityTimer {
		private Participant target;
		private Location location;
		private double y = 0;
		private boolean up = true;

		public SummonTimer() {
			super(DURATION.getValue() * 20);
			this.setPeriod(TimeUnit.TICKS, 1);
		}

		protected void onStart() {
			NMS.sendTitle(target.getPlayer(), "경   고", "소환사가 당신을 소환하고 있습니다", 5, DURATION.getValue() * 20 - 10, 5);
			location = getPlayer().getLocation();
		}

		@Override
		protected void run(int arg0) {
			if (up && y >= 2) {
				up = false;
			} else if (!up && y <= 0) {
				up = true;
			}

			if (up) {
				y += 0.1;
			} else {
				y -= 0.1;
			}

			for (Location particle : Circle.iteratorOf(location, 1, 16).iterable()) {
				particle.setY(LocationUtil.getFloorYAt(Objects.requireNonNull(particle.getWorld()), particle.getY(), particle.getBlockX(), particle.getBlockZ()) + y);
				ParticleLib.REDSTONE.spawnParticle(particle, RGB.OLIVE);
			}
		}

		protected void onEnd() {
			target.getPlayer().teleport(location);
			onSilentEnd();
			cool.start();
		}

		protected void onSilentEnd() {
			NMS.clearTitle(target.getPlayer());
		}

		protected SummonTimer setTarget(Participant participant) {
			this.target = participant;
			return this;
		}
	}
}
