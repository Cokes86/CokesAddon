package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

@AbilityManifest(name = "여왕", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
		"상대방을 철괴로 우클릭시 상대방의 남은 체력의 절반만큼",
		"최대체력이 증가하고 그 수치의 $[multiply]배만큼의 체력을 회복합니다. $[cool]",
		"능력을 사용할 때 마다, 최대체력이 게임 설정 초깃값으로 수정된 후 증가합니다.",
		"철괴 좌클릭시, 자신의 최대체력을 수치로 확인할 수 있습니다."
})
public class Queen extends CokesAbility implements ActiveHandler, TargetHandler {
	private static final Config<Integer> cool = new Config<Integer>(Queen.class, "쿨타임", 120, Config.Condition.COOLDOWN) {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	private static final Config<Double> multiply = new Config<Double>(Queen.class, "회복배율", 1.0) {
		@Override
		public boolean condition(Double aDouble) {
			return aDouble >= 0;
		}
	};
	private final DecimalFormat df = new DecimalFormat("#.##");
	private final double defaultHealth = AttributeUtil.getMaxHealth(getPlayer());
	private final Cooldown cooldown = new Cooldown(cool.getValue());

	public Queen(Participant arg0) {
		super(arg0);
	}

	@Override
	public void TargetSkill(Material arg0, LivingEntity arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1 instanceof Player) {
			Player target = (Player) arg1;
			if (getGame().isParticipating(target) && !cooldown.isCooldown()) {
				double plus = target.getHealth() / 2;

				AttributeUtil.setMaxHealth(getPlayer(), defaultHealth+plus);
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + (plus*multiply.getValue()));
				cooldown.start();
			}
		}
	}

	@Override
	protected void onUpdate(AbilityBase.Update update) {
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			AttributeUtil.setMaxHealth(getPlayer(), defaultHealth);
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onGameEnd(GameEndEvent e) {
		AttributeUtil.setMaxHealth(getPlayer(), defaultHealth);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.LEFT_CLICK)) {
			getPlayer().sendMessage("현재 최대체력: " + df.format(AttributeUtil.getMaxHealth(getPlayer())));
		}
		return false;
	}
}
