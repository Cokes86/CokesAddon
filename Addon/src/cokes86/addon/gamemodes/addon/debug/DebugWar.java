package cokes86.addon.gamemodes.addon.debug;

import static daybreak.abilitywar.game.GameManager.getGame;

import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.manager.object.WRECK;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;

@GameManifest(name = "코크스애드온 디버그", description = { "§r어빌리티 테스트용." })
@Category(value = Category.GameCategory.DEBUG)
public class DebugWar extends AbstractGame implements Listener, WRECK.Handler {
	private final WRECK wreck = newWRECK();

	public DebugWar() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
	}

	@Override
	public void startGame() {
		super.startGame();
	}

	@Override
	protected void run(int seconds) {
		if (seconds == 1) {
			startGame();
			Bukkit.broadcastMessage("코크스애드온 디버그 모드 시작");
			setRestricted(false);
		}
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
						DebugAbilityGui g = new DebugAbilityGui(p, plugin);
						g.openGUI(1);
					} else {
						targetPlayer = Bukkit.getPlayerExact(args[0]);
						if (targetPlayer != null) {
							AbstractGame game = getGame();
							if (game.isParticipating(targetPlayer)) {
								AbstractGame.Participant target = game.getParticipant(targetPlayer);
								DebugAbilityGui gui = new DebugAbilityGui(p, target, plugin);
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
			}
			break;
		case ABLIST:
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2===== &a능력자 목록 &2====="));
			count = 0;
			for (AbstractGame.Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility()) {
					count++;
					AbilityBase ability = participant.getAbility();
					if (ability.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ability;
						if (mix.hasSynergy()) {
							Synergy synergy = mix.getSynergy();
							Pair<AbilityFactory.AbilityRegistration, AbilityFactory.AbilityRegistration> base = SynergyFactory
									.getSynergyBase(synergy.getRegistration());
							String name = "&e" + synergy.getName() + " &f(&c"
									+ base.getLeft().getManifest().name()
									+ " &f+ &c"
									+ base.getRight().getManifest().name()
									+ "&f)";
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: " + name));
						} else {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: &c"
											+ mix.getFirst().getName() + " &f+ &c" + mix.getSecond().getName()));
						}
					} else {
						String name = ability.getName();
						if (name != null) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: &c" + name));
						}
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
		default:
			super.executeCommand(commandType, sender, command, args, plugin);
			break;
		}
	}

	@Override
	public WRECK getWRECK() {
		return wreck;
	}

	@Override
	public boolean isWRECKEnabled() {
		return wreck.isEnabled();
	}
}
