package cokes86.addon.ability.list;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Scheduled;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.compat.nms.Hologram;
import daybreak.abilitywar.utils.base.minecraft.compat.nms.NMSHandler;
import daybreak.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "오비스니", rank = Rank.B, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 상대방에게 §2맹독 카운터§f를 1씩 상승시키며",
		"상대방은 매 10초마다 §2맹독 카운터§f만큼의 대미지를 수시로 입습니다.",
		"철괴 우클릭시 모든 플레이어의 §2맹독 카운터§f를 없애고",
		"그 수의 2배만큼의 대미지를 입힙니다. $[cool]",
		"각각 플레이어마다 §2맹독 카운터§f는 최대 $[max]씩 쌓입니다.",
		"철괴 좌클릭시 모든 플레이어의 §2맹독 카운터§f를 알 수 있습니다.",
		"0개는 따로 표시하지 않습니다."
})
public class Ovisni extends AbilityBase implements ActiveHandler {

	public static final Config<Integer> COOLDOWN_CONFIG = new Config<Integer>(Ovisni.class, "쿨타임", 30, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, MAX_COUNTER_CONFIG = new Config<Integer>(Ovisni.class, "최대카운터", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	private final int maxCounter = MAX_COUNTER_CONFIG.getValue();

	private final Map<Participant, OvisniStack> stackMap = new HashMap<>();

	private final CooldownTimer cooldownTimer = new CooldownTimer(COOLDOWN_CONFIG.getValue());

	@Scheduled
	private final Timer passive = new Timer() {
		@Override
		protected void run(int arg0) {
			for (Entry<Participant, OvisniStack> entry : stackMap.entrySet()) {
				entry.getKey().getPlayer().damage(entry.getValue().stack, getPlayer());
			}
		}
	}.setPeriod(TimeUnit.SECONDS, 10);

	public Ovisni(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!cooldownTimer.isCooldown()) {
					for (Entry<Participant, OvisniStack> entry : stackMap.entrySet()) {
						entry.getKey().getPlayer().damage(entry.getValue().stack * 2, getPlayer());
						entry.getValue().stop(false);
					}
					stackMap.clear();
					cooldownTimer.start();
				}
				return true;
			} else if (clickType == ClickType.LEFT_CLICK) {
				getPlayer().sendMessage("§e===== §2맹독 카운터§f 수치 §e=====");
				for (Entry<Participant, OvisniStack> entry : stackMap.entrySet()) {
					getPlayer().sendMessage("§f" + entry.getKey().getPlayer().getName() + " §7: §2" + entry.getValue().stack);
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (getPlayer().equals(damager) && getGame().isGameStarted() && e.getEntity() instanceof Player) {
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

	@SubscribeEvent
	private void onParticipantDeath(ParticipantDeathEvent e) {
		final Player player = e.getPlayer();
		if (player.equals(getPlayer())) {
			for (OvisniStack stack : stackMap.values()) {
				stack.stop(false);
			}
			stackMap.clear();
		} else if (getGame().isParticipating(player)) {
			final Participant participant = getGame().getParticipant(player);
			stackMap.get(participant).stop(false);
		}
	}

	private class OvisniStack extends Timer {

		private int stack;
		private final Participant target;
		private final Hologram hologram;

		private OvisniStack(Participant target) {
			super();
			this.setPeriod(TimeUnit.TICKS, 1);
			this.target = target;
			final Player targetPlayer = target.getPlayer();
			this.hologram = NMSHandler.getNMS().newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
			this.hologram.setText(Strings.repeat("§2◆", stack).concat(Strings.repeat("§2◇", maxCounter - stack)));
			this.hologram.display(getPlayer());
			this.stack = 1;
			this.start();
		}

		@Override
		protected void run(int arg0) {
			this.hologram.setText(Strings.repeat("§2◆", stack).concat(Strings.repeat("§2◇", maxCounter - stack)));
			final Player targetPlayer = target.getPlayer();
			hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hologram.hide(getPlayer());
			stackMap.remove(target);
		}

		private void addStack() {
			if (stack < maxCounter) {
				stack++;
			}
		}
	}
}
