package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
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
		"§7철괴 우클릭 §8- §c여왕의 품격§f: 바라보는 대상의 체력의 반만큼",
		"  자신 최대 체력에 추가하고, 그 수치의 $[MULTIPLY]배만큼의 체력을 회복합니다. $[COOLDOWN]",
		"  재사용시 그 전에 추가되었던 최대 체력은 사라지고 새로 추가됩니다.",
		"§7철괴 좌클릭 §8- §c상태 관리§f: 자신의 최대 체력의 수치를 확인합니다."
})
public class Queen extends CokesAbility implements ActiveHandler, TargetHandler {
	private static final Config<Integer> COOLDOWN = Config.of(Queen.class, "쿨타임", 120, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private static final Config<Double> MULTIPLY = Config.of(Queen.class, "회복배율", 1.0, FunctionalInterfaces.greaterThanOrEqual(0.0));
	private final DecimalFormat df = new DecimalFormat("0.##");
	private final double defaultHealth = AttributeUtil.getMaxHealth(getPlayer());
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

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
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + (plus* MULTIPLY.getValue()));
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
