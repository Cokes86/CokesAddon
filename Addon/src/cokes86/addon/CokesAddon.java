package cokes86.addon;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.ability.CokesAbility;
import cokes86.addon.synergy.AddonSynergyFactory;
import cokes86.addon.synergy.CokesSynergy;
import cokes86.addon.effect.AddonEffectFactory;
import cokes86.addon.gamemode.disguiseparty.DisguiseParty;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.addon.AddonLoader;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.manager.GameFactory;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
		//Load Addon Ability
		addon = this;
		AddonAbilityFactory.nameValues();
		AddonSynergyFactory.nameValues();

		configLoader.run();

		//Load Effects
		AddonEffectFactory.load();

		//Check Other Addon
		CompletableFuture.runAsync(otherAddonLoader);

		//Load Mode
		GameFactory.registerMode(DisguiseParty.class);

		//Register Event
		Bukkit.getPluginManager().registerEvents(this, getPlugin());

		//Repeat Load Ability
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), configLoader);

		//Load Complete
		Messager.sendConsoleMessage("[§cCokesAddon§r] "+getDisplayName()+"이 활성화되었습니다.");
	}

	@Override
	public void onDisable() {
		configLoader.run();
	}

	public static boolean isLoadAddon(String name) {
		return loaded.get(name) != null;
	}

	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		if (e.getGame().getRegistration().getCategory().equals(Category.GameCategory.GAME)) {
			e.addCredit("§c코크스 애드온 §f적용중. 총 " + AddonAbilityFactory.nameValues().size() + "개의 능력이 추가되었습니다.");
			if (e.getGame() instanceof AbstractMix) {
				e.addCredit("§c믹스! §f새로운 시너지 " + AddonSynergyFactory.nameValues().size() + "개가 추가되었습니다!");
			}
			e.addCredit("§c코크스 애드온 §f제작자 : Cokes_86  [§7디스코드 §f: Cokes_86#9329]");
		}
	}

	public static File getAddonFile(String name) {
		return new File("plugins/AbilityWar/Addon/CokesAddon/"+name);
	}

	static class ConfigLoader implements Runnable {
		@Override
		public void run() {
			CokesAbility.config.update();
			CokesSynergy.config.update();
		}
	}

	class OtherAddonLoader implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(250L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			loaded.clear();
			for (Addon addon : AddonLoader.getAddons()) {
				if (addon.equals(CokesAddon.this)) continue;
				loaded.put(addon.getName(), addon);
				Messager.sendConsoleMessage("[§cCokesAddon§r] "+addon.getDisplayName()+"§r"+ KoreanUtil.getJosa(addon.getDisplayName(), KoreanUtil.Josa.이가) +" 감지되었습니다.");
			}
			AddonSynergyFactory.loadAddonSynergies();
		}
	}
}
