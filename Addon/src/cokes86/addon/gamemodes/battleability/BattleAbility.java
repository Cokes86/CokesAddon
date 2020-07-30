package cokes86.addon.gamemodes.battleability;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.manager.object.Invincibility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;

import cokes86.addon.configuration.gamemode.GameConfiguration.Config;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.manager.object.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "자기장 능력자 전쟁", description = {"§f맵의 크기가 서서히 줄어든다!", "§f줄어들기 전에 얼른 사냥해라!", "§f※게임 시작 전에 게임 스폰을 지정해주세요."})
@Category(value = Category.GameCategory.GAME)
@GameAliases(value = {"자기장", "자능전"})
public class BattleAbility extends Game implements DefaultKitHandler, Winnable, Observer{
	Location location;
	World world;
	double size;
	private final Border border = new Border(this);

	public BattleAbility() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		world = Bukkit.getWorld(Settings.getSpawnLocation().world);
		WorldBorder wb = world.getWorldBorder();
		
		location = wb.getCenter();
		size = wb.getSize();
	}

	@Override
	protected void progressGame(int arg0) {
		switch (arg0) {
		case 1:
			List<String> lines = Messager.asList(ChatColor.translateAlternateColorCodes('&', "&6==== &e게임 참여자 목록 &6===="));
			int count = 0;
			for (Participant p : getParticipants()) {
				count++;
				lines.add(ChatColor.translateAlternateColorCodes('&', "&a" + count + ". &f" + p.getPlayer().getName()));
			}
			lines.add(ChatColor.translateAlternateColorCodes('&', "&e총 인원수 : " + count + "명"));
			lines.add(ChatColor.translateAlternateColorCodes('&', "&6=========================="));

			for (String line : lines) {
				Bukkit.broadcastMessage(line);
			}

			if (getParticipants().size() < 1) {
				stop();
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. &8(&72명&8)"));
			}
			break;
		case 3:
			lines = Messager.asList(
					ChatColor.translateAlternateColorCodes('&', "&cAbilityWar &f- &6자기장 능력자 전쟁"),
					ChatColor.translateAlternateColorCodes('&', "&e버전 &7: &f" + AbilityWar.getPlugin().getDescription().getVersion()),
					ChatColor.translateAlternateColorCodes('&', "&b애드온 개발자 &7: &fCokes_86"),
					ChatColor.translateAlternateColorCodes('&', "&9디스코드 &7: &fCokes_86&7#9329")
			);

			GameCreditEvent event = new GameCreditEvent(this);
			Bukkit.getPluginManager().callEvent(event);
			lines.addAll(event.getCredits());

			for (String line : lines) {
				Bukkit.broadcastMessage(line);
			}
			break;
		case 5:
			if (Settings.getDrawAbility()) {
				for (String line : Messager.asList(
						ChatColor.translateAlternateColorCodes('&', "&f플러그인에 총 &b" + AbilityList.nameValues().size() + "개&f의 능력이 등록되어 있습니다."),
						ChatColor.translateAlternateColorCodes('&', "&7능력을 무작위로 할당합니다..."))) {
					Bukkit.broadcastMessage(line);
				}
				try {
					startAbilitySelect();
				} catch (OperationNotSupportedException ignored) {
				}
			}
			break;
		case 6:
			if (Settings.getDrawAbility()) {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f모든 참가자가 능력을 &b확정&f했습니다."));
			} else {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f능력자 게임 설정에 따라 &b능력&f을 추첨하지 않습니다."));
			}
			break;
		case 8:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e잠시 후 게임이 시작됩니다."));
			break;
		case 10:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c5&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 11:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c4&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 12:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c3&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 13:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c2&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 14:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c1&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 15:
			for (String line : Messager.asList(
					ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"),
					ChatColor.translateAlternateColorCodes('&', "&f             &cAbilityWar &f- &6능력자 전쟁  "),
					ChatColor.translateAlternateColorCodes('&', "&f                    게임 시작                "),
					ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"))) {
				Bukkit.broadcastMessage(line);
			}

			giveDefaultKit(getParticipants());

			Location spawn = Settings.getSpawnLocation().toBukkitLocation();
			for (Participant participant : getParticipants()) {
				participant.getPlayer().teleport(spawn);
			}

			if (Settings.getNoHunger()) {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&2배고픔 무제한&a이 적용됩니다."));
			} else {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4배고픔 무제한&c이 적용되지 않습니다."));
			}

			if (Settings.getInfiniteDurability()) {
				attachObserver(new InfiniteDurability());
			} else {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4내구도 무제한&c이 적용되지 않습니다."));
			}

			if (Settings.getClearWeather()) {
				for (World w : Bukkit.getWorlds()) {
					w.setStorm(false);
				}
			}
			WorldBorder wb = world.getWorldBorder();
			
			if (isRestricted()) {
				getInvincibility().start(false);
			} else {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4초반 무적&c이 적용되지 않습니다."));
				setRestricted(false);
				
				wb.setCenter(spawn);
				wb.setSize(Config.getInt(GameNodes.BattleAbility_startSize));
				wb.setSize(1, Config.getInt(GameNodes.BattleAbility_time));
			}

			ScriptManager.runAll(this);

			startGame();
			break;
	}
	}
	
	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		if (isParticipating(player)) {
			Participant quitParticipant = getParticipant(player);
			getDeathManager().Operation(quitParticipant);
			Player winner = null;
			for (Participant participant : getParticipants()) {
				if (!getDeathManager().isExcluded(participant.getPlayer())) {
					if (winner == null) {
						winner = player;
					} else {
						return;
					}
				}
			}
			if (winner != null) Win(getParticipant(winner));
		}
	}
	
	@Override
	public DeathManager newDeathManager() {
		return new DeathManager(this) {
			public void Operation(Participant victim) {
				switch (DeathSettings.getOperation()) {
					case 탈락:
						Eliminate(victim);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
					case 관전모드:
					case 없음:
						victim.getPlayer().setGameMode(GameMode.SPECTATOR);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
				}
				Player winner = null;
				for (Participant participant : getParticipants()) {
					Player player = participant.getPlayer();
					if (!isExcluded(player)) {
						if (winner == null) {
							winner = player;
						} else {
							return;
						}
					}
				}
				if (winner != null) Win(getParticipant(winner));
			}
		};
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
			WorldBorder wb = world.getWorldBorder();
			wb.setCenter(location);
			wb.setSize(size);
		}
	}

	@Override
	public Invincibility getInvincibility() {
		return border;
	}
}
