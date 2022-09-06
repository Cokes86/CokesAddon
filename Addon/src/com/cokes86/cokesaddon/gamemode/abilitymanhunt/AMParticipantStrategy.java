package com.cokes86.cokesaddon.gamemode.abilitymanhunt;

import daybreak.abilitywar.game.ParticipantStrategy;
import org.bukkit.entity.Player;

import java.util.*;

public class AMParticipantStrategy implements ParticipantStrategy {
    private final Map<String, AMParticipant> HuntingRunnerParticipants = new HashMap<>();
    private AMParticipant targetHuntingRunnerParticipant;

    public AMParticipant getTargetHuntingRunnerParticipant() {
        return targetHuntingRunnerParticipant;
    }

    public void setTargetHuntingRunnerParticipant(AMParticipant target) {
        this.targetHuntingRunnerParticipant = target;
    }

    public AMParticipantStrategy(AbilityManhunt huntingrunner, Collection<Player> players) {
        for (Player player : players) {
            HuntingRunnerParticipants.put(player.getUniqueId().toString(), new AMParticipant(huntingrunner, player));
        }
    }

    @Override
    public Collection<? extends AMParticipant> getParticipants() {
        return Collections.unmodifiableCollection(HuntingRunnerParticipants.values());
    }

    @Override
    public boolean isParticipating(UUID uuid) {
        return HuntingRunnerParticipants.containsKey(uuid.toString());
    }

    @Override
    public AMParticipant getParticipant(UUID uuid) {
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