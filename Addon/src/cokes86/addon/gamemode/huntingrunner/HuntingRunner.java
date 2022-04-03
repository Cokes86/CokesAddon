package cokes86.addon.gamemode.huntingrunner;

import java.util.Collection;

import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.Category.GameCategory;
import daybreak.abilitywar.game.GameManifest;
import org.bukkit.entity.Player;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.ParticipantStrategy;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;

@GameManifest(name = "러너 잡기", description = {
        "한 명의 스피드러너와 헌터들!",
        "엔더드래곤을 잡기 전까지 얼른 러너를 방해하세요!"
})
@Category(GameCategory.MINIGAME)
public class HuntingRunner extends AbstractGame {

    public HuntingRunner() throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
    }

    @Override
    protected void run(int count) {

    }

    @Override
    protected ParticipantStrategy newParticipantStrategy(Collection<Player> arg0) {
        return new HRStrategy(this, arg0);
    }
    
    
}
