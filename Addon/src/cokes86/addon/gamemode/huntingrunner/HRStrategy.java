package cokes86.addon.gamemode.huntingrunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import daybreak.abilitywar.game.ParticipantStrategy;

public class HRStrategy implements ParticipantStrategy {
    private final Map<String, HRParticipant> HuntingRunnerParticipants = new HashMap<>();
    private HRParticipant targetHuntingRunnerParticipant;

    public HRParticipant getTargetHuntingRunnerParticipant() {
        return targetHuntingRunnerParticipant;
    }

    public void setTargetHuntingRunnerParticipant(HRParticipant target) {
        this.targetHuntingRunnerParticipant = target;
    }

    public HRStrategy(HuntingRunner huntingrunner, Collection<Player> players) {
        for (Player player : players) {
            HuntingRunnerParticipants.put(player.getUniqueId().toString(), new HRParticipant(huntingrunner, player));
        }
    }

    @Override
    public Collection<? extends HRParticipant> getParticipants() {
        return Collections.unmodifiableCollection(HuntingRunnerParticipants.values());
    }

    @Override
    public boolean isParticipating(UUID uuid) {
        return HuntingRunnerParticipants.containsKey(uuid.toString());
    }

    @Override
    public HRParticipant getParticipant(UUID uuid) {
        return HuntingRunnerParticipants.get(uuid.toString());
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