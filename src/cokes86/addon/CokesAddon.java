package cokes86.addon;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.configuration.ConfigFile;
import cokes86.addon.configuration.gamemode.GameConfiguration;
import cokes86.addon.gamemodes.addon.debug.DebugWar;
import cokes86.addon.gamemodes.addon.standard.AddonAbilityWar;
import cokes86.addon.gamemodes.battleability.*;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.GameFactory;

public class CokesAddon extends Addon implements Listener {
	public final static AbilitySettings config = new AbilitySettings(ConfigFile.createFile("AddonAbilities.yml"));
	public final static AbilitySettings sconfig = new AbilitySettings(ConfigFile.createFile("AddonSynergies.yml"));

	@Override
	public void onEnable() {
		try { // firstLoad
			AddonAbilityFactory.nameValues();
			AddonAbilityFactory.nameSynergyValues();
			GameConfiguration.load();
			
			config.update();
			sconfig.update();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		// AddonAbilityWar
		GameFactory.registerMode(AddonAbilityWar.class);
		if (Settings.DeveloperSettings.isEnabled()) GameFactory.registerMode(DebugWar.class);
		
		//Battle Ability
		GameFactory.registerMode(BattleAbility.class);
		GameFactory.registerMode(BattleMixAbility.class);

		// Load Complete
		Bukkit.getConsoleSender().sendMessage("[AbilityAddon] 애드온이 활성화되었습니다.");

		// Load Configuration
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new ConfigLoader());
		
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}

	@Override
	public void onDisable() {
		try {
			AddonAbilityFactory.nameValues();
			AddonAbilityFactory.nameSynergyValues();
			GameConfiguration.update();
			
			config.update();
			sconfig.update();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a코크스에드온 §f적용중. 총 "+AddonAbilityFactory.getAddonAbilities().size()+"개의 능력이 추가되었습니다.");
		e.addCredit("§a코크스에드온 §f제작자 : Cokes_86  [§7디스코드 §f: Cokes_86#9329]");
	}

	class ConfigLoader implements Runnable {

		@Override
		public void run() {
			try {
				AddonAbilityFactory.nameValues();
				AddonAbilityFactory.nameSynergyValues();
				GameConfiguration.load();
				
				config.update();
				sconfig.update();
			} catch (IOException | InvalidConfigurationException e) {
				Bukkit.getConsoleSender().sendMessage("콘피그를 불러오는 도중 오류가 발생하였습니다.");
			}
		}

	}
}
