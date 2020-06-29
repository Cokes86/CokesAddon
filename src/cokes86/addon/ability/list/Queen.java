package cokes86.addon.ability.list;

import java.text.DecimalFormat;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import cokes86.addon.ability.Test;
import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameEndEvent;

@AbilityManifest(name = "여왕", rank= AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
		"상대방을 철괴로 우클릭시 상대방의 남은 체력의 절반만큼",
		"최대체력이 증가하고 그 수치만큼 체력을 회복합니다. $[cool]",
		"능력을 사용할 때 마다, 최대체력이 게임 설정 초깃값으로 수정된 후 증가합니다.",
		"철괴 좌클릭시, 자신의 최대체력을 수치로 확인할 수 있습니다."
})
@Test
public class Queen extends AbilityBase implements ActiveHandler, TargetHandler {
	DecimalFormat df = new DecimalFormat(".00");
	double defaultHealth = Settings.getDefaultMaxHealth();
	
	public static Config<Integer> cool = new Config<Integer>(Queen.class, "쿨타임", 120, 1) {

		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
		
	};
	CooldownTimer cooldown = new CooldownTimer(cool.getValue());
	
	public Queen(Participant arg0) {
		super(arg0);
		getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultHealth);
	}

	@Override
	public void TargetSkill(Material arg0, LivingEntity arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1 instanceof Player) {
			Player target = (Player) arg1;
			if (getGame().isParticipating(target) && !cooldown.isCooldown()) {
				double plus = target.getHealth()/2;
				
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultHealth + plus);
				getPlayer().setHealth(getPlayer().getHealth() + plus);
				cooldown.start();
			}
		}
	}
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultHealth);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onGameEnd(GameEndEvent e) {
		getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultHealth);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK)) {
			getPlayer().sendMessage("현재 최대체력: "+ df.format(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
		}
		return false;
	}
}
