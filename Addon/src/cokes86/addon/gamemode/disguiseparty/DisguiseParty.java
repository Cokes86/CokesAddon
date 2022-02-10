package cokes86.addon.gamemode.disguiseparty;

import cokes86.addon.util.disguise.DisguiseUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.*;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.minecraft.BroadBar;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

@GameManifest(name = "변장 파티", description = {
        "§f누가 진짜인지 모르겠다고요? 얼른 찾아보세요!"
})
@GameAliases({
        "변장"
})
@Category(Category.GameCategory.MINIGAME)
@Beta
public class DisguiseParty extends AbstractGame implements Winnable {
    private boolean tutorial = true;
    private DisguiseParticipant tag, target;
    private final List<UUID> eliminate = new ArrayList<>();
    private final Scoreboard score;
    private final Team disguiseTeam;
    private int attack_chance=0;
    private int max_attack_chance=0;
    private BroadBar bar = null;

    public DisguiseParty(final String[] args) throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        if (args.length != 0 && "speed".equals(args[0])) this.tutorial = false;
        if (Bukkit.getScoreboardManager() != null) {
            score = Bukkit.getScoreboardManager().getMainScoreboard();
            disguiseTeam = score.registerNewTeam("Disguise_Hide");
            disguiseTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        } else {
            score = null;
            disguiseTeam = null;
        }

    }

    public boolean eliminate(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage("§c" + player.getName() + "§f님이 탈락하셨습니다.");
        return eliminate.add(player.getUniqueId());
    }

    public boolean isEliminate(Player player) {
        return eliminate.contains(player.getUniqueId());
    }

    public void hideNameTag(Player player) {
        if (score != null) {
            disguiseTeam.addEntry(player.getName());
        }
    }

    public void showNameTag(Player player) {
        if (score != null) {
            disguiseTeam.removeEntry(player.getName());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isParticipating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onFoodLevelChange(final FoodLevelChangeEvent e) {
        if (isParticipating(e.getEntity().getUniqueId())) {
            e.setFoodLevel(19);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isParticipating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private void setMaxAttackChance(int chance) {
        max_attack_chance = chance;
        setAttackChance(chance);
    }

    private void setAttackChance(int chance) {
        attack_chance = chance;
        if (bar != null) {
            bar.setProgress((double)chance / max_attack_chance);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (tag != null && e.getDamager().equals(tag.getPlayer()) && e.getEntity() instanceof Player && isParticipating(e.getEntity().getUniqueId())) {
            Player entity = (Player) e.getEntity();
            if (!isEliminate(entity)) {
                e.setDamage(0);
                if (entity.equals(target.getPlayer())) {
                    tag.getPlayer().sendMessage("§a목표§f를 찾았습니다! 게임을 종료합니다!");
                    eliminate(entity);
                    Win(tag);
                    stop();
                } else {
                    tag.getPlayer().sendMessage("이런 §a목표§f가 아니에요!");
                    DisguiseUtil.changeSkin(entity, entity.getUniqueId());
                    DisguiseUtil.setPlayerNameTag(entity, entity.getUniqueId());
                    DisguiseUtil.reloadPlayer(entity);
                    setAttackChance(attack_chance--);

                    if (eliminate(entity) && attack_chance < 1) {
                        Bukkit.broadcastMessage("§a목표§f를 끝까지 찾지 못했습니다! 게임을 종료합니다!");
                        List<Participant> winner = new ArrayList<>(getParticipants());
                        winner.remove(tag);
                        winner.removeIf(participant -> isEliminate(participant.getPlayer()));
                        Win(winner.toArray(new Participant[]{}));
                        if (bar != null) {
                            bar.unregister();
                            bar = null;
                        }
                    }
                }
            }
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    protected void run(int count) {
        switch (count) {
            case 1: {
                Bukkit.broadcastMessage(Formatter.formatTitle(36, ChatColor.GOLD, ChatColor.WHITE, "게임 참가자"));
                final Collection<? extends Participant> participants = getParticipants();
                {
                    int i = 0;
                    final StringJoiner joiner = new StringJoiner(", ");
                    for (Participant participant : participants) {
                        joiner.add(participant.getPlayer().getName());
                        if (++i > 10) {
                            joiner.add("...");
                        }
                    }
                    Bukkit.broadcastMessage(joiner.toString());
                }
                Bukkit.broadcastMessage("§6총 인원수§f: §d" + participants.size() + "명");
                Bukkit.broadcastMessage("§6=====================================");
                if (getParticipants().size() < 3) {
                    stop();
                    Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§73명§8)");
                    return;
                }
                Bukkit.broadcastMessage("§5DisguiseParty §f- §d변장 파티");
                Bukkit.broadcastMessage("§b개발자 §7: §fCokes_86 코크스");
                Bukkit.broadcastMessage("§9디스코드 §7: §f코크스§7#9329");
                break;
            }
            case 2: {
                if (!tutorial) {
                    setCount(8);
                }
                break;
            }
            case 3: {
                Bukkit.broadcastMessage("§6간단§f하게 게임 설명해드리겠습니다!");
                Bukkit.broadcastMessage("§6(§f/aw start speed §7명령어로 게임을 시작해 스킵할 수 있습니다.§6)");
                break;
            }
            case 4: {
                Bukkit.broadcastMessage("이 게임에는 §c술래§f 1명, §a목표§f 1명이 존재합니다.");
                break;
            }
            case 5: {
                Bukkit.broadcastMessage("§c술래§f와 §a목표§f가 정해지면 남은 사람들은 전부 §a목표§f가 된 플레이어로 변장합니다.");
                break;
            }
            case 6: {
                Bukkit.broadcastMessage("30초 동안 §c술래§f는 가려진 뒤 진짜 §a목표§f를 찾아 때리시면 됩니다.");
                break;
            }
            case 7: {
                Bukkit.broadcastMessage("§c술래§f는 §a목표§f를 잡으면 승리하고, §a목표§f는 남은 공격 기회가 없어질 때 까지 들키지 않으면 승리!");
                break;
            }
            case 8: {
                Bukkit.broadcastMessage("얼마나 빨리 찾는 지 볼까요?");
                break;
            }

            case 9: {
                Bukkit.broadcastMessage("§c술래§f 1명, §a목표§f 1명을 정합니다...");
                break;
            }

            case 10: {
                final Random random = new Random();
                final ArrayList<DisguiseParticipant> list = new ArrayList<>(getParticipants());
                this.tag = list.get(random.nextInt(list.size()));
                list.remove(tag);
                this.target = list.get(random.nextInt(list.size()));
                break;
            }

            case 11: {
                Bukkit.broadcastMessage("§c술래§f: "+tag.getPlayer().getName());
                SoundLib.ENTITY_ARROW_HIT_PLAYER.broadcastSound();
                break;
            }

            case 12: {
                Bukkit.broadcastMessage("§a목표§f: "+target.getPlayer().getName());
                SoundLib.ENTITY_ARROW_HIT_PLAYER.broadcastSound();
                DisguiseUtil.addData(target.getPlayer().getUniqueId());
                break;
            }

            case 13: {
                SoundLib.BLOCK_BEACON_ACTIVATE.broadcastSound();
                Bukkit.broadcastMessage("§c술래§f는 30초간 눈을 감습니다. 눈이 떠지면 플레이어를 때려서 진짜를 찾으세요!");
                new HideTimer().start();
                for (DisguiseParticipant participant : getParticipants()) {
                    hideNameTag(participant.getPlayer());
                    if (participant.equals(tag) || participant.equals(target)) continue;
                    participant.getPlayer().sendMessage("§a목표§f "+target.getPlayer().getName()+"으로 변장합니다. 최대한 속이세요!");
                    DisguiseUtil.changeSkin(participant.getPlayer(), target.getPlayer().getUniqueId());
                    DisguiseUtil.setPlayerNameTag(participant.getPlayer(), target.getPlayer().getUniqueId());
                    DisguiseUtil.reloadPlayer(participant.getPlayer());
                }
                target.getPlayer().sendMessage("최대한 숨어서 오래 살아남으세요!");
                attack_chance = ((getParticipants().size()-1) / 5) + 1;
                break;
            }
        }
    }

    @Override
    public Collection<DisguiseParticipant> getParticipants() {
        return ((DisguiseParticipantStrategy) participantStrategy).getParticipants();
    }

    @Override
    public DisguiseParticipant getParticipant(Player player) {
        return ((DisguiseParticipantStrategy) participantStrategy).getParticipant(player.getUniqueId());
    }

    @Override
    public DisguiseParticipant getParticipant(UUID uuid) {
        return ((DisguiseParticipantStrategy) participantStrategy).getParticipant(uuid);
    }

    @Override
    public ParticipantStrategy newParticipantStrategy(Collection<Player> collection) {
        return new DisguiseParticipantStrategy(collection);
    }

    class HideTimer extends GameTimer implements Listener {

        public HideTimer() {
            super(TaskType.NORMAL, 30);
        }

        @Override
        protected void onStart() {
            PotionEffects.BLINDNESS.addPotionEffect(tag.getPlayer(), 40, 0, true);
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void run(int count) {
            PotionEffects.BLINDNESS.addPotionEffect(tag.getPlayer(), 40, 0, true);
        }

        @Override
        protected void onEnd() {
            super.onEnd();
            bar = new BroadBar("남은 공격 기회", BarColor.BLUE, BarStyle.SEGMENTED_10);
            setMaxAttackChance((getParticipants().size()-1)/5 +1);
            Bukkit.broadcastMessage("게임이 시작되었습니다!");
            for (DisguiseParticipant participant : getParticipants()) {
                if (participant.equals(tag)) {
                    participant.getPlayer().sendMessage("[§6우승조건§f] §a목표§f "+target.getPlayer().getName()+"을(를) "+max_attack_chance+"번 안에 찾아라!");
                } else if (participant.equals(target)) {
                    participant.getPlayer().sendMessage("[§6우승조건§f] §c술래§f "+tag.getPlayer().getName()+"을(를) 피해 "+(((getParticipants().size()-1)/5) +1)+"명이 남을 때 까지 생존하라!");
                } else {
                    participant.getPlayer().sendMessage("[§6우승조건§f] §a목표§f "+target.getPlayer().getName()+"을(를) 도와 "+(((getParticipants().size()-1)/5) +1)+"명이 남을 때 까지 생존하라!");
                }
            }
            SoundLib.ENTITY_ENDER_DRAGON_GROWL.broadcastSound();
            HandlerList.unregisterAll(this);

            for (DisguiseParticipant participant : getParticipants()) {
                showNameTag(participant.getPlayer());
            }
        }

        @Override
        protected void onSilentEnd() {
            super.onSilentEnd();
            HandlerList.unregisterAll(this);
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            if (event.getPlayer().equals(tag.getPlayer())) {
                event.setTo(event.getFrom());
            }
        }
    }

    protected class DisguiseParticipant extends Participant {

        private final Attributes attributes = new Attributes();

        protected DisguiseParticipant(Player player) {
            super(player);
        }

        @Override
        public void setAbility(AbilityFactory.AbilityRegistration registration) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAbility() {
            return false;
        }

        @Override
        public AbilityBase getAbility() {
            return null;
        }

        @Override
        public AbilityBase removeAbility() {
            return null;
        }

        @Override
        public Attributes attributes() {
            return attributes;
        }
    }

    class DisguiseParticipantStrategy implements ParticipantStrategy {

        private final Map<String, DisguiseParticipant> participants = new HashMap<>();

        public DisguiseParticipantStrategy(Collection<Player> players) {
            for (Player player : players) {
                participants.put(player.getUniqueId().toString(), new DisguiseParticipant(player));
            }
        }

        @Override
        public Collection<DisguiseParticipant> getParticipants() {
            return Collections.unmodifiableCollection(participants.values());
        }

        @Override
        public boolean isParticipating(UUID uuid) {
            return participants.containsKey(uuid.toString());
        }

        @Override
        public DisguiseParticipant getParticipant(UUID uuid) {
            return participants.get(uuid.toString());
        }

        @Override
        public void addParticipant(Player player) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("참가자를 추가할 수 없습니다.");
        }

        @Override
        public void removeParticipant(UUID uuid) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("참가자를 제거할 수 없습니다.");
        }

    }

    @Override
    public void executeCommand(CommandType commandType, CommandSender sender, String command, String[] args, Plugin plugin) {
        sender.sendMessage(ChatColor.RED + "이 게임모드에서 사용할 수 없는 명령어입니다.");
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        for (DisguiseParticipant participant : getParticipants()) {
            if (participant.equals(tag) || participant.equals(target)) continue;
            DisguiseUtil.changeSkin(participant.getPlayer(), participant.getPlayer().getUniqueId());
            DisguiseUtil.setPlayerNameTag(participant.getPlayer(), participant.getPlayer().getUniqueId());
            DisguiseUtil.reloadPlayer(participant.getPlayer());
        }
        DisguiseUtil.clearData();
        if (disguiseTeam != null) disguiseTeam.unregister();
        super.onEnd();
    }
}
