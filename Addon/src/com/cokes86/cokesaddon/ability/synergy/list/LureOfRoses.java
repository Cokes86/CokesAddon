package com.cokes86.cokesaddon.ability.synergy.list;

import java.util.function.Predicate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.timer.InvincibilityTimer;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "장미의 유혹", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §c가시돋움§f: 상대방을 공격할 시 §d가시 카운터§f를 1씩 올리고,",
		"  (§d가시 카운터§f*$[PRICKLY_MULTIPLY])의 추가대미지를 줍니다. (최대 $[PRICKLY_MAX_DAMAGE]대미지)",
		"  또한 모든 받는 대미지가 (§d가시 카운터§f * $[PRICKLY_RECEIVE_MULTIPLY])%만큼 증가합니다. (최대 50%)",
		"§7쿨타임 패시브 §8- §c유혹§f: 쿨타임이 아닐 때 자신이 죽을 위기에 처할 시",
		"  모든 §d가시 카운터§f를 소비하고 반경 10블럭 이내 모든 플레이어에게 (§d가시 카운터§f*0.5)초의",
		"  블라인드 효과를 준 후 지속시간동안 자신은 무적이 되며,",
		"  0.5초마다 채력을 1씩 회복합니다. $[COOLDOWN]"
})
@Beta
public class LureOfRoses extends CokesSynergy {
	private static final Config<Integer> COOLDOWN = Config.cooldown(LureOfRoses.class, "cooldown", 300,
			"쿨타임 패시브 <유혹> 쿨타임",
			"기본값: 300 (초)");

	private static final Config<Double> PRICKLY_MULTIPLY = Config.positive(LureOfRoses.class, "prickly-multiply", .25,
			"가시 카운터당 상대에게 주는 대미지 증가량",
			"기본값: 0.25");
	private static final Config<Double> PRICKLY_MAX_DAMAGE = Config.positive(LureOfRoses.class, "prickly-max-damage", 7d,
			"상대에게 주는 대미지 최대치",
			"기본값: 7.0");
	private static final Config<Double> PRICKLY_RECEIVE_MULTIPLY = Config.positive(LureOfRoses.class, "prickly-receive-multiply", 1.0,
			"가시 카운터당 받는 대미지 증가량 배율",
			"기본값: 1.0");

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				return !game.getDeathManager().isExcluded(entity.getUniqueId());
			}
		}
		return true;
	};
	private int counter = 0;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final InvTimer duration = new InvTimer(this);

	private final ActionbarChannel channel = newActionbarChannel();

	public LureOfRoses(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	public void onCEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 + Math.min(counter * PRICKLY_RECEIVE_MULTIPLY.getValue(), 50.0) / 100));
			if (duration.isRunning()) {
				e.setCancelled(true);
			} else if (!cooldown.isRunning() && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				e.setDamage(0);
				getPlayer().setHealth(1);
				duration.setMaximumCount(counter);
				duration.start();
				setCounter(0);
			}
		}

		Entity attacker = CokesUtil.getDamager(e.getDamager());
		if (attacker.equals(getPlayer()) && e.getEntity() instanceof Player && getGame().getParticipant((Player) e.getEntity()) != null) {
			setCounter(counter++);
			e.setDamage(e.getDamage() + Math.min(counter * PRICKLY_MULTIPLY.getValue(), PRICKLY_MAX_DAMAGE.getValue()));
		}
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			channel.update(CokesUtil.noticeString("§d가시 카운터", counter));
		}
	}

	private void setCounter(int counter) {
		this.counter = counter;
		channel.update(CokesUtil.noticeString("§d가시 카운터", counter));
	}

	private class InvTimer extends InvincibilityTimer {
		public InvTimer(AbilityBase base) {
			super(base, 10, true);
			setPeriod(TimeUnit.TICKS, 10);
		}

		@Override
		protected void run(int count) {
			for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
				PotionEffects.BLINDNESS.addPotionEffect(p, 50, 0, true);
			}
			CokesUtil.healPlayer(getPlayer(), 1);
		}
	}
}
