package cokes86.addon;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.ability.CokesAbility;
import cokes86.addon.synergy.AddonSynergyFactory;
import cokes86.addon.synergy.CokesSynergy;
import cokes86.addon.effect.AddonEffectFactory;
import cokes86.addon.gamemode.disguiseparty.DisguiseParty;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.manager.GameFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

public class CokesAddon extends Addon implements Listener {
	ConfigLoader loader = new ConfigLoader();

	@Override
	public void onEnable() {
		loader.run();

		// Load Complete
		Bukkit.getConsoleSender().sendMessage("[CokesAddon] 애드온이 활성화되었습니다.");

		// Load Configuration
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), loader);

		//effects
		AddonEffectFactory.load();

		//Game mode
		GameFactory.registerMode(DisguiseParty.class);

		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}

	@Override
	public void onDisable() {
		loader.run();
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
			AddonAbilityFactory.nameValues();
			AddonSynergyFactory.nameValues();

			CokesAbility.config.update();
			CokesSynergy.config.update();
		}
	}
}
