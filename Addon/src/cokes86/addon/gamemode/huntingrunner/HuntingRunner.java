package cokes86.addon.gamemode.huntingrunner;

import java.util.Collection;

import org.bukkit.entity.Player;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.ParticipantStrategy;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;

public class HuntingRunner extends AbstractGame {

    public HuntingRunner() throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
    }

    @Override
    protected ParticipantStrategy newParticipantStrategy(Collection<Player> arg0) {
        return new HRStrategy(this, arg0);
    }
    
    
}
