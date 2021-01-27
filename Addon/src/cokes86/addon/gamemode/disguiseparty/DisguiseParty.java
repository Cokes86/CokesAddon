package cokes86.addon.gamemode.disguiseparty;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.ParticipantStrategy;
import daybreak.abilitywar.game.event.participant.ParticipantAbilitySetEvent;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class DisguiseParty extends AbstractGame {
    public DisguiseParty() throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
    }

    @Override
    protected void run(int count) {
        super.run(count);
    }

    @Override
    protected ParticipantStrategy newParticipantStrategy(Collection<Player> collection) {
        return new DefaultManagement(this, collection);
    }

    protected class ParticipantImpl extends Participant {

        private AbilityBase ability = null;
        private final Attributes attributes = new Attributes();

        protected ParticipantImpl(Player player) {
            super(player);
        }

        @Override
        public void setAbility(AbilityFactory.AbilityRegistration registration) throws ReflectiveOperationException {
            final AbilityBase oldAbility = removeAbility();
            final AbilityBase ability = AbilityBase.create(registration, this);
            ability.setRestricted(false);
            this.ability = ability;
            Bukkit.getPluginManager().callEvent(new ParticipantAbilitySetEvent(this, oldAbility, ability));
        }

        @Override
        public boolean hasAbility() {
            return ability != null;
        }

        @Override
        public AbilityBase getAbility() {
            return ability;
        }

        @Override
        public AbilityBase removeAbility() {
            final AbilityBase ability = this.ability;
            if (ability != null) {
                ability.destroy();
                this.ability = null;
            }
            return ability;
        }

        @Override
        public Attributes attributes() {
            return attributes;
        }
    }

    class DefaultManagement implements ParticipantStrategy {

        private final Map<String, Participant> participants = new HashMap<>();

        public DefaultManagement(DisguiseParty game, Collection<Player> players) {
            for (Player player : players) {
                participants.put(player.getUniqueId().toString(), new ParticipantImpl(player));
            }
        }

        @Override
        public Collection<? extends Participant> getParticipants() {
            return Collections.unmodifiableCollection(participants.values());
        }

        @Override
        public boolean isParticipating(UUID uuid) {
            return participants.containsKey(uuid.toString());
        }

        @Override
        public Participant getParticipant(UUID uuid) {
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
}
