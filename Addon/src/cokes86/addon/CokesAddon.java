package cokes86.addon;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.CokesSynergy;
import cokes86.addon.ability.remake.Remaking;
import cokes86.addon.configuration.addon.Config;
import cokes86.addon.configuration.gamemode.GameConfiguration;
import cokes86.addon.gamemodes.addon.debug.DebugWar;
import cokes86.addon.gamemodes.addon.standard.AddonAbilityWar;
import cokes86.addon.gamemodes.battleability.BattleAbility;
import cokes86.addon.gamemodes.battleability.BattleMixAbility;
import cokes86.addon.gamemodes.tailcatching.TailCatching;
import cokes86.addon.gamemodes.targethunting.TargetHunting;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.manager.GameFactory;
import daybreak.google.gson.JsonElement;
import daybreak.google.gson.JsonObject;
import daybreak.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CokesAddon extends Addon implements Listener {
	ConfigLoader loader = new ConfigLoader();

	@Override
	public void onEnable() {
		loader.run();

		// AddonAbilityWar
		GameFactory.registerMode(AddonAbilityWar.class);
		if (Settings.DeveloperSettings.isEnabled()) GameFactory.registerMode(DebugWar.class);

		//Battle Ability
		GameFactory.registerMode(BattleAbility.class);
		GameFactory.registerMode(BattleMixAbility.class);

		//TailCatching
		GameFactory.registerMode(TailCatching.class);

		//Target Hunting (BETA)
		if (Settings.DeveloperSettings.isEnabled()) GameFactory.registerMode(TargetHunting.class);

		// Load Complete
		Bukkit.getConsoleSender().sendMessage("[CokesAddon] 애드온이 활성화되었습니다.");

		//Bug Info
		try {
			if (!getBugLists().isEmpty()) {
				Bukkit.getConsoleSender().sendMessage("[CokesAddon] 해당 버전에는 아래와 같은 버그가 있습니다. 게임 플레이시 유의해주세요.");
				for (String str : getBugLists()) {
					Bukkit.getConsoleSender().sendMessage("[CokesAddon] " + str);
				}
			}
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		// Load Configuration
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), loader);

		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}

	@Override
	public void onDisable() {
		loader.run();
	}

	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a코크스에드온 §f적용중. 총 " + AddonAbilityFactory.getAddonAbilities().size() + "개의 능력이 추가되었습니다.");
		if (e.getGame() instanceof AbstractMix) {
			e.addCredit("§a믹스! §f새로운 시너지 " + AddonAbilityFactory.nameSynergyValues().size() + "개가 추가되었습니다!");
		}
		e.addCredit("§a코크스에드온 §f제작자 : Cokes_86  [§7디스코드 §f: Cokes_86#9329]");
	}

	public List<String> getBugLists() throws IllegalStateException, IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://pastebin.com/raw/GSnXXu64").openStream(), StandardCharsets.UTF_8));
		final StringBuilder result = new StringBuilder();
		{
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		}
		final JsonObject json = JsonParser.parseString(result.toString()).getAsJsonObject();
		final List<String> bugs = new ArrayList<>();
		if (json.has(this.getDescription().getVersion()) && json.get(this.getDescription().getVersion()).isJsonArray()) {
			for (JsonElement line : json.get(this.getDescription().getVersion()).getAsJsonArray()) {
				bugs.add(line.getAsString());
			}
		}
		return bugs;
	}

	static class ConfigLoader implements Runnable {

		@Override
		public void run() {
			try {
				AddonAbilityFactory.nameValues();
				AddonAbilityFactory.nameSynergyValues();
				AddonAbilityFactory.getAddonRemaking();
				GameConfiguration.load();
				Config.INSTANCE.update();

				CokesAbility.config.update();
				CokesSynergy.config.update();
				if (Settings.DeveloperSettings.isEnabled()) Remaking.config.update();
			} catch (IOException | InvalidConfigurationException e) {
				Bukkit.getConsoleSender().sendMessage("콘피그를 불러오는 도중 오류가 발생하였습니다.");
			}
		}

	}
}
