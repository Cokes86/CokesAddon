package cokes86.addon.gamemodes.battleability;

import cokes86.addon.configuration.gamemode.GameConfiguration;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.*;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.*;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@GameManifest(name = "자기장 믹스 능력자 전쟁", description = {
        "§f맵의 크기가 서서히 줄어든다!",
        "§f줄어들기 전에 얼른 사냥해라!",
        "§f※게임 시작 전에 게임 스폰을 지정해주세요."
})
@Category(value = Category.GameCategory.GAME)
@GameAliases(value = {"자기장믹스", "자믹스"})
public class BattleMixAbility extends AbstractMix implements DefaultKitHandler, Winnable, AbstractGame.Observer {
    private final Location location;
    private final World world;
    private final double size;
    private final Border border = new Border(this);
    private static final Logger logger = Logger.getLogger(BattleMixAbility.class);

    public BattleMixAbility() {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
        setRestricted(Configuration.Settings.InvincibilitySettings.isEnabled());
        attachObserver(this);
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        world = Configuration.Settings.getSpawnLocation().getWorld();
        WorldBorder wb = world.getWorldBorder();

        location = wb.getCenter();
        size = wb.getSize();
    }

    @Override
    public void update(GameUpdate gameUpdate) {
        if (gameUpdate == GameUpdate.END) {
            HandlerList.unregisterAll(this);
            WorldBorder wb = world.getWorldBorder();
            wb.setCenter(location);
            wb.setSize(size);
        }
    }

    @Override
    public AbilitySelect newAbilitySelect() {
        return new AbilitySelect(this, getParticipants(), 1) {

            private List<Class<? extends AbilityBase>> abilities;

            @Override
            protected void drawAbility(Collection<? extends Participant> selectors) {
                abilities = AbilitySelectStrategy.EVERY_ABILITY_EXCLUDING_BLACKLISTED.getAbilities();
                if (abilities.size() > 0) {
                    Random random = new Random();

                    for (Participant participant : selectors) {
                        Player p = participant.getPlayer();

                        Class<? extends AbilityBase> abilityClass = abilities.get(random.nextInt(abilities.size()));
                        Class<? extends AbilityBase> secondAbilityClass = abilities.get(random.nextInt(abilities.size()));
                        try {
                            ((Mix) participant.getAbility()).setAbility(abilityClass, secondAbilityClass);

                            p.sendMessage(new String[]{
                                    "§a능력이 할당되었습니다. §e/aw check§f로 확인 할 수 있습니다.",
                                    "§e/aw yes §f명령어를 사용하여 능력을 확정합니다.",
                                    "§e/aw no §f명령어를 사용하여 능력을 변경합니다."
                            });
                        } catch (IllegalAccessException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException e) {
                            logger.error(ChatColor.YELLOW + participant.getPlayer().getName() + ChatColor.WHITE + "님에게 능력을 할당하는 도중 오류가 발생하였습니다.");
                            logger.error("문제가 발생한 능력: " + ChatColor.AQUA + abilityClass.getName());
                        }
                    }
                } else {
                    Messager.broadcastErrorMessage("사용 가능한 능력이 없습니다.");
                    GameManager.stopGame();
                }
            }

            @Override
            protected boolean changeAbility(Participant participant) {
                Player p = participant.getPlayer();

                if (abilities.size() > 0) {
                    Random random = new Random();

                    if (participant.hasAbility()) {
                        Class<? extends AbilityBase> abilityClass = abilities.get(random.nextInt(abilities.size()));
                        Class<? extends AbilityBase> secondAbilityClass = abilities.get(random.nextInt(abilities.size()));
                        try {
                            ((Mix) participant.getAbility()).setAbility(abilityClass, secondAbilityClass);
                            return true;
                        } catch (Exception e) {
                            logger.error(ChatColor.YELLOW + p.getName() + ChatColor.WHITE + "님의 능력을 변경하는 도중 오류가 발생하였습니다.");
                            logger.error(ChatColor.WHITE + "문제가 발생한 능력: " + ChatColor.AQUA + abilityClass.getName());
                        }
                    }
                } else {
                    Messager.sendErrorMessage(p, "능력을 변경할 수 없습니다.");
                }

                return false;
            }
        };
    }

    @Override
    protected void progressGame(int i) {
        switch (i) {
            case 1:
                List<String> lines = Messager.asList(ChatColor.translateAlternateColorCodes('&', "&5==== &d게임 참여자 목록 &5===="));
                int count = 0;
                for (Participant p : getParticipants()) {
                    count++;
                    lines.add(ChatColor.translateAlternateColorCodes('&', "&d" + count + ". &f" + p.getPlayer().getName()));
                }
                lines.add(ChatColor.translateAlternateColorCodes('&', "&5총 인원수 : " + count + "명"));
                lines.add(ChatColor.translateAlternateColorCodes('&', "&5=========================="));

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
                        ChatColor.translateAlternateColorCodes('&', "&cAbilityWar &f- &6자기장 믹스 능력자 전쟁"),
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
                if (Configuration.Settings.getDrawAbility()) {
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
                if (Configuration.Settings.getDrawAbility()) {
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
                        ChatColor.translateAlternateColorCodes('&', "&f             &cAbilityWar &f- &6자기장 능력자 믹스 전쟁  "),
                        ChatColor.translateAlternateColorCodes('&', "&f                    게임 시작                "),
                        ChatColor.translateAlternateColorCodes('&', "&e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"))) {
                    Bukkit.broadcastMessage(line);
                }

                giveDefaultKit(getParticipants());

                Location spawn = Configuration.Settings.getSpawnLocation();
                for (Participant participant : getParticipants()) {
                    participant.getPlayer().teleport(spawn);
                }

                if (Configuration.Settings.getNoHunger()) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&2배고픔 무제한&a이 적용됩니다."));
                } else {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4배고픔 무제한&c이 적용되지 않습니다."));
                }

                if (Configuration.Settings.getInfiniteDurability()) {
                    attachObserver(new InfiniteDurability());
                } else {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&4내구도 무제한&c이 적용되지 않습니다."));
                }

                if (Configuration.Settings.getClearWeather()) {
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
                    wb.setSize(GameConfiguration.Config.getInt(GameNodes.BattleAbility_startSize));
                    wb.setSize(1, GameConfiguration.Config.getInt(GameNodes.BattleAbility_time));
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
                switch (Configuration.Settings.DeathSettings.getOperation()) {
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

            @Override
            protected String getRevealMessage(Participant victim) {
                final Mix mix = (Mix) victim.getAbility();
                if (mix.hasAbility()) {
                    if (mix.hasSynergy()) {
                        final Synergy synergy = mix.getSynergy();
                        final Pair<AbilityFactory.AbilityRegistration, AbilityFactory.AbilityRegistration> base = SynergyFactory.getSynergyBase(synergy.getRegistration());
                        final String name = synergy.getName() + " (" + base.getLeft().getManifest().name() + " + " + base.getRight().getManifest().name() + ")";
                        return "§f[§c능력§f] §c" + victim.getPlayer().getName() + "§f님의 능력은 §e" + name + "§f" + KoreanUtil.getJosa(name, KoreanUtil.Josa.이었였) + "습니다.";
                    } else {
                        final String name = mix.getFirst().getName() + " + " + mix.getSecond().getName();
                        return "§f[§c능력§f] §c" + victim.getPlayer().getName() + "§f님의 능력은 §e" + name + "§f" + KoreanUtil.getJosa(name, KoreanUtil.Josa.이었였) + "습니다.";
                    }
                } else {
                    return "§f[§c능력§f] §c" + victim.getPlayer().getName() + "§f님은 능력이 없습니다.";
                }
            }
        };
    }

    @Override
    public Invincibility getInvincibility() {
        return border;
    }
}
