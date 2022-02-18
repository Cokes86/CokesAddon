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
		"활로 플레이어를 적중할 시 원거리 대미지가 (§b인챈트 스택§f * $[damage])% 증가하며,",
		"§b인챈트 스택§f이 거리에 비례하여 1에서 $[max_stack_up]만큼 증가합니다. (최대 $[max_stack]회)",
		"적중에 실패할 시 §b인챈트 스택§f이 거리에 비례하여 1에서 $[max_stack_down]만큼 감소합니다.",
		"§b인챈트 스택§f이 0인 상태로 적중에 실패할 시 $[risk]의 고정 대미지를 입습니다.",
		"자신이 쏜 화살은 명중 시 바로 사라집니다.",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class EnchantArrow extends CokesAbility {
	private static final Config<Integer> damage = new Config<>(EnchantArrow.class, "추가대미지(%)", 10, PredicateUnit.positive());
	private static final Config<Integer> risk = new Config<>(EnchantArrow.class, "리스크", 1, PredicateUnit.positive());
	private static final Config<Integer> max_stack = new Config<>(EnchantArrow.class, "최대스택", 9, PredicateUnit.positive());
	private static final Config<Integer> max_stack_up = new Config<>(EnchantArrow.class, "최대스택상승치", 3, PredicateUnit.positive());
	private static final Config<Integer> max_stack_down = new Config<>(EnchantArrow.class, "최대스택감소치", 3, PredicateUnit.positive());
	private int enchantStack = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final String notice = "⤐";

	public EnchantArrow(Participant participant) {
		super(participant);
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', max_stack.getValue() - enchantStack));
		}
	}

	@SubscribeEvent(priority = 5)
	private void onProjectileHit(ProjectileHitEvent e) {
		if (Objects.equals(e.getEntity().getShooter(), getPlayer())) {
			if (e.getHitEntity() == null) {
				SoundLib.ENTITY_VILLAGER_NO.playSound(getPlayer());
				if (enchantStack == 0) {
					getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth() - risk.getValue()));
				} else {
					final double length = getPlayer().getLocation().clone().subtract(e.getEntity().getLocation().clone()).length();
					enchantStack -= Math.min(max_stack_down.getValue(), Math.max(1, 3 - length / 7));
					if (enchantStack < 0) enchantStack = 0;
				}
			}
			e.getEntity().remove();
			ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', max_stack.getValue() - enchantStack));
		}
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && NMS.isArrow(e.getDamager()) && !e.getEntity().equals(getPlayer())) {
			Projectile arrow = (Projectile) e.getDamager();
			if (Objects.equals(arrow.getShooter(), getPlayer())) {
				e.setDamage(e.getDamage() * (1 + enchantStack * damage.getValue() / 100.0));
				final double length = getPlayer().getLocation().clone().subtract(e.getEntity().getLocation().clone()).length();
				enchantStack += Math.min(max_stack_up.getValue(), length / 7 + 1);
				if (enchantStack >= max_stack.getValue()) enchantStack = max_stack.getValue();
				ac.update(TextMaker.repeatWithTwoColor(notice, 'b', enchantStack, 'f', max_stack.getValue() - enchantStack));
				arrow.remove();
			}
		}
	}
}
