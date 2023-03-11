package com.cokes86.cokesaddon;

import com.cokes86.cokesaddon.ability.AddonAbilityFactory;
import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.command.CokesCommand;
import com.cokes86.cokesaddon.effect.AddonEffectRegistry;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.game.gamemode.disguiseparty.DisguiseParty;
import com.cokes86.cokesaddon.game.gamemode.tailcatch.TailCatch;
import com.cokes86.cokesaddon.game.module.roulette.Roulette;
import com.cokes86.cokesaddon.game.module.roulette.RouletteRegister;
import com.cokes86.cokesaddon.synergy.AddonSynergyFactory;
import com.cokes86.cokesaddon.synergy.CokesSynergy;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.addon.AddonLoader;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.Category.GameCategory;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.event.GameReadyEvent;
import daybreak.abilitywar.game.list.blind.BlindAbilityWar;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.blind.MixBlindGame;
import daybreak.abilitywar.game.manager.GameFactory;
import daybreak.abilitywar.utils.base.Messager;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CokesAddon extends Addon implements Listener {
	private static final Map<String, Addon> loaded = new HashMap<>();
	private static CokesAddon addon;
	private final ConfigLoader configLoader = new ConfigLoader();
	private final OtherAddonLoader otherAddonLoader = new OtherAddonLoader();

	public static CokesAddon getAddon() {
		return addon;
	}

	@Override
	public void onEnable() {
		//Load Effects
		AddonEffectRegistry.nameValues();

		//Load Addon Ability
		addon = this;
		AddonAbilityFactory.nameValues();
		AddonSynergyFactory.nameValues();
		RouletteRegister.getMapPairs();

		configLoader.run();

		//Check Other Addon
		CompletableFuture.runAsync(otherAddonLoader);

		//Load Game Mode
		GameFactory.registerMode(DisguiseParty.class);  // 변장파티
		GameFactory.registerMode(TailCatch.class);  // 꼬리잡기

		//Register Event
		Bukkit.getPluginManager().registerEvents(this, getPlugin());

		//Repeat Load Ability
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), configLoader);

		//command
		new CokesCommand();

		//Load Complete
		Messager.sendConsoleMessage("[§cCokesAddon§r] "+getDisplayName()+"이 활성화되었습니다.");
	}

	@Override
	public void onDisable() {
		configLoader.run();
	}

	@EventHandler
    public void onGameReady(GameReadyEvent e) {
		if (e.getGame() instanceof BlindAbilityWar) return;
		if (e.getGame() instanceof MixBlindGame) return;
		if (!RouletteRegister.isEnabled()) return;
		if (e.getGame().getClass().getAnnotation(Category.class) != null) {
			if (e.getGame().getClass().getAnnotation(Category.class).value() == GameCategory.GAME) {
				e.getGame().addModule(new Roulette(e.getGame()));
			}
		} else {
			e.getGame().addModule(new Roulette(e.getGame()));
		}
	}

	@EventHandler
	public void onGameEnd(GameEndEvent e) {
		if (e.getGame().hasModule(Roulette.class)) {
			e.getGame().getModule(Roulette.class).unregister();
		}
	}

	public static boolean isLoadAddon(String name) {
		return loaded.get(name) != null;
	}

	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		if (e.getGame().getRegistration().getCategory().equals(Category.GameCategory.GAME)) {
			e.addCredit("§c코크스 애드온 §f적용중. 총 " + AddonAbilityFactory.nameValues().size() + "개의 능력이 추가되었습니다.");
			if (e.getGame() instanceof AbstractMix) {
				e.addCredit("§c믹스! §c코크스 애드온§f에서 새로운 시너지 " + AddonSynergyFactory.nameValues().size() + "개가 추가되었습니다!");
			}
			e.addCredit("§c코크스 애드온 §f제작자 : Cokes_86  [§7디스코드 §f: Cokes_86#9329]");
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (GameManager.isGameRunning() && e.getCause() == DamageCause.VOID) return;
		CEntityDamageEvent event = new CEntityDamageEvent(e);
		if (!event.isCancelled() && !e.isCancelled()) Bukkit.getPluginManager().callEvent(new CEntityDamageEvent(e));
	}

	public static File getAddonFile(String name) {
		return new File("plugins/AbilityWar/Addon/CokesAddon/"+name);
	}

	private static class ConfigLoader implements Runnable {
		@Override
		public void run() {
			CokesAbility.config.update();
			CokesSynergy.config.update();
			Roulette.config.update();
		}
	}

	private class OtherAddonLoader implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(250L);
				loaded.clear();
				for (Addon addon : AddonLoader.getAddons()) {
					if (addon.equals(CokesAddon.this)) continue;
					loaded.put(addon.getName(), addon);
				}
				AddonSynergyFactory.loadAddonSynergies();
			} catch (InterruptedException e) {
				Messager.sendConsoleMessage("§c[!] 코크스 애드온이 다른 애드온을 확인 중 오류가 발생하였습니다.");
			}
		}
	}
}
