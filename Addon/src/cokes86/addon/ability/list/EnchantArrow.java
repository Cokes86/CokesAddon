package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import cokes86.addon.util.TextMaker;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Objects;

@AbilityManifest(name = "인챈트 애로우", rank = Rank.S, species = Species.HUMAN, explain = {
		"활로 플레이어를 적중할 시 원거리 대미지가 (§b인챈트 스택§f * $[DAMAGE])% 증가하며,",
		"§b인챈트 스택§f이 거리에 비례하여 1에서 $[MAX_STACK_UP]만큼 증가합니다. (최대 $[MAX_STACK]회)",
		"적중에 실패할 시 §b인챈트 스택§f이 거리에 비례하여 1에서 $[MAX_STACK_UP]만큼 감소합니다.",
		"§b인챈트 스택§f이 0인 상태로 적중에 실패할 시 $[RISK]의 고정 대미지를 입습니다.",
		"자신이 쏜 화살은 명중 시 바로 사라집니다.",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class EnchantArrow extends CokesAbility {
	private static final Config<Double> DAMAGE = new Config<>(EnchantArrow.class, "damage", 8.5, PredicateUnit.positive(),
			"# 인챈트 스택 당 상승할 대미지 증가량",
			"# 기본값: 8.5 (%)");
	private static final Config<Integer> RISK = new Config<>(EnchantArrow.class, "risk", 1, PredicateUnit.positive(),
			"# 인챈트 스택이 없는 상태에서 적중 실패 시 받는 대미지",
			"# 기본값: 1");
	private static final Config<Integer> MAX_STACK = new Config<>(EnchantArrow.class, "max-stack", 9, PredicateUnit.positive(),
			"# 인챈트 스택의 최대치",
			"# 기본값: 9");
	private static final Config<Integer> MAX_STACK_UP = new Config<>(EnchantArrow.class, "max-increase-stack", 3, PredicateUnit.positive(),
			"# 인챈트 스택의 최대 증가량",
			"# 기본값: 3");
	private static final Config<Integer> MAX_STACK_DOWN = new Config<>(EnchantArrow.class, "max-decrease-stack", 3, PredicateUnit.positive(),
			"# 인챈트 스택의 최대 감소량",
			"# 기본값: 3");
	private static final Config<Integer> DISTANCE = new Config<>(EnchantArrow.class, "distance", 10, PredicateUnit.positive(),
			"# 인챈트 스택이 증가하거나 감소할 때 거리의 비례량",
			"# 가까울 수록 인챈트 스택이 적게 증가, 크게 감소합니다.",
			"# 기본값: 10 (블럭)");
	private int enchantStack = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final String notice = "⤐";

	public EnchantArrow(Participant participant) {
		super(participant);
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', MAX_STACK.getValue() - enchantStack));
		}
	}

	@SubscribeEvent(priority = 5)
	private void onProjectileHit(ProjectileHitEvent e) {
		if (Objects.equals(e.getEntity().getShooter(), getPlayer())) {
			if (e.getHitEntity() == null) {
				SoundLib.ENTITY_VILLAGER_NO.playSound(getPlayer());
				if (enchantStack == 0) {
					getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth() - RISK.getValue()));
				} else {
					final double length = getPlayer().getLocation().clone().subtract(e.getEntity().getLocation().clone()).length();
					enchantStack -= Math.min(MAX_STACK_DOWN.getValue(), Math.max(1, MAX_STACK_DOWN.getValue() - length / DISTANCE.getValue()));
					if (enchantStack < 0) enchantStack = 0;
				}
			}
			e.getEntity().remove();
			ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', MAX_STACK.getValue() - enchantStack));
		}
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && NMS.isArrow(e.getDamager()) && !e.getEntity().equals(getPlayer())) {
			Projectile arrow = (Projectile) e.getDamager();
			if (Objects.equals(arrow.getShooter(), getPlayer())) {
				e.setDamage(e.getDamage() * (1 + enchantStack * DAMAGE.getValue() / 100.0));
				final double length = getPlayer().getLocation().clone().subtract(e.getEntity().getLocation().clone()).length();
				enchantStack += Math.min(MAX_STACK_UP.getValue(), length / DISTANCE.getValue() + 1);
				if (enchantStack >= MAX_STACK.getValue()) enchantStack = MAX_STACK.getValue();
				ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', MAX_STACK.getValue() - enchantStack));
				arrow.remove();
			}
		}
	}
}
