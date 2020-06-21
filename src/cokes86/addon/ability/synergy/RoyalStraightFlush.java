package cokes86.addon.ability.synergy;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import cokes86.addon.configuration.synergy.Config;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mixability.synergy.Synergy;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name="로열 스트레이트 플러쉬", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴 우클릭시 1 ~ 20사이의 숫자를 2개 뽑습니다.",
		"주변 $[range]범위 이내 모든 플레이어에게",
		"뽑은 두 숫자의 평균에 해당하는 대미지를 줍니다. $[cool]"
})
public class RoyalStraightFlush extends Synergy implements ActiveHandler {
	public static Config<Integer> range = new Config<Integer>(RoyalStraightFlush.class, "범위", 15) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}	
	},
			cool = new Config<Integer>(RoyalStraightFlush.class, "쿨타임", 90, 1) {
				@Override
				public boolean condition(Integer arg0) {
					return arg0 >= 0;
				}
	};
	
	CooldownTimer cooldown = new CooldownTimer(cool.getValue());
	
	public RoyalStraightFlush(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!cooldown.isCooldown()) {
				int r = range.getValue();
				int a = new Random().nextInt(20), b = new Random().nextInt(20);
				getPlayer().sendMessage("숫자를 뽑습니다 : "+a+", "+b);
				getPlayer().sendMessage("총 "+(double)(a+b)/2+"대미지를 상대방에게 줍니다.");
				for (Player p : LocationUtil.getNearbyPlayers(getPlayer(), r, r)) {
					p.damage((a+b)/2);
					SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR.playSound(p);
				}
				cooldown.start();
				return true;
			}
		}
		return false;
	}

}
