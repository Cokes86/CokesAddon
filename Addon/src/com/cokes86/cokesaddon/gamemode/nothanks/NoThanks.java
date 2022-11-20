package com.cokes86.cokesaddon.gamemode.nothanks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.ParticipantStrategy;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "노 땡스", description = {
    "3인에서 7인 사이에서 진행되는 간단한 미니게임입니다.",
    "벌점카드를 최대한 적게 받아 승리하세요."
})
@GameAliases({
    "노땡", "땡스"
})
@Category(Category.GameCategory.MINIGAME)
@Beta
public class NoThanks extends AbstractGame implements Winnable {
    private boolean tutorial = true;
    private int main_chip = 0;
    private int bad_score = 0;
    private HashSet<Integer> gameDeck;

    public NoThanks(final String[] args) throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        if (args.length != 0 && "speed".equals(args[0])) this.tutorial = false;
    }

    @Override
    public Collection<NTParticipant> getParticipants() {
        return ((NTParticipantStrategy) participantStrategy).getParticipants();
    }

    @Override
    public NTParticipant getParticipant(Player player) {
        return ((NTParticipantStrategy) participantStrategy).getParticipant(player.getUniqueId());
    }

    @Override
    public NTParticipant getParticipant(UUID uuid) {
        return ((NTParticipantStrategy) participantStrategy).getParticipant(uuid);
    }

    @Override
    public ParticipantStrategy newParticipantStrategy(Collection<Player> collection) {
        return new NTParticipantStrategy(collection);
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
                if (getParticipants().size() > 7) {
                    stop();
                    Bukkit.broadcastMessage("§c최대 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§77명§8)");
                    return;
                }
                Bukkit.broadcastMessage("§5No Thanks! §f- §d노 땡쓰!");
                Bukkit.broadcastMessage("§b개발자 §7: §fCokes_86 코크스");
                Bukkit.broadcastMessage("§9디스코드 §7: §f코크스§7#9329");
                break;
            }
            case 2: {
                if (!tutorial) {
                    setCount(11);
                }
                break;
            }
            case 3: {
                Bukkit.broadcastMessage("§6간단§f하게 게임 설명해드리겠습니다!");
                Bukkit.broadcastMessage("§6(§f/aw start speed §7명령어로 게임을 시작해 스킵할 수 있습니다.§6)");
                break;
            }
            case 4: {
                Bukkit.broadcastMessage("§a3§f과 §c35§f사이의 벌점숫자를 보여주면");
                for (NTParticipant participant : getParticipants()) {
                    NMS.sendTitle(participant.getPlayer(), "§a7", "차례: Cokes_86 | 칩: 0", 0, 20, 0);
                }
                break;
            }
            case 5: {
                Bukkit.broadcastMessage("§b칩§f 하나를 제출하고 다른 플레이어에게 넘기거나");
                for (NTParticipant participant : getParticipants()) {
                    NMS.sendTitle(participant.getPlayer(), "§a7", "차례: RainStar_ | 칩: 1", 0, 20, 0);
                    SoundLib.ENTITY_VILLAGER_NO.playSound(participant.getPlayer());
                }
                break;
            }
            case 6: {
                Bukkit.broadcastMessage("제출한 §b칩§f과 함께 벌점숫자를 가져갈 수 있습니다.");
                for (NTParticipant participant : getParticipants()) {
                    NMS.sendTitle(participant.getPlayer(), "§b21", "차례: _DayBreak_ | 칩: 0", 0, 20, 0);
                    SoundLib.ENTITY_VILLAGER_YES.playSound(participant.getPlayer());
                }
                break;
            }
            case 7: {
                Bukkit.broadcastMessage("만약 벌점숫자 중 연속되는 숫자가 있다면");
                for (NTParticipant participant : getParticipants()) {
                    NMS.sendTitle(participant.getPlayer(), "예시", "7 8 9 14 17", 0, 20, 0);
                }
                break;
            }
            case 8: {
                Bukkit.broadcastMessage("가장 작은 숫자만 남고, 나머지는 삭제됩니다.");
                for (NTParticipant participant : getParticipants()) {
                    NMS.sendTitle(participant.getPlayer(), "예시", "7 §m8 9§r 14 17", 0, 20, 0);
                }
                break;
            }
            case 9: {
                Bukkit.broadcastMessage("벌점숫자를 더하고 가진 칩 개수를 뺀 점수가 최종 점수가 되며");
                break;
            }
            case 10: {
                Bukkit.broadcastMessage("최종적으로 점수가 가장 낮은 사람이 이기게 되는 미니게임입니다.");
                break;
            }
            case 11: {
                Bukkit.broadcastMessage("게임의 재미를 위해, 1~5개의 숫자는 게임에서 제외됩니다! 이는, 모든 플레이어가 알 수 없어요");
                break;
            }
            case 12: {
                gameDeck = new HashSet<>();
                for (int i = 3; i <= 35; i++) {
                    gameDeck.add(i);
                }
                
                break;
            }
        }
    }

    private class NTParticipant extends Participant {

        private final Attributes attributes = new Attributes();

        protected NTParticipant(Player player) {
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

    class NTParticipantStrategy implements ParticipantStrategy {

        private final Map<String, NTParticipant> participants = new HashMap<>();

        public NTParticipantStrategy(Collection<Player> players) {
            for (Player player : players) {
                participants.put(player.getUniqueId().toString(), new NTParticipant(player));
            }
        }

        @Override
        public Collection<NTParticipant> getParticipants() {
            return Collections.unmodifiableCollection(participants.values());
        }

        @Override
        public boolean isParticipating(UUID uuid) {
            return participants.containsKey(uuid.toString());
        }

        @Override
        public NTParticipant getParticipant(UUID uuid) {
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
}
