package cokes86.addon.ability.list;

import org.bukkit.Material;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.base.Strings;

@Beta
@AbilityManifest(name = "물고기", rank = Rank.B, species = Species.ANIMAL, explain = {
		"자신이 물 속에 있을 시 수중호흡, 신속 1 효과를 얻습니다.",
		"물 밖에 있을 시 구속효과가 걸리며 이후 3초마다 수분이 감소합니다.",
		"감소할 수분이 없을 경우 1의 고정대미지를 입습니다.",
		"철괴 좌클릭 시 자신의 위치에 물을 설치합니다. $[cool]"
})
public class Fish extends AbilityBase implements ActiveHandler {
	private static final Config<Integer> cool =  new Config<Integer>(Fish.class, "쿨타임", 30, 1) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}		
	};
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown c = new Cooldown(cool.getValue());
	private int moisture = 10;

	public Fish(Participant arg0) {
		super(arg0);
		timer.register();
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !c.isCooldown()) {
			getPlayer().getLocation().clone().getBlock().setType(Material.WATER);
			c.start();
			return true;
		}
		return false;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			timer.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	private AbilityTimer timer = new AbilityTimer() {
		int count = 0;
		int up = 0;

		@Override
		protected void run(int arg0) {
			if (getPlayer().getLocation().getBlock().isLiquid()) {
				if (count != 0) {
					PotionEffects.SLOW.removePotionEffect(getPlayer());
					count = 0;
				}
				up++;
				if (up % 20 == 0 && moisture < 10) {moisture++;}
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 21, 0, true);
				PotionEffects.WATER_BREATHING.addPotionEffect(getPlayer(), 21, 0, true);
			} else {
				if (count == 0) {
					PotionEffects.SPEED.removePotionEffect(getPlayer());
					PotionEffects.WATER_BREATHING.removePotionEffect(getPlayer());
					up = 0;
				}
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 21, 0, true);
				count++;
				if (count % 60 == 0) {
					if (moisture > 0) {
						moisture--;
					} else {
						Damages.damageFixed(getPlayer(), getPlayer(), 1);
					}
				}
			}
			ac.update("§b".concat(Strings.repeat("●", moisture)).concat(Strings.repeat("○", 10 - moisture)));
		}
		
	};
}
