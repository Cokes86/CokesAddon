package cokes86.addon.gamemodes.tailcatching;

import cokes86.addon.ability.AddonAbilityFactory;
import cokes86.addon.configuration.gamemode.GameConfiguration;
import cokes86.addon.configuration.gamemode.GameNodes;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;

import javax.naming.OperationNotSupportedException;
import java.util.*;

import static daybreak.abilitywar.game.GameManager.stopGame;

@GameManifest(name = "꼬리잡기 능력자 전쟁", description = { "§f한국의 민속놀이가 능력자 전쟁으로!", "§f자신의 목표를 찾아 죽여내자!" })
public class TailCatching extends Game implements DefaultKitHandler, Winnable, AbstractGame.Observer {
    private final boolean invincible;
    ArrayList<Participant> list;
    Map<Participant, Pair<Participant.ActionbarNotification.ActionbarChannel, Participant.ActionbarNotification.ActionbarChannel>> channel = new HashMap<>();
    private final int range;

    public TailCatching() {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
        this.invincible = Configuration.Settings.InvincibilitySettings.isEnabled();
        setRestricted(this.invincible);
        attachObserver(this);
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        this.range = GameConfiguration.Config.getInt(GameNodes.TailCatching_range);
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
                this.list = new ArrayList<>(getParticipants());
                for (Participant participant : list) {
                    channel.put(participant, Pair.of(participant.actionbar().newChannel(), participant.actionbar().newChannel()));
                }
                suffle();
                break;
            case 3:
                Bukkit.broadcastMessage("§b코크스애드온 §f꼬리잡기 능력자 전쟁 게임모드 제작자 : §aCokes_86");

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
                        ChatColor.translateAlternateColorCodes('&', "&f            &cTail Cathing - 꼬리잡기 능력 전쟁            "),
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
                                for (AbstractGame.Participant participant : getParticipants()) {
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
                    new CheckTargetTimer();
                    startGame();
                    break;
                }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity(), killer = e.getEntity().getKiller();
        if (killer != null && isParticipating(killed) && isParticipating(killer)) {
            Participant killedParticipant = getParticipant(killed), killerParticipant = getParticipant(killer);
            if (list.contains(killedParticipant) && list.contains(killerParticipant)) {
                channel.get(killedParticipant).getLeft().unregister();
                channel.get(killedParticipant).getRight().unregister();
                channel.remove(killedParticipant);

                int killedIndex = list.indexOf(killedParticipant), killerIndex = list.indexOf(killerParticipant);

                if (killerIndex + 1 != killedIndex) {
                    killer.setHealth(0);
                    channel.get(killerParticipant).getLeft().unregister();
                    channel.get(killerParticipant).getRight().unregister();
                    channel.remove(killerParticipant);
                    Bukkit.broadcastMessage("[꼬리잡기] 이런! 자신의 타겟을 못잡은 "+killer.getName()+"님도 힘께 죽습니다!");
                } else if (!(killerIndex == list.size()-1 && killedIndex == 0)) {
                    killer.setHealth(0);
                    channel.get(killerParticipant).getLeft().unregister();
                    channel.get(killerParticipant).getRight().unregister();
                    channel.remove(killerParticipant);
                    Bukkit.broadcastMessage("[꼬리잡기] 이런! 자신의 타겟을 못잡은 "+killer.getName()+"님도 힘께 죽습니다!");
                }
            }
        }
    }

    public void suffle() {
        Random r = new Random();
        for (int a = 0 ; a < 300 ; a++) {
            Participant temp;
            int index1 = r.nextInt(list.size()-1);
            int index2 = r.nextInt(list.size()-1);
            temp = list.get(index2);
            list.set(index2, list.get(index1));
            list.set(index1, temp);
        }
    }

    @Override
    public DeathManager newDeathManager() {
        return new DeathManager(this) {
            public void Operation(Participant victim) {
                switch (Configuration.Settings.DeathSettings.getOperation()) {
                    case 탈락:
                        Eliminate(victim);
                    case 관전모드:
                    case 없음:
                        victim.getPlayer().setGameMode(GameMode.SPECTATOR);
                        excludedPlayers.add(victim.getPlayer().getUniqueId());
                        list.remove(victim);
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
        }
    }

    class CheckTargetTimer extends GameTimer {

        public CheckTargetTimer() {
            super(TaskType.INFINITE, -1);
            setPeriod(TimeUnit.TICKS, 1);
            start();
        }

        @Override
        protected void run(int i) {
            if (!TailCatching.this.invincible || !getInvincibility().isEnabled()) {
                for (Participant participant1 : list) {
                    for (Participant participant2 : list) {
                        if (participant1 == participant2) continue;
                        int index1 = list.indexOf(participant1);
                        int index2 = list.indexOf(participant2);

                        double length = participant1.getPlayer().getLocation().subtract(participant2.getPlayer().getLocation()).length();

                        if ((index1 + 1 == index2 || (index1 == list.size()-1 && index2 == 0)) && length <= range) {
                            channel.get(participant1).getLeft().update("§c§l타겟 접근 중");
                            channel.get(participant2).getRight().update("§c§l누군가가 당신을 노리는 중");
                        } else {
                            channel.get(participant1).getLeft().update(null);
                            channel.get(participant2).getRight().update(null);
                        }
                    }
                }
            }
        }
    }
}
