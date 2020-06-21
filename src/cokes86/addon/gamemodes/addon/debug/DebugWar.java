package cokes86.addon.gamemodes.addon.debug;

import static daybreak.abilitywar.game.GameManager.getGame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.list.mixability.Mix;
import daybreak.abilitywar.game.list.mixability.synergy.Synergy;
import daybreak.abilitywar.game.list.mixability.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.object.CommandHandler;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;

@GameManifest(name = "코크스애드온 디버그", description = { "§r어빌리티 테스트용." })
public class DebugWar extends AbstractGame implements Listener {

	public DebugWar() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
	}

	@Override
	public void startGame() {
		super.startGame();
	}

	@Override
	protected void run(int seconds) {
		switch (seconds) {
		case 1:
			startGame();
			Bukkit.broadcastMessage("코크스애드온 디버그 모드 시작");
			setRestricted(false);
		}
	}

	@Override
	public void executeCommand(CommandHandler.CommandType type, Player p, String[] args, Plugin plugin) {
		Player targetPlayer;
		int count;
		switch (type) {
		case ABI:
			if (args[0].equalsIgnoreCase("@a")) {
				DebugAbilityGui g = new DebugAbilityGui(p, plugin);
				g.openGUI(1);
				break;
			} else {
				targetPlayer = Bukkit.getPlayerExact(args[0]);
				if (targetPlayer != null) {
					AbstractGame game = getGame();
					if (game.isParticipating(targetPlayer)) {
						AbstractGame.Participant target = game.getParticipant(targetPlayer);
						DebugAbilityGui gui = new DebugAbilityGui(p, target, plugin);
						gui.openGUI(1);
						break;
					} else {
						p.sendMessage("해당 플레이어는 게임에 참가하지 않았거나 탈락한 플레이어입니다.");
						break;
					}
				} else {
					p.sendMessage("해당 플레이어는 존재하지 않는 플레이어입니다.");
					break;
				}
			}
		case ABLIST:
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2===== &a능력자 목록 &2====="));
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
									+ ((AbilityFactory.AbilityRegistration) base.getLeft()).getManifest().name()
									+ " &f+ &c"
									+ ((AbilityFactory.AbilityRegistration) base.getRight()).getManifest().name()
									+ "&f)";
							p.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: " + name));
						} else {
							p.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: &c"
											+ mix.getFirst().getName() + " &f+ &c" + mix.getSecond().getName()));
						}
					} else {
						String name = ability.getName();
						if (name != null) {
							p.sendMessage(ChatColor.translateAlternateColorCodes('&',
									"&e" + count + ". &f" + participant.getPlayer().getName() + " &7: &c" + name));
						}
					}
				}
			}
			if (count == 0) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f능력자가 발견되지 않았습니다."));
			}

			p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2========================"));

			Bukkit.broadcastMessage(
					ChatColor.translateAlternateColorCodes('&', "&f" + p.getName() + "&a님이 플레이어들의 능력을 확인하였습니다."));
			break;
		}
	}
}
