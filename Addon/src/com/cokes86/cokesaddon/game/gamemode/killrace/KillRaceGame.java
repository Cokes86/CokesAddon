package com.cokes86.cokesaddon.game.gamemode.killrace;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.enums.OnDeath;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.config.kitpreset.KitConfiguration;
import daybreak.abilitywar.config.serializable.KitPreset;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.module.Invincibility;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@GameManifest(
        name = "킬 레이스",
        description = {
                "§f목표 킬 수를 먼저 달성하면 승리합니다.",
                "§f사망 시 관전으로 대기 후 부활합니다.",
                "§f킬러가 있는 죽음만 능력 재배정/가중치가 적용됩니다.",
                "§f연속킬(기본 3)마다 능력이 재배정됩니다.",
                "§f시너지는 1킬 시 즉시 재배정됩니다."
        }
)
@GameAliases({"킬전", "킬레이스", "KillRace"})
public class KillRaceGame extends Game implements Winnable, org.bukkit.event.Listener, DefaultKitHandler {

  // ====== Settings ======
  public static final Setting<Integer> KILL_GOAL =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "kill-goal", 30, "# 고정 목표 킬 수") {
            @Override public boolean condition(Integer v) { return v >= 1; }
          };

  public static final Setting<Boolean> USE_DYNAMIC_KILL_GOAL =
          gameSettings.new Setting<Boolean>(KillRaceGame.class, "use-dynamic-kill-goal", true,
                  "# true면 목표 킬 수 = 참가자 수 × multiplier") {
            @Override public boolean condition(Boolean v) { return true; }
          };

  public static final Setting<Integer> KILL_GOAL_MULTIPLIER =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "kill-goal-multiplier", 3,
                  "# use-dynamic-kill-goal=true일 때 목표 킬 = 참가자 수 × multiplier / 연속킬 재배정 기준으로도 사용") {
            @Override public boolean condition(Integer v) { return v >= 1; }
          };

  public static final Setting<Integer> RESPAWN_DELAY =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "respawn-delay", 20,
                  "# 사망 후 부활까지 지연(초)") {
            @Override public boolean condition(Integer v) { return v >= 0; }
          };

  public static final Setting<Integer> SYNERGY_STREAK =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "synergy-streak", 5, "# 연속 데스 몇 번이면 시너지 강제") {
            @Override public boolean condition(Integer v) { return v >= 1; }
          };

  public static final Setting<Integer> INVINCIBLE_SEC =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "invincible-sec", 3, "# 부활 후 무적 시간(초)") {
            @Override public boolean condition(Integer v) { return v >= 0; }
          };

  public static final Setting<Integer> BORDER_MARGIN =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "border-margin", 8, "# 월드보더 안쪽 여유(블록)") {
            @Override public boolean condition(Integer v) { return v >= 0; }
          };

  public static final Setting<Boolean> ALLOW_SPECIAL =
          gameSettings.new Setting<Boolean>(KillRaceGame.class, "allow-special", false, "# SPECIAL 등급 포함 여부") {
            @Override public boolean condition(Boolean v) { return true; }
          };

  public static final Setting<Boolean> USE_WEIGHT =
          gameSettings.new Setting<Boolean>(KillRaceGame.class, "use-weight", false, "# 등급 별 배정 균등치 사용 여부") {
            @Override public boolean condition(Boolean v) { return true; }
          };

  public static final Setting<Integer> KILL_LOCK_TIME =
          gameSettings.new Setting<Integer>(KillRaceGame.class, "kill-lock-time", 3, "# 상대방을 죽인 후 이후 사람을 죽일 수 없는 시간(초)") {
            @Override public boolean condition(Integer v) { return v >= 0; }
          };

  public static final Setting<Boolean> USE_KIT_PRESET = gameSettings.new Setting<Boolean>(
          KillRaceGame.class,
          "use-kit-preset",
          true,
          "# 킬레이스 시작 시 AbilityWar 기본 킷 프리셋을 지급할지 여부"
  )
  {
    @Override public boolean condition(Boolean v) { return true; }
  };

  public static final Setting<Integer> KIT_PRESET_INDEX = gameSettings.new Setting<Integer>(
          KillRaceGame.class,
          "kit-preset-index",
          0,
          "# 사용할 AbilityWar 기본 킷 프리셋 번호",
          "# 첫 번째 프리셋은 0, 두 번째 프리셋은 1입니다.",
          "# AbilityWar 킷프리셋 GUI에서 킬레이스용 프리셋을 만든 뒤 번호를 지정하세요."
  ) {
    @Override public boolean condition(Integer v) { return v >= 0; }
  };

  private static final int BORDER_EXTRA_MARGIN = 6;

  // ====== State ======
  private final Random random = new Random();

  private static class PlayerState {
    int deathStreak = 0;
    boolean lowBiasNextAssign = false;
    int killStreak = 0;
  }

  private void giveKillRaceKit(final Participant participant) {
    if (!USE_KIT_PRESET.getValue()) return;

    final Player player = participant.getPlayer();
    final KitPreset kit = getKillRaceKitPreset(player);

    if (kit == null) {
      player.sendMessage("§c지급 가능한 킷을 찾을 수 없습니다.");
      return;
    }

    giveKitPreset(player, kit);
  }

  private KitPreset getKillRaceKitPreset(final Player player) {
    final List<?> presets = KitConfiguration.KitSettings.getKitPresets();
    final int index = KIT_PRESET_INDEX.getValue();

    if (presets != null && !presets.isEmpty()) {
      if (index >= 0 && index < presets.size()) {
        final Object object = presets.get(index);

        if (object instanceof KitPreset) {
          return (KitPreset) object;
        }

        player.sendMessage("§c킬레이스 프리셋 #" + index + "을 불러올 수 없어 기본킷을 지급합니다.");
        return KitConfiguration.KitSettings.getKit();
      }

      player.sendMessage("§c킬레이스 프리셋 번호가 범위를 벗어났습니다: §f" + index);
      player.sendMessage("§7기본킷을 대신 지급합니다.");
      return KitConfiguration.KitSettings.getKit();
    }

    return KitConfiguration.KitSettings.getKit();
  }

  private void giveKitPreset(final Player player, final KitPreset kit) {
    final PlayerInventory inv = player.getInventory();

    inv.clear();
    inv.setArmorContents(null);

    for (ItemStack object : kit.getItems()) {
      if (object != null) {
        inv.addItem(object.clone());
      }
    }

    inv.setHelmet(cloneOrNull(kit.getHelmet()));
    inv.setChestplate(cloneOrNull(kit.getChestplate()));
    inv.setLeggings(cloneOrNull(kit.getLeggings()));
    inv.setBoots(cloneOrNull(kit.getBoots()));

    player.updateInventory();
  }

  private ItemStack cloneOrNull(final ItemStack item) {
    return item != null ? item.clone() : null;
  }

  private final Map<UUID, PlayerState> states = new HashMap<>();
  private PlayerState st(Player p) { return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerState()); }

  private final Set<UUID> invincible = new HashSet<>();
  private final Map<UUID, Integer> kills = new HashMap<>();

  // ====== kill lock (연속 킬 방지) ======
  private final Map<UUID, Long> killLockUntil = new HashMap<>();

  private void lockKill(Player p, int seconds) {
    if (p == null || seconds <= 0) return;
    killLockUntil.put(p.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
  }

  private boolean isKillLocked(Player p) {
    if (p == null) return false;
    return killLockUntil.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
  }


  private int effectiveKillGoal = -1;
  private boolean ended = false;

  private final Objective killObjective;

  private boolean firstAbilityAssigned = false;
  private final boolean invincibleStart = Settings.InvincibilitySettings.isEnabled();

  // ====== Spawnpoint save/restore ======
  private final Map<UUID, Location> originalSpawnPoints = new HashMap<>();
  private Location gameSpawnPoint = null;

  // ====== Countdown tasks (respawn countdown) ======
  private final Map<UUID, BukkitRunnable> countdownTasks = new HashMap<>();

  // ====== Participant actionbar channels ======
  private final Map<UUID, ActionbarChannel> respawnChannels = new HashMap<>();

  // ====== "waiting respawn" state ======
  private final Set<UUID> waitingRespawn = new HashSet<>();

  // ====== pending ability reassign (killer death only) ======
  private final Set<UUID> pendingReassign = new HashSet<>();

  public KillRaceGame() {
    super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
    Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    killObjective = getScoreboardManager().registerNewObjective("kr_kills", "dummy", "§cKILLS");
  }

  @Override
  protected @NotNull DeathManager newDeathManager() {
    return new DeathManager(this)
            .setOperation(OnDeath.없음)
            .setAbilityRemoval(false)
            .setAutoRespawn(true)
            .setAbilityReveal(false);
  }

  private int getKillGoal() {
    return (effectiveKillGoal > 0) ? effectiveKillGoal : KILL_GOAL.getValue();
  }

  // ====== Rank color helpers ======
  private String rankColor(Rank r) {
    if (r == null) return "§f";
    String s = r.getRankName(); // 예: "§cC 등급"
    if (s != null && s.length() >= 2 && s.charAt(0) == '§') {
      return s.substring(0, 2); // "§c"
    }
    return "§f";
  }

  private String coloredAbilityName(AbilityBase ability) {
    if (ability == null) return "§f(없음)";
    AbilityRegistration reg = AbilityFactory.getRegistration(ability.getClass());
    Rank r = (reg != null && reg.getManifest() != null) ? reg.getManifest().rank() : null;
    return rankColor(r) + ability.getDisplayName();
  }

  // ====== TARGETABLE helper ======
  private void setTargetable(Player p, boolean value) {
    Participant pt = getParticipant(p);
    if (pt == null) return;
    pt.attributes().TARGETABLE.setValue(value);
  }

  // ====== Actionbar (Participant) helpers ======
  private ActionbarChannel getOrCreateRespawnChannel(Player player) {
    UUID id = player.getUniqueId();
    ActionbarChannel ch = respawnChannels.get(id);
    if (ch != null && ch.isValid()) return ch;

    Participant pt = getParticipant(player);
    if (pt == null) return null;

    ch = pt.actionbar().newChannel();
    respawnChannels.put(id, ch);
    return ch;
  }

  private void updateRespawnActionbar(Player player, String msg) {
    ActionbarChannel ch = getOrCreateRespawnChannel(player);
    if (ch != null) ch.update(msg);
  }

  private void clearRespawnActionbar(Player player) {
    UUID id = player.getUniqueId();
    ActionbarChannel ch = respawnChannels.remove(id);
    if (ch != null) {
      try { ch.unregister(); } catch (Throwable ignore) {}
    }
  }

  // ====== Game Flow ======
  @Override
  protected void progressGame(int seconds) {
    switch (seconds) {
      case 1: {
        List<String> lines = new ArrayList<>();
        lines.add("§d==== §f게임 참여자 목록 §d====");
        int count = 0;
        for (Participant p : getParticipants()) {
          count++;
          lines.add("§5" + count + ". §f" + p.getPlayer().getName());
        }
        lines.add("§f총 인원수 §5: §d" + count + "명");
        lines.add("§d==========================");
        for (String line : lines) Bukkit.broadcastMessage(line);

        if (getParticipants().size() < 2) {
          stop();
          Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§72명§8)");
        }
        break;
      }

      case 3: {
        ArrayList<String> msg = new ArrayList<>();
        msg.add("§5§l킬 레이스 §d§l능력 §f§l전쟁");
        msg.add("§e플러그인 버전 §7: §f" + AbilityWar.getPlugin().getDescription().getVersion());
        msg.add("§b모드 개발자 §7: §fCokes86 (Addon)");

        GameCreditEvent event = new GameCreditEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        msg.addAll(event.getCredits());

        for (String m : msg) Bukkit.broadcastMessage(m);
        break;
      }

      case 5: {
        Bukkit.broadcastMessage("§f플러그인에 총 §d" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.");
        Bukkit.broadcastMessage("§7첫 능력은 §f초반 무적 종료 후§7 배정됩니다.");
        Bukkit.broadcastMessage("§7부활 시간: §f" + RESPAWN_DELAY.getValue() + "초 §7(액션바 표시)");
        Bukkit.broadcastMessage("§7연속킬 재배정 기준: §f" + KILL_GOAL_MULTIPLIER.getValue() + "연속킬");
        break;
      }

      case 7: {
        killObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Participant participant : getParticipants()) {
          killObjective.getScore(participant.getPlayer().getName()).setScore(0);
        }
        Bukkit.broadcastMessage("§d잠시 후 §f게임이 시작됩니다.");
        break;
      }

      case 9:
        Bukkit.broadcastMessage("§f게임이 §55§f초 후에 시작됩니다."); SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound(); break;
      case 10:
        Bukkit.broadcastMessage("§f게임이 §54§f초 후에 시작됩니다."); SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound(); break;
      case 11:
        Bukkit.broadcastMessage("§f게임이 §53§f초 후에 시작됩니다."); SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound(); break;
      case 12:
        Bukkit.broadcastMessage("§f게임이 §52§f초 후에 시작됩니다."); SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound(); break;
      case 13:
        Bukkit.broadcastMessage("§f게임이 §51§f초 후에 시작됩니다."); SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound(); break;

      case 14: {
        for (String line : new String[]{
                "§d■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
                "§f                §5§l킬 레이스 §d§l능력 §f§l전쟁",
                "§f                    게임 시작                ",
                "§d■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■"}) {
          Bukkit.broadcastMessage(line);
        }
        SoundLib.ENTITY_WITHER_SPAWN.broadcastSound();

        ended = false;
        firstAbilityAssigned = false;

        if (USE_DYNAMIC_KILL_GOAL.getValue()) {
          int players = getParticipants().size();
          effectiveKillGoal = Math.max(1, players * KILL_GOAL_MULTIPLIER.getValue());
          Bukkit.broadcastMessage("§7목표 킬 수: §c" + effectiveKillGoal
                  + " §8(§f인원수 " + players + " × " + KILL_GOAL_MULTIPLIER.getValue() + "§8)");
        } else {
          effectiveKillGoal = KILL_GOAL.getValue();
          Bukkit.broadcastMessage("§7목표 킬 수: §c" + effectiveKillGoal + " §8(§f고정§8)");
        }

        for (Participant participant : getParticipants()) {
          giveKillRaceKit(participant);
        }

        if (Settings.getInfiniteDurability()) addModule(new InfiniteDurability());
        if (Settings.getClearWeather()) {
          for (World w : Bukkit.getWorlds()) w.setStorm(false);
        }

        kills.clear();
        states.clear();
        invincible.clear();
        waitingRespawn.clear();
        pendingReassign.clear();
        cancelAllCountdowns();
        clearAllActionbars();
        killLockUntil.clear();

        for (Participant p : getParticipants()) {
          Player pl = p.getPlayer();
          kills.put(pl.getUniqueId(), 0);
          PlayerState s = st(pl);
          s.deathStreak = 0;
          s.lowBiasNextAssign = false;
          s.killStreak = 0;
          p.attributes().TARGETABLE.setValue(true);
        }

        if (Settings.getSpawnEnable()) {
          gameSpawnPoint = Settings.getSpawnLocation().toBukkitLocation();
          saveAndApplyGameSpawnPoint(gameSpawnPoint);
          for (Participant p : getParticipants()) {
            p.getPlayer().teleport(gameSpawnPoint);
          }
        } else {
          gameSpawnPoint = null;
          originalSpawnPoints.clear();
        }

        getInvincibility().attachObserver(new Invincibility.Observer() {
          @Override public void onStart() { }

          @Override public void onEnd() {
            if (!firstAbilityAssigned) {
              firstAbilityAssigned = true;
              for (Participant p : getParticipants()) {
                assignAbilityByRule(p, Trigger.START);
              }
              Bukkit.broadcastMessage("§a초반 무적 종료! §f첫 능력이 배정되었습니다.");
            }
            setRestricted(false);
          }
        });

        if (invincibleStart) {
          getInvincibility().start(false);
        } else {
          Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
          firstAbilityAssigned = true;
          for (Participant p : getParticipants()) {
            assignAbilityByRule(p, Trigger.START);
          }
          setRestricted(false);
        }

        startGame();
        break;
      }
    }
  }

  // ====== Spawnpoint save/restore ======
  private void saveAndApplyGameSpawnPoint(Location gameSpawn) {
    originalSpawnPoints.clear();
    for (Participant pt : getParticipants()) {
      Player p = pt.getPlayer();
      originalSpawnPoints.put(p.getUniqueId(), p.getBedSpawnLocation());
      try {
        p.setBedSpawnLocation(gameSpawn, true);
      } catch (Throwable t) {
        try { p.setBedSpawnLocation(gameSpawn); } catch (Throwable ignore) {}
      }
    }
  }

  private void restoreOriginalSpawnPoints() {
    for (Map.Entry<UUID, Location> e : originalSpawnPoints.entrySet()) {
      Player p = Bukkit.getPlayer(e.getKey());
      if (p == null) continue;

      Location original = e.getValue();
      try {
        p.setBedSpawnLocation(original, true);
      } catch (Throwable t) {
        try { p.setBedSpawnLocation(original); } catch (Throwable ignore) {}
      }
    }
    originalSpawnPoints.clear();
    gameSpawnPoint = null;
  }

  // ====== Death ======
  @EventHandler
  private void onPlayerDeath(PlayerDeathEvent e) {
    if (!isRunning() || ended) return;

    Player p = e.getEntity();
    if (!isParticipating(p)) return;

    e.setKeepInventory(true);
    e.getDrops().clear();
    e.setKeepLevel(true);
    e.setDroppedExp(0);
    e.setDeathMessage(null);

    waitingRespawn.add(p.getUniqueId());
    setTargetable(p, false);

    startRespawnCountdown(p, RESPAWN_DELAY.getValue());
    scheduleActualRevive(p);
  }

  // ====== Respawn ======
  @EventHandler(priority = EventPriority.HIGHEST)
  private void onRespawn(PlayerRespawnEvent e) {
    if (!isRunning() || ended) return;

    Player p = e.getPlayer();
    if (!isParticipating(p)) return;

    if (!Settings.getSpawnEnable()) {
      e.setRespawnLocation(findSafeLocation(p.getWorld()));
    }

    Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> {
      if (!isRunning() || ended) return;
      if (!p.isOnline() || !isParticipating(p)) return;

      if (waitingRespawn.contains(p.getUniqueId())) {
        p.setGameMode(GameMode.SPECTATOR);
      }
    }, 2L);
  }

  // ====== Damage control ======
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  private void onDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player)) return;
    Player p = (Player) e.getEntity();
    if (!isRunning() || ended || !isParticipating(p)) return;

    if (waitingRespawn.contains(p.getUniqueId())) {
      e.setCancelled(true);
      return;
    }

    if (invincible.contains(p.getUniqueId())) {
      e.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  private void cancelKill(EntityDamageByEntityEvent e) {
    if (!isRunning() || ended) return;
    if (!(e.getEntity() instanceof Player)) return;

    Player victim = (Player) e.getEntity();
    if (!isParticipating(victim)) return;

    // "실제 공격자"를 플레이어로 해석 (원거리/폭발/간접 피해 포함)
    Player attacker = resolveDamagingPlayer(e.getDamager());
    if (attacker == null) return;
    if (!isParticipating(attacker)) return;

    // 연속 킬 락이 걸려있는 동안 "치명타"만 막는다
    if (isKillLocked(attacker)) {
      if (victim.getHealth() - e.getFinalDamage() <= 0) {
        e.setCancelled(true);
      }
    }
  }

  // ====== "부활 대기 중" 공격 방지 ======
  private Player resolveDamagingPlayer(Entity damager) {
    if (damager instanceof Player) return (Player) damager;

    if (damager instanceof Projectile) {
      Object shooter = ((Projectile) damager).getShooter();
      if (shooter instanceof Player) return (Player) shooter;
    }

    if (damager instanceof TNTPrimed) {
      try {
        Entity src = ((TNTPrimed) damager).getSource();
        if (src instanceof Player) return (Player) src;
      } catch (Throwable ignore) {}
    }

    return null;
  }

  @EventHandler(ignoreCancelled = true)
  private void blockDamageFromWaitingPlayer(EntityDamageByEntityEvent e) {
    Player attacker = resolveDamagingPlayer(e.getDamager());
    if (attacker == null) return;

    if (!isRunning() || ended) return;
    if (!isParticipating(attacker)) return;

    if (waitingRespawn.contains(attacker.getUniqueId())) {
      e.setCancelled(true);
    }
  }

  // ====== ParticipantDeathEvent ======
  @EventHandler
  private void onParticipantDeath(ParticipantDeathEvent e) {
    if (!isRunning() || ended) return;

    final Participant victim = e.getParticipant();
    final Player v = victim.getPlayer();
    if (!isParticipating(v)) return;

    PlayerState vs = st(v);
    vs.killStreak = 0;

    final Player killer = v.getKiller();

    if (killer == null || !isParticipating(killer)) {
      pendingReassign.remove(v.getUniqueId());
      Bukkit.broadcastMessage("§f" + v.getName() + " §7님이 사망했습니다.");
      return;
    }

    vs.deathStreak++;
    vs.lowBiasNextAssign = false;

    int killerKills = onKill(killer);
    Bukkit.broadcastMessage("§f" + v.getName() + " §7님이 §f" + killer.getName()
            + "§7님에게 살해당했습니다. §8(§c" + killerKills + "§7/§c" + getKillGoal() + "§8)");

    Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> {
      if (!isRunning() || ended) return;
      if (!v.isOnline() || !isParticipating(v)) return;

      try {
        if (victim.hasAbility()) victim.removeAbility();
      } catch (Throwable ignore) {}
    }, 2L);

    final int delaySec = RESPAWN_DELAY.getValue();
    pendingReassign.add(v.getUniqueId());

    Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> {
      if (!isRunning() || ended) return;
      if (!v.isOnline() || !isParticipating(v)) return;

      if (pendingReassign.remove(v.getUniqueId())) {
        assignAbilityByRule(victim, Trigger.DEATH);
      }
    }, delaySec * 20L);
  }

  // ====== Actual revive ======
  private void scheduleActualRevive(Player p) {
    final int delay = RESPAWN_DELAY.getValue();
    if (delay <= 0) {
      finalizeRevive(p);
      return;
    }

    Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> {
      if (!isRunning() || ended) return;
      if (!p.isOnline() || !isParticipating(p)) return;
      finalizeRevive(p);
    }, delay * 20L);
  }

  private void finalizeRevive(Player p) {
    UUID id = p.getUniqueId();

    waitingRespawn.remove(id);

    cancelCountdown(id);
    clearRespawnActionbar(p);

    p.setGameMode(GameMode.SURVIVAL);
    setTargetable(p, true);

    // ✅ 월드보더 안쪽 안전 위치로 강제 이동
    Location safe = findSafeLocation(p.getWorld());
    Bukkit.getScheduler().runTask(AbilityWar.getPlugin(), () -> {
      if (!isRunning() || ended) return;
      if (!p.isOnline() || !isParticipating(p)) return;
      p.teleport(safe, PlayerTeleportEvent.TeleportCause.PLUGIN);
    });

    // ✅ 부활 무적
    grantInvincibility(p, INVINCIBLE_SEC.getValue());
  }


  private void grantInvincibility(Player p, int seconds) {
    if (seconds <= 0) return;
    invincible.add(p.getUniqueId());
    Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(),
            () -> invincible.remove(p.getUniqueId()),
            seconds * 20L);
  }

  // ====== Respawn countdown ======
  private void startRespawnCountdown(Player player, int seconds) {
    cancelCountdown(player.getUniqueId());
    clearRespawnActionbar(player);

    if (seconds <= 0) return;

    BukkitRunnable task = new BukkitRunnable() {
      int left = seconds;

      @Override
      public void run() {
        if (!isRunning() || ended || !player.isOnline() || !isParticipating(player)) {
          cancelCountdown(player.getUniqueId());
          clearRespawnActionbar(player);
          return;
        }

        if (!waitingRespawn.contains(player.getUniqueId())) {
          cancelCountdown(player.getUniqueId());
          clearRespawnActionbar(player);
          return;
        }

        if (left <= 0) {
          cancelCountdown(player.getUniqueId());
          clearRespawnActionbar(player);
          return;
        }

        updateRespawnActionbar(player, "§e부활까지 §f" + left + "§e초");
        left--;
      }
    };

    countdownTasks.put(player.getUniqueId(), task);
    task.runTaskTimer(AbilityWar.getPlugin(), 0L, 20L);
  }

  private void cancelCountdown(UUID uuid) {
    BukkitRunnable r = countdownTasks.remove(uuid);
    if (r != null) {
      try { r.cancel(); } catch (Throwable ignore) {}
    }

    Player p = Bukkit.getPlayer(uuid);
    if (p != null) clearRespawnActionbar(p);
  }

  private void cancelAllCountdowns() {
    for (UUID uuid : new ArrayList<>(countdownTasks.keySet())) {
      cancelCountdown(uuid);
    }
  }

  private void clearAllActionbars() {
    for (UUID uuid : new ArrayList<>(respawnChannels.keySet())) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) clearRespawnActionbar(p);
      else respawnChannels.remove(uuid);
    }
  }

  // ====== Kill / Win ======
  private int onKill(Player killer) {
    UUID kid = killer.getUniqueId();
    int next = kills.getOrDefault(kid, 0) + 1;
    kills.put(kid, next);

    Score score = killObjective.getScore(killer.getName());
    score.setScore(next);

    if (next >= getKillGoal()) {
      ended = true;
      cancelAllCountdowns();
      waitingRespawn.clear();
      pendingReassign.clear();
      Win(getParticipant(killer));
      return next;
    }

    // ✅ 3초간 추가 킬(치명타) 방지
    lockKill(killer, KILL_LOCK_TIME.getValue());

    PlayerState ks = st(killer);
    ks.deathStreak = 0;
    ks.lowBiasNextAssign = true;
    ks.killStreak++;

    Participant pk = getParticipant(killer);
    if (pk == null) return next;

    boolean didReassignThisKill = false;

    if (isSynergyAbility(pk)) {
      didReassignThisKill = true;
      Bukkit.getScheduler().runTask(AbilityWar.getPlugin(), () -> {
        if (!isRunning() || ended) return;
        if (!isParticipating(killer)) return;
        assignAbilityByRule(pk, Trigger.KILL);
      });
    }

    int streakStep = KILL_GOAL_MULTIPLIER.getValue();
    if (!didReassignThisKill && streakStep > 0 && ks.killStreak % streakStep == 0) {
      Bukkit.broadcastMessage("§6" + killer.getName() + " §e님의 연속킬(" + ks.killStreak + ")로 능력이 재조정됩니다!");
      Bukkit.getScheduler().runTask(AbilityWar.getPlugin(), () -> {
        if (!isRunning() || ended) return;
        if (!isParticipating(killer)) return;
        assignAbilityByRule(pk, Trigger.KILL);
      });
    }

    return next;
  }

  // ====== Ability Assignment ======
  private enum Trigger { START, DEATH, KILL }

  private void assignAbilityByRule(Participant participant, Trigger trigger) {
    Player player = participant.getPlayer();
    PlayerState s = st(player);

    try {
      if (participant.hasAbility()) participant.removeAbility();

      // 시너지 강제 배정
      if (trigger == Trigger.DEATH && s.deathStreak >= SYNERGY_STREAK.getValue()) {
        Class<? extends AbilityBase> sy = pickRandomSynergyClass();
        participant.setAbility(sy);

        AbilityBase ab = participant.getAbility();

        player.sendMessage("§d시너지 강제 배정! §f새 능력: " + coloredAbilityName(ab));
        return;
      }

      boolean allowSpecial = ALLOW_SPECIAL.getValue();

      boolean killTriggered = (trigger == Trigger.KILL);
      boolean deathTriggered = (trigger == Trigger.DEATH);

      if (!killTriggered && s.lowBiasNextAssign) {
        killTriggered = true;
        s.lowBiasNextAssign = false;
      }

      Class<? extends AbilityBase> clazz;
      if (USE_WEIGHT.getValue()) {
        clazz = pickWeightedAbilityClass(
                s.deathStreak, killTriggered, deathTriggered, allowSpecial
        );
      } else {
        clazz = pickAbilityClass(allowSpecial);
      }
      participant.setAbility(clazz);

      AbilityBase ab = participant.getAbility();

      player.sendMessage("§a새 능력: " + coloredAbilityName(ab));
    } catch (Exception ex) {
      player.sendMessage("§c능력 배정 중 오류가 발생했습니다.");
      ex.printStackTrace();
    }
  }

  private boolean isSynergyAbility(Participant p) {
    if (p == null || !p.hasAbility()) return false;
    AbilityRegistration reg = AbilityFactory.getRegistration(Objects.requireNonNull(p.getAbility()).getClass());
    return reg != null && SynergyFactory.isSynergy(reg);
  }

  // ====== Common filter ======
  private boolean isAllowedRegistration(AbilityRegistration reg, boolean allowSpecial) {
    if (reg == null) return false;

    if (Settings.isBlacklisted(reg.getManifest().name())) return false;
    if (!reg.isAvailable(getClass())) return false;
    if (!Settings.isUsingBetaAbility() && reg.hasFlag(AbilityRegistration.Flag.BETA)) return false;

    Rank r = reg.getManifest().rank();
    return allowSpecial || r != Rank.SPECIAL;
  }

  private Class<? extends AbilityBase> pickRandomSynergyClass() {
    boolean allowSpecial = ALLOW_SPECIAL.getValue();

    List<AbilityRegistration> pool = new ArrayList<>();
    for (AbilityRegistration reg : SynergyFactory.getSynergies()) {
      if (!isAllowedRegistration(reg, allowSpecial)) continue;
      pool.add(reg);
    }

    if (pool.isEmpty()) throw new IllegalStateException("사용 가능한 시너지가 없습니다. (블랙/베타/설정에 의해 비었을 수 있음)");
    return pool.get(random.nextInt(pool.size())).getAbilityClass();
  }

  private Class<? extends AbilityBase> pickAbilityClass(boolean allowSpecial) {
    List<AbilityRegistration> registrations = new ArrayList<>();
    for (AbilityRegistration reg : AbilityList.values()) {
      if (!isAllowedRegistration(reg, allowSpecial)) continue;
      registrations.add(reg);
    }

    return registrations.get(random.nextInt(registrations.size())).getAbilityClass();
  }

  private Class<? extends AbilityBase> pickWeightedAbilityClass(
          int deathStreak,
          boolean killTriggered,
          boolean deathTriggered,
          boolean allowSpecial
  ) {
    Map<Rank, Double> weights = adjustedWeights(deathStreak, killTriggered, deathTriggered, allowSpecial);

    Map<Rank, List<AbilityRegistration>> byRank = new EnumMap<>(Rank.class);
    for (AbilityRegistration reg : AbilityList.values()) {
      if (!isAllowedRegistration(reg, allowSpecial)) continue;
      Rank r = reg.getManifest().rank();
      byRank.computeIfAbsent(r, k -> new ArrayList<>()).add(reg);
    }

    double total = 0.0;
    for (Map.Entry<Rank, Double> e : weights.entrySet()) {
      Rank r = e.getKey();
      double w = Math.max(0.0, e.getValue());
      if (w <= 0) continue;
      if (!byRank.containsKey(r) || byRank.get(r).isEmpty()) continue;
      total += w;
    }
    if (total <= 0) throw new IllegalStateException("사용 가능한 능력이 없습니다.");

    double roll = random.nextDouble() * total;
    Rank chosen = Rank.C;
    for (Map.Entry<Rank, Double> e : weights.entrySet()) {
      Rank r = e.getKey();
      double w = Math.max(0.0, e.getValue());
      if (w <= 0) continue;
      if (!byRank.containsKey(r) || byRank.get(r).isEmpty()) continue;

      roll -= w;
      if (roll <= 0) { chosen = r; break; }
    }

    List<AbilityRegistration> pool = byRank.get(chosen);
    return pool.get(random.nextInt(pool.size())).getAbilityClass();
  }

  // ====== Skript respawn teleport block (destination only) ======
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  private void onTeleportDuringRespawn(PlayerTeleportEvent e) {
    if (!isRunning() || ended) return;

    Player p = e.getPlayer();
    if (!isParticipating(p)) return;

    if (!waitingRespawn.contains(p.getUniqueId())) return;

    if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      Location to = e.getTo();
      if (to == null || to.getWorld() == null) return;

      if (!"world".equalsIgnoreCase(to.getWorld().getName())) return;
      if (Math.abs(to.getX()) > 0.75) return;
      if (Math.abs(to.getY() - 14) > 0.75) return;
      if (Math.abs(to.getZ()) > 0.75) return;

      e.setCancelled(true);
    }
  }

  // ====== Weights ======
  private EnumSet<Rank> lowRanks(boolean allowSpecial) {
    return allowSpecial ? EnumSet.of(Rank.C, Rank.B, Rank.A) : EnumSet.of(Rank.C, Rank.B);
  }

  private EnumSet<Rank> highRanks(boolean allowSpecial) {
    return allowSpecial ? EnumSet.of(Rank.S, Rank.L, Rank.SPECIAL) : EnumSet.of(Rank.A, Rank.S, Rank.L);
  }

  private Map<Rank, Double> baseWeights(boolean allowSpecial) {
    Map<Rank, Double> w = new EnumMap<>(Rank.class);
    w.put(Rank.C, 1.0);
    w.put(Rank.B, 1.0);
    w.put(Rank.A, 1.0);
    w.put(Rank.S, 1.0);
    w.put(Rank.L, 1.0);
    w.put(Rank.SPECIAL, allowSpecial ? 1.0 : 0.0);
    return w;
  }

  private Map<Rank, Double> adjustedWeights(int deathStreak, boolean killTriggered, boolean deathTriggered, boolean allowSpecial) {
    Map<Rank, Double> w = baseWeights(allowSpecial);

    if (killTriggered) {
      for (Rank r : lowRanks(allowSpecial)) mul(w, r, 1.40);
      for (Rank r : highRanks(allowSpecial)) mul(w, r, 0.65);
      return w;
    }

    if (deathTriggered) {
      for (Rank r : lowRanks(allowSpecial)) mul(w, r, 0.75);
      for (Rank r : highRanks(allowSpecial)) mul(w, r, 1.35);

      if (deathStreak >= 3) {
        for (Rank r : highRanks(allowSpecial)) mul(w, r, 1.25);
        for (Rank r : lowRanks(allowSpecial)) mul(w, r, 0.92);
      }
      return w;
    }

    return w;
  }

  private void mul(Map<Rank, Double> w, Rank r, double m) {
    w.put(r, Math.max(0.0, w.getOrDefault(r, 0.0) * m));
  }

  // ====== Safe Spawn (spawnEnable=false일 때만) ======
  private boolean isAir(Block b) {
    return b.getType() == Material.AIR;
  }

  private Location findSafeLocation(World world) {
    final WorldBorder wb = world.getWorldBorder();
    final Location center = wb.getCenter();

    double half = wb.getSize() / 2.0;
    double radius = half - (BORDER_MARGIN.getValue() + BORDER_EXTRA_MARGIN);
    radius = Math.max(6, radius);

    for (int i = 0; i < 60; i++) {
      double x = center.getX() + (random.nextDouble() * 2 - 1) * radius;
      double z = center.getZ() + (random.nextDouble() * 2 - 1) * radius;

      int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
      Location loc = new Location(world, x + 0.5, y, z + 0.5);

      if (!world.getBlockAt(loc.clone().add(0, -1, 0)).getType().isSolid()) continue;
      if (!isAir(world.getBlockAt(loc))) continue;
      if (!isAir(world.getBlockAt(loc.clone().add(0, 1, 0)))) continue;

      try {
        if (world.getBlockAt(loc.clone().add(0, -1, 0)).isLiquid()) continue;
      } catch (Throwable ignore) {}

      return clampInsideBorder(world, loc, BORDER_EXTRA_MARGIN);
    }

    return clampInsideBorder(world, world.getSpawnLocation(), BORDER_EXTRA_MARGIN);
  }

  private Location clampInsideBorder(World world, Location loc, int extraMargin) {
    WorldBorder wb = world.getWorldBorder();
    Location c = wb.getCenter();
    double half = wb.getSize() / 2.0;

    double m = BORDER_MARGIN.getValue() + Math.max(0, extraMargin);
    double minX = c.getX() - half + m;
    double maxX = c.getX() + half - m;
    double minZ = c.getZ() - half + m;
    double maxZ = c.getZ() + half - m;

    double x = Math.min(maxX, Math.max(minX, loc.getX()));
    double z = Math.min(maxZ, Math.max(minZ, loc.getZ()));

    int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
    return new Location(world, x + 0.5, y, z + 0.5);
  }

  // ====== Stop ======
  @Override
  public boolean stop() {
    ended = true;

    cancelAllCountdowns();
    waitingRespawn.clear();
    pendingReassign.clear();
    invincible.clear();
    killLockUntil.clear();

    restoreOriginalSpawnPoints();
    clearAllActionbars();

    boolean result = super.stop();
    HandlerList.unregisterAll(this);

    states.clear();
    kills.clear();
    return result;
  }
}
