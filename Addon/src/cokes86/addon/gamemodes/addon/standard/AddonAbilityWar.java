package cokes86.addon.gamemodes.addon.standard;

import static daybreak.abilitywar.game.GameManager.getGame;
import static daybreak.abilitywar.game.GameManager.stopGame;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.naming.OperationNotSupportedException;

import daybreak.abilitywar.game.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import cokes86.addon.ability.AddonAbilityFactory;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings.DeveloperSettings;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "애드온 능력자 전쟁", description = { "§f기존의 능력자 전쟁과 같은데 능력이 코크스애드온의 능력들 뿐이다?!" })
@Category(value = Category.GameCategory.GAME)
@GameAliases(value = {"애드온", "애능전"})
public class AddonAbilityWar extends Game implements DefaultKitHandler {
	private final boolean invincible;

	public AddonAbilityWar() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		this.invincible = Configuration.Settings.InvincibilitySettings.isEnabled();
		setRestricted(this.invincible);
	}

	@Override
	protected void progressGame(int arg0) {
		switch (arg0) {
		case 1:
			List<String> lines = Messager
					.asList(ChatColor.translateAlternateColorCodes('&', "&6==== &e게임 참여자 목록 &6===="));
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
				stopGame();
				Bukkit.broadcastMessage(
						ChatColor.translateAlternateColorCodes('&', "&c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. &8(&71명&8)"));
			}
			if (getParticipants().size() > AddonAbilityFactory.getAddonAbilities().size()) {
				stopGame();
				Bukkit.broadcastMessage(
						ChatColor.translateAlternateColorCodes('&', "&c최대 참가자 수를 충족하지 못하여 게임을 중지합니다. &8(&7"
								+ AddonAbilityFactory.getAddonAbilities().size() + "명&8)"));
			}
			break;
		case 3:
			Bukkit.broadcastMessage("§b코크스애드온 §f애드온 능력자 전쟁 게임모드 제작자 : §aCokes_86");

			GameCreditEvent event = new GameCreditEvent(this);
			Bukkit.getPluginManager().callEvent(event);

			for (String line : event.getCredits()) {
				Bukkit.broadcastMessage(line);
			}
			break;
		case 5:
			if (Configuration.Settings.getDrawAbility()) {
				for (String line : Messager.asList(ChatColor.translateAlternateColorCodes('&',
						"&f애드온에는 총 &b" + AddonAbilityFactory.getAddonAbilities().size()
								+ "개&f의 능력이 등록되어 있습니다."),
						ChatColor.translateAlternateColorCodes('&', "&7능력이 무작위로 배정됩니다..."))) {
					Bukkit.broadcastMessage(line);
				}
			}
			break;
		case 8:
			if (Configuration.Settings.getDrawAbility()) {
				try {
					startAbilitySelect();
				} catch (OperationNotSupportedException e) {
					e.printStackTrace();
				}
			}
			break;
		case 10:
			if (Configuration.Settings.getDrawAbility()) {
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f모든 참가자가 능력을 &b확정&f했습니다."));
				break;
			}
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f능력자 게임 설정에 따라 &b능력&f을 추첨하지 않습니다."));
			break;
		case 12:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e잠시 후 게임이 시작됩니다."));
			break;
		case 13:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c5&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 14:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c4&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 15:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c3&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 16:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c2&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 17:
			Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c1&e초 후에 시작됩니다."));
			SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			break;
		case 18:
			for (String line : Messager.asList(
					ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"),
					ChatColor.translateAlternateColorCodes('&', "&f                &c애드온 능력 전쟁                "),
					ChatColor.translateAlternateColorCodes('&', "&f                    게임 시작                "),
					ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"))) {
				Bukkit.broadcastMessage(line);

				if (Configuration.Settings.getSpawnEnable()) {
					Location spawn = Configuration.Settings.getSpawnLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}

				giveDefaultKit(getParticipants());

				if (Configuration.Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}

				if (Configuration.Settings.getNoHunger()) {
					(new GameTimer(TaskType.INFINITE, -1) {
						public void onStart() {
							Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a배고픔 무제한이 적용됩니다."));
						}

						public void run(int count) {
							for (AbstractGame.Participant participant : AddonAbilityWar.this.getParticipants()) {
								participant.getPlayer().setFoodLevel(19);
							}
						}
					}).setPeriod(TimeUnit.TICKS, 1).start();
				} else {
					Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4배고픔 무제한이 &c적용되지 않습니다."));
				}

				if (this.invincible) {
					getInvincibility().start(false);
				} else {
					Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4초반 무적이 &c적용되지 않습니다."));
					setRestricted(false);
				}

				ScriptManager.runAll(this);
				startGame();
				break;
			}
		}
	}

	public AbilitySelect newAbilitySelect() {
		return new AbilitySelect(this, getParticipants(), 1) {
			private List<Class<? extends AbilityBase>> abilities;

			@Override
			public boolean changeAbility(Participant arg0) {
				Player p = arg0.getPlayer();
				if (this.abilities.size() > 0) {
					Random random = new Random();
					if (arg0.hasAbility()) {
						Class<? extends AbilityBase> old = arg0.getAbility().getClass();
						Class<? extends AbilityBase> newer = this.abilities.get(random.nextInt(this.abilities.size()));

						try {
							arg0.setAbility(newer);

							abilities.remove(newer);
							abilities.add(old);
							return true;
						} catch (Exception e) {
							Messager.sendConsoleMessage("능력을 배정받는 도중 오류가 발생하였습니다.");
							Messager.sendConsoleMessage("오류가 발생한 능력: " + newer.getName());
						}
					}
				} else {
					p.sendMessage("더이상 변경할 수 있는 능력이 존재하지 않습니다.");
				}
				return false;
			}

			@Override
			protected void drawAbility(Collection<? extends Participant> arg0) {
				this.abilities = AddonAbilityFactory.getAddonAbilities();
				if (getSelectors().size() <= this.abilities.size()) {
					Random random = new Random();
					for (AbstractGame.Participant participant : arg0) {
						Player p = participant.getPlayer();
						Class<? extends AbilityBase> abilityClass = this.abilities
								.get(random.nextInt(this.abilities.size()));
						try {
							participant.setAbility(abilityClass);
							this.abilities.remove(abilityClass);
							p.sendMessage("§a당신에게 능력을 배정하였습니다. §e/aw check §a명령어를 통해 확인이 가능합니다.");
							p.sendMessage("§e/aw yes §a명령어를 통해 능력을 확정할 수 있습니다.");
							p.sendMessage("§e/aw no §a명령어를 통해 능력을 변경할 수 있습니다.");
						} catch (Exception e) {
							Messager.sendConsoleMessage("능력을 배정받는 도중 오류가 발생하였습니다.");
							Messager.sendConsoleMessage("오류가 발생한 능력: " + abilityClass.getName());
						}
					}
				}
			}
		};
	}

	@Override
	public void executeCommand(CommandType commandType, CommandSender sender, String command, String[] args, Plugin plugin) {
		Player targetPlayer;
		int count;
		switch (commandType) {
		case ABI:
			if (args.length == 1) {
				if (sender instanceof Player) {
					Player p = (Player) sender;
					if (args[0].equalsIgnoreCase("@a")) {
						AddonAbilityGui g = new AddonAbilityGui(p, plugin);
						g.openGUI(1);
					} else {
						targetPlayer = Bukkit.getPlayerExact(args[0]);
						if (targetPlayer != null) {
							AbstractGame game = getGame();
							if (game.isParticipating(targetPlayer)) {
								AbstractGame.Participant target = game.getParticipant(targetPlayer);
								AddonAbilityGui gui = new AddonAbilityGui(p, target, plugin);
								gui.openGUI(1);
							} else {
								p.sendMessage("해당 플레이어는 게임에 참가하지 않았거나 탈락한 플레이어입니다.");
							}
						} else {
							p.sendMessage("해당 플레이어는 존재하지 않는 플레이어입니다.");
						}
					}
					break;
				} else Messager.sendErrorMessage(sender, "콘솔에서 사용할 수 없는 명령어입니다.");
			} else {
				String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				
				if (AddonAbilityFactory.getByString(name) != null) {
					if (args[0].equalsIgnoreCase("@a")) {
						try {
							for (Participant participant : GameManager.getGame().getParticipants()) {
								participant.setAbility(AddonAbilityFactory.getByString(name));
							}
							Bukkit.broadcastMessage("§e" + sender.getName() + "§a님이 §f모든 참가자§a에게 능력을 임의로 부여하였습니다.");
						} catch (Exception e) {
							Messager.sendErrorMessage(sender, "능력 설정 도중 오류가 발생하였습니다.");
							if (DeveloperSettings.isEnabled()) e.printStackTrace();
						}
					} else {
						targetPlayer = Bukkit.getPlayerExact(args[0]);
						if (targetPlayer != null) {
							AbstractGame game = GameManager.getGame();
							if (game.isParticipating(targetPlayer)) {
								try {
									game.getParticipant(targetPlayer).setAbility(AddonAbilityFactory.getByString(name));
									Bukkit.broadcastMessage("§e" + sender.getName() + "§a님이 §f" + targetPlayer.getName() + "§a님에게 능력을 임의로 부여하였습니다.");
								} catch (Exception e) {
									Messager.sendErrorMessage(sender, "능력 설정 도중 오류가 발생하였습니다.");
									if (DeveloperSettings.isEnabled()) e.printStackTrace();
								}
							} else
								Messager.sendErrorMessage(sender, targetPlayer.getName() + "님은 탈락했거나 게임에 참여하지 않았습니다.");
						} else
							Messager.sendErrorMessage(sender, args[0] + KoreanUtil.getJosa(args[0], Josa.은는) + " 존재하지 않는 플레이어입니다.");
					}
				}
			}
			break;
		case ABLIST:
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2===== &a능력자 목록 &2====="));
			count = 0;
			for (AbstractGame.Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility()) {
					count++;
					AbilityBase ability = participant.getAbility();
					String name = ability.getName();
					if (name != null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
								"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: &c" + name));
					}
				}
			}
			if (count == 0) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f능력자가 발견되지 않았습니다."));
			}

			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2========================"));

			Bukkit.broadcastMessage(
					ChatColor.translateAlternateColorCodes('&', "&f" + sender.getName() + "&a님이 플레이어들의 능력을 확인하였습니다."));
			break;
		}
	}
}
