package cokes86.addon.gamemodes.targethunting;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.configuration.gamemode.GameConfiguration;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.manager.object.Invincibility;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.BroadBar;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static daybreak.abilitywar.game.GameManager.stopGame;

@GameManifest(name = "타겟 헌팅", description = {
		"§r게임 내 존재하는 타겟을 찾아 그 사람을 헌팅해라!"
})
@Category(value = Category.GameCategory.GAME)
@GameAliases(value = {"타겟"})
public class TargetHunting extends Game implements Winnable, DeathManager.Handler, DefaultKitHandler, AbstractGame.Observer {
	private static final Logger logger = Logger.getLogger(TargetHunting.class);
	private final int inv = GameConfiguration.Config.getInt(GameNodes.TARGET_HUNTING_INV), game = GameConfiguration.Config.getInt(GameNodes.TARGET_HUNTING_GAME_TIME);
	private Participant target;
	private final GameTimer phase2 = new GameTimer(TaskType.REVERSE, game) {
		private BroadBar bar;

		protected void onStart() {
			bar = new BroadBar("", BarColor.GREEN, BarStyle.SEGMENTED_20);
		}

		@Override
		protected void run(int i) {
			bar.setTitle("남은 시간 : " + TimeUtil.parseTimeAsString(i));
			bar.setProgress(Math.min(1.0, (double) getFixedCount() / game));

			if (i == getMaximumCount() / 2 || (i <= 5 && i >= 1)) {
				Bukkit.broadcastMessage("게임 종료까지 §c" + TimeUtil.parseTimeAsString(i) + " §f전");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
			}
		}

		@Override
		protected void onEnd() {
			bar.unregister();
			Win(target);
		}

		@Override
		protected void onSilentEnd() {
			bar.unregister();
		}
	};
	private int phase = 0;

	public TargetHunting() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		setRestricted(true);
	}

	@Override
	protected void progressGame(int i) {
		switch (i) {
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
				Bukkit.broadcastMessage("§b코크스애드온 §f타겟 헌팅 게임모드 제작자 : §aCokes_86");
				Bukkit.broadcastMessage("§b타겟§f을 찾아 해당 플레이어를 죽이십시오.");

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);

				for (String line : event.getCredits()) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Configuration.Settings.getDrawAbility()) {
					for (String line : Messager.asList(ChatColor.translateAlternateColorCodes('&',
							"&f능력자에는 총 &b" + AbilityList.values().size()
									+ "개&f의 능력이 등록되어 있습니다."),
							ChatColor.translateAlternateColorCodes('&', "&7능력이 무작위로 배정됩니다..."))) {
						Bukkit.broadcastMessage(line);
					}
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§8타겟을 배정합니다.");
				break;
			case 10:
				try {
					startAbilitySelect();
				} catch (OperationNotSupportedException e) {
					e.printStackTrace();
				}
				break;
			case 15:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e잠시 후 게임이 시작됩니다."));
				break;
			case 17:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c5&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 18:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c4&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 19:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c3&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 20:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c2&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 21:
				Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e게임이 &c1&e초 후에 시작됩니다."));
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 22:
				for (String line : Messager.asList(
						ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"),
						ChatColor.translateAlternateColorCodes('&', "&f            &cTarget - 타겟 헌팅 전쟁            "),
						ChatColor.translateAlternateColorCodes('&', "&f                    게임 시작                "),
						ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"))) {
					Bukkit.broadcastMessage(line);

					if (Configuration.Settings.getSpawnEnable()) {
						Location spawn = Configuration.Settings.getSpawnLocation().toBukkitLocation();
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
								for (AbstractGame.Participant participant : getParticipants()) {
									participant.getPlayer().setFoodLevel(19);
								}
							}
						}).setPeriod(TimeUnit.TICKS, 1).start();
					} else {
						Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4배고픔 무제한이 &c적용되지 않습니다."));
					}
					startGame();
					this.phase = 1;
					Bukkit.broadcastMessage("타겟은 " + TimeUtil.parseTimeAsString(inv) + "간 도망치십시오.");
					for (Participant participant : getParticipants()) {
						if (participant.equals(target)) continue;
						participant.getPlayer().setGameMode(GameMode.SPECTATOR);
						PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), Integer.MAX_VALUE, 0, true);
					}
					getInvincibility().attachObserver(new Invincibility.Observer() {
						@Override
						public void onStart() {
						}

						@Override
						public void onEnd() {
							for (Participant participant : getParticipants()) {
								if (participant.equals(target)) {
									target.getPlayer().sendMessage("끝까지 살아남아 승리하십시오.");
									continue;
								}
								participant.getPlayer().setGameMode(GameMode.SURVIVAL);
								PotionEffects.BLINDNESS.removePotionEffect(participant.getPlayer());
								participant.getPlayer().sendMessage("타겟을 찾아 게임에서 승리하십시오.");
							}
							phase = 2;
							phase2.start();
							setRestricted(false);
						}
					});
					getInvincibility().start(inv);
					break;
				}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (isParticipating(player.getUniqueId())) {
			if (phase == 1) {
				if (player.equals(target.getPlayer())) return;
				e.setTo(e.getFrom());
			}
		}
	}

	@Override
	public DeathManager newDeathManager() {
		return new DeathManager(this);
	}

	@Override
	public void update(GameUpdate gameUpdate) {
		if (gameUpdate == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}

	@Override
	public AbilitySelect newAbilitySelect() {
		return new AbilitySelect(this, getParticipants(), 2) {
			final Random random = new Random();
			private List<Class<? extends AbilityBase>> abilities;

			protected Collection<? extends Participant> filterSelectors(Collection<? extends Participant> collection) {
				return Collections.singletonList(new ArrayList<>(collection)).get(random.nextInt(collection.size()));
			}

			@Override
			protected void drawAbility(Collection<? extends Participant> collection) {
				target = new ArrayList<>(collection).get(0);
				abilities = getTargetAbilities();
				Player p = target.getPlayer();
				Bukkit.broadcastMessage(p.getName() + "님이 타겟으로 선정되었습니다.");

				Class<? extends AbilityBase> abilityClass = abilities.get(random.nextInt(abilities.size()));

				try {
					target.setAbility(abilityClass);
					p.sendMessage(new String[]{
							"§a당신이 타겟입니다. 능력이 할당되었습니다. §e/aw check§f로 확인 할 수 있습니다.",
							"§e/aw yes §f명령어를 사용하여 능력을 확정합니다.",
							"§e/aw no §f명령어를 사용하여 능력을 변경합니다."
					});
					abilities.remove(abilityClass);
				} catch (IllegalAccessException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException e) {
					logger.error(ChatColor.YELLOW + target.getPlayer().getName() + ChatColor.WHITE + "님에게 능력을 할당하는 도중 오류가 발생하였습니다.");
					logger.error("문제가 발생한 능력: " + ChatColor.AQUA + abilityClass.getName());
				}
			}

			@Override
			protected boolean changeAbility(Participant participant) {
				if (participant.equals(target)) {
					if (abilities.size() > 0) {
						if (participant.hasAbility()) {
							Class<? extends AbilityBase> abilityClass = abilities.get(random.nextInt(abilities.size()));
							try {
								participant.setAbility(abilityClass);
								participant.getPlayer().sendMessage(new String[]{
										"§a당신의 능력이 변경되었습니다.",
								});
								abilities.remove(abilityClass);
								return true;
							} catch (IllegalAccessException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException e) {
								logger.error(ChatColor.YELLOW + participant.getPlayer().getName() + ChatColor.WHITE + "님에게 능력을 할당하는 도중 오류가 발생하였습니다.");
								logger.error("문제가 발생한 능력: " + ChatColor.AQUA + abilityClass.getName());
							}
						}
					} else {
						participant.getPlayer().sendMessage("변경할 능력이 존재하지 않습니다.");
					}
				} else {
					participant.getPlayer().sendMessage("당신은 능력을 변경할 수 없습니다.");
				}
				return false;
			}
		};
	}

	private List<Class<? extends AbilityBase>> getTargetAbilities() {
		List<Class<? extends AbilityBase>> abilities = AbilitySelect.AbilityCollector.EVERY_ABILITY_EXCLUDING_BLACKLISTED.collect(TargetHunting.class);
		for (Class<? extends AbilityBase> clazz : new ArrayList<>(abilities)) {
			if (AbilityFactory.getRegistration(clazz).getManifest().rank() == AbilityManifest.Rank.A || AbilityFactory.getRegistration(clazz).getManifest().rank() == AbilityManifest.Rank.S)
				continue;
			abilities.remove(clazz);
		}

		return abilities;
	}

	@EventHandler
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getParticipant().equals(target)) {
			List<Participant> winner = new ArrayList<>(getParticipants());
			winner.remove(target);
			Win(winner.toArray(new Participant[]{}));
		}
	}
}
