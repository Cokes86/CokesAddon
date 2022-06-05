package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AbilityManifest(name = "오비스니", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 상대방에게 §2맹독§f을 1 증가합니다.",
		"상대방은 매 $[DELAY]마다 §2맹독§f만큼의 대미지를 받습니다. (최대 $[MAX_DAMAGE_HIT])",
		"철괴 우클릭시 모든 플레이어의 §2맹독§f을 모두 감소하고",
		"그 수의 2배만큼의 대미지를 입힙니다. $[COOLDOWN_CONFIG]",
		"각각 플레이어마다 §2맹독§f은 최대 $[MAX_COUNTER_CONFIG]까지 증가합니다.",
		"철괴 좌클릭시 모든 플레이어의 §2맹독§f을 알 수 있습니다.",
		"0개는 따로 표시하지 않습니다.",
		"맹독을 가진 플레이어는 자신이 맹독에 걸렸는 지 알 수 없습니다."
})
public class Ovisni extends CokesAbility implements ActiveHandler {

	public static final Config<Integer> COOLDOWN_CONFIG = Config.of(Ovisni.class, "쿨타임", 30, Config.Condition.COOLDOWN);
	public static final Config<Integer> MAX_COUNTER_CONFIG = Config.of(Ovisni.class, "최대카운터", 7, FunctionalInterfaceUnit.positive());
	public static final Config<Integer> DELAY = Config.of(Ovisni.class, "지속딜레이", 10, Config.Condition.TIME);
	public static final Config<Integer> MAX_DAMAGE_HIT = Config.of(Ovisni.class, "최대_맹독_타격_횟수", 12, FunctionalInterfaceUnit.positive());

	private final Map<Participant, OvisniStack> stackMap = new ConcurrentHashMap<>();

	private final Cooldown cooldownTimer = new Cooldown(COOLDOWN_CONFIG.getValue());

	private final String icon = "☣";

	public Ovisni(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK && !cooldownTimer.isCooldown()) {
				if (!stackMap.isEmpty()) {
					Set<Entry<Participant, OvisniStack>> entries = stackMap.entrySet();
					for (Entry<Participant, OvisniStack> entry : entries) {
						if (entry != null) {
							entry.getKey().getPlayer().damage(entry.getValue().stack * 2, getPlayer());
							entry.getValue().stop(false);
						}
					}
					stackMap.clear();
					cooldownTimer.start();
					return true;
				} else {
					getPlayer().sendMessage("§2맹독 카운터§f를 가진 플레이어가 존재하지 않습니다.");
				}
			} else if (clickType == ClickType.LEFT_CLICK) {
				getPlayer().sendMessage("§e===== §2맹독§f 수치 §e=====");
				stackMap.forEach((key, value) -> getPlayer().sendMessage("§f" + key.getPlayer().getName() + " §7: §2" + value.stack));
				getPlayer().sendMessage("§e========================");
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		Entity damager = e.getDamager();
		if (damager == null) return;
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (getPlayer().equals(damager) && getGame().isGameStarted() && e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer())) {
			if (getGame().isParticipating(e.getEntity().getUniqueId())) {
				final Participant victim = getGame().getParticipant(e.getEntity().getUniqueId());
				if (!stackMap.containsKey(victim)) {
					stackMap.put(victim, new OvisniStack(victim));
				} else {
					stackMap.get(victim).addStack();
				}
			}
		}
	}

	public void onUpdate(Update update) {
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			stackMap.values().forEach(value -> value.stop(false));
			stackMap.clear();
		}
	}

	@SubscribeEvent
	private void onParticipantDeath(ParticipantDeathEvent e) {
		final Player player = e.getPlayer();
		if (player.equals(getPlayer())) {
			stackMap.values().forEach(value -> value.stop(false));
			stackMap.clear();
		} else if (getGame().isParticipating(player)) {
			final Participant participant = getGame().getParticipant(player);
			if (stackMap.containsKey(participant)) {
				stackMap.get(participant).stop(false);
				stackMap.remove(participant);
			}
		}
	}

	private class OvisniStack extends AbilityTimer {
		private final Participant target;
		private final IHologram hologram;
		private final int maxCounter = MAX_COUNTER_CONFIG.getValue();
		private int stack;
		private int damageCount = 0;

		private OvisniStack(Participant target) {
			super();
			this.setPeriod(TimeUnit.TICKS, 1);
			this.target = target;
			final Player targetPlayer = target.getPlayer();
			this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
			this.hologram.setText(CokesUtil.repeatWithTwoColor(icon, '2', stack, 'f', maxCounter-stack));
			this.hologram.display(getPlayer());
			this.stack = 1;
			this.start();
		}

		@Override
		protected void run(int arg0) {
			this.hologram.setText(CokesUtil.repeatWithTwoColor(icon, '2', stack, 'f', maxCounter-stack));
			final Player targetPlayer = target.getPlayer();
			hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
			if (arg0 % (20 * DELAY.getValue()) == 0 && damageCount <= MAX_DAMAGE_HIT.getValue()) {
				targetPlayer.damage(stack, getPlayer());
				damageCount++;
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hologram.hide(getPlayer());
			hologram.unregister();
		}

		private void addStack() {
			if (stack < maxCounter) {
				stack++;
			}
		}
	}
}
