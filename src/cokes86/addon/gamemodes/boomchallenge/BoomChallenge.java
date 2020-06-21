package cokes86.addon.gamemodes.boomchallenge;

import static daybreak.abilitywar.game.GameManager.stopGame;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.object.CommandHandler;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name="폭발첼린지", description = {"§f모두가 폭탄을 지니고 있다! 철괴를 우클릭해서 폭탄을 남에게 터트려주자!", "§f잠깐, 손이 미끄러졌...!"})
public class BoomChallenge extends Game implements Winnable {
	
	public BoomChallenge() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(true);
	}
	
	@Override
	protected void progressGame(int second) {
		switch(second) {
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

				if (getParticipants().size() < 2) {
					stopGame();
					Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. &8(&72명&8)"));
				}
				break;
			case 3:
				Bukkit.broadcastMessage("§b코크스애드온 §f폭발첼린지 게임모드 제작자 : §aCokes_86");

				GameCreditEvent event = new GameCreditEvent();
				Bukkit.getPluginManager().callEvent(event);
			
				for (String line : event.getCreditList()) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				try {
					for(Participant p : getParticipants()) {
						p.setAbility(BoomAbility.class);
					}
				} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				break;
			case 6:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e폭발 능력이 배정되었습니다."));
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "상대방에게 철괴로 우클릭하면, 자신 또는 상대방이 50%확률로 사망합니다."));
				break;
			case 7:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e잠시 후 게임이 시작됩니다."));
				break;
			case 9:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c5&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 10:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c4&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c3&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c2&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c1&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				for (String line : Messager.asList(
						ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"),
						ChatColor.translateAlternateColorCodes('&', "&f                &c폭발첼린지                "),
						ChatColor.translateAlternateColorCodes('&', "&f                    게임 시작                "),
						ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"))) {
				Bukkit.broadcastMessage(line);
				}
				
				if (Configuration.Settings.getSpawnEnable()) {
					Location spawn = Configuration.Settings.getSpawnLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}
				
				for(Participant p : getParticipants()) {
					p.getPlayer().getInventory().clear();
					p.getPlayer().getInventory().addItem(new ItemStack(Material.IRON_INGOT,1));
				}
				
				if (Configuration.Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}
				setRestricted(false);
				startGame();
				break;
		}
	}
	
	private final ArrayList<Participant> death = new ArrayList<>();
	
	@Override
	public DeathManager newDeathManager() {
		return new DeathManager(this) {
			@Override
			public void Operation(Participant victim) {
				death.add(victim);
				super.Operation(victim);
				
				int c = 0;
				Participant winner = null;
				for (Participant p : getParticipants()) {
					if (!death.contains(p)) {
						c++;
						winner = p;
					}
				}
				
				if (c == 1) {
                    Win(winner);
                }
			}
		};
	}
	
	public void executeCommand(CommandHandler.CommandType commandType, Player player, String[] args, Plugin plugin) {
		switch (commandType) {
			case ABI:
				player.sendMessage("§4해당 명령어는 폭발첼린지 게임모드에서 사용할 수 없습니다.");
				break;
			case ABLIST:
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2===== &a능력자 목록 &2====="));
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2===== &e전원. &f 폭발 &2====="));
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2========================"));
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f" + player.getName() + "&a님이 플레이어들의 능력을 확인하였습니다."));
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a그런데, 굳이 확인 안해도 다 알텐데 왜...? (제작자 올림)"));
				break;
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (isParticipating(player)) {
				e.setDamage(0);
				e.setCancelled(true);
			}
		}
	}
}
