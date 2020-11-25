package cokes86.addon.ability.list;

import java.util.StringJoiner;

import org.bukkit.Material;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;

@AbilityManifest(name = "아이올라이트&제이드", rank= AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS, explain = {
        "보랏빛의 보석과 초록빛의 보석이 만났습니다. 두 보석은 체력을 각각 반씩 나누어가집니다.",
        "능력 활성화 시 두 보석 중 하나를 랜덤으로 배정받습니다.",
        "보석의 체력이 모두 0이 되었을 시 사망합니다.",
        "철괴 좌클릭 시 다른 보석으로 변경합니다. 이는 공유된 쿨타임을 가집니다. $[TAG]",
        "============",
        "$(ABILITY)"
})
@Beta
public class IoliteAndJade extends CokesAbility implements ActiveHandler {
	private final Cooldown tag = new Cooldown(3, "태그");
	private final Gem gem = null;
	private final Object ABILITY = new Object() {
		@Override
		public String toString() {
			if (gem != null) {
				StringJoiner joiner = new StringJoiner("\n");
				joiner.add(gem.name());
				for (String explain : gem.explain()) {
					joiner.add(explain);
				}
				return joiner.toString();
			} else {
				return "보석이 아직 배정되지 않았습니다.";
			}
		}
	};
    public IoliteAndJade(AbstractGame.Participant arg0) {
        super(arg0);
    }
    
    @Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
    	if (arg0 == Material.IRON_INGOT && arg1 == ClickType.LEFT_CLICK && !tag.isCooldown()) {
    		tag.start();
    	} else {
    		gem.Active(arg0, arg1);
    	}
		return false;
	}
    
    public Object getABILITY() {
		return ABILITY;
	}

	interface Gem {
    	boolean Active(Material material, ClickType clickType);
    	String[] explain();
    	String name();
    }

    class Iolite implements Gem {

		@Override
		public boolean Active(Material material, ClickType clickType) {
			return false;
		}

		@Override
		public String[] explain() {
			return new String[] {
					"패시브 - 보랏빛 자태: 자연회복속도가 2배가 됩니다.",
					"철괴 우클릭 - "
			};
		}

		@Override
		public String name() {
			return "아이올라이트";
		}
        
    }
    
    class Jade implements Gem {

		@Override
		public boolean Active(Material material, ClickType clickType) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String[] explain() {
			return new String[] {
					""
			};
		}

		@Override
		public String name() {
			return "제이드";
		}
    	
    }
}
