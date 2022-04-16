package cokes86.addon.gamemode.huntingrunner;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.Category.GameCategory;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.ParticipantStrategy;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import org.bukkit.entity.Player;

import javax.naming.OperationNotSupportedException;
import java.util.Collection;

@GameManifest(name = "러너 잡기", description = {
        "한 명의 스피드러너와 헌터들!",
        "엔더드래곤을 잡기 전까지 얼른 러너를 방해하세요!",
        "● 러너 : 헌터들의 방해를 피해 마인크래프트의 엔딩을 봐주세요!",
        "● 헌터 : 야생 러너를 잡기만 하면 이깁니다. 모두 분발합시다!"
})
@Category(GameCategory.MINIGAME)
public class HuntingRunner extends AbstractGame implements AbilitySelect.Handler {

    public HuntingRunner() throws IllegalArgumentException {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
    }

    @Override
    protected void run(int count) {
        switch(count) {
            case 1 : {

            }
            case 3 : {

            }
        }
    }

    @Override
    protected ParticipantStrategy newParticipantStrategy(Collection<Player> arg0) {
        return new HRStrategy(this, arg0);
    }

    @Override
    public AbilitySelect getAbilitySelect() {
        return null;
    }

    @Override
    public AbilitySelect newAbilitySelect() {
        return null;
    }

    @Override
    public void startAbilitySelect() {

    }
}
