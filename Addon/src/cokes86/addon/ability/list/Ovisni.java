package cokes86.addon.ability.list;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "오비스니", rank = Rank.A, species = Species.HUMAN, explain = {
		"상대방을 공격할 시 상대방에게 §2맹독 카운터§f를 1씩 상승시키며",
		"상대방은 매 $[DELAY]마다 §2맹독 카운터§f만큼의 대미지를 수시로 입습니다.",
		"철괴 우클릭시 모든 플레이어의 §2맹독 카운터§f를 없애고",
		"그 수의 2배만큼의 대미지를 입힙니다. $[COOLDOWN_CONFIG]",
		"각각 플레이어마다 §2맹독 카운터§f는 최대 $[MAX_COUNTER_CONFIG]씩 쌓입니다.",
		"철괴 좌클릭시 모든 플레이어의 §2맹독 카운터§f를 알 수 있습니다.",
		"0개는 따로 표시하지 않습니다."
})
public class Ovisni extends AbilityBase implements ActiveHandler {

	public static final Config<Integer> COOLDOWN_CONFIG = new Config<Integer>(Ovisni.class, "쿨타임", 30, 1) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, MAX_COUNTER_CONFIG = new Config<Integer>(Ovisni.class, "최대카운터", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, DELAY = new Config<Integer>(Ovisni.class, "지속딜레이", 10, 2) {
		@Override
		public boolean condition(Integer value) {
			return value>0;
		}
	};

	private final Map<Participant, OvisniStack> stackMap = new HashMap<>();

	private final Cooldown cooldownTimer = new Cooldown(COOLDOWN_CONFIG.getValue());

	public Ovisni(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK && !cooldownTimer.isCooldown()) {
				if (!stackMap.isEmpty()) {
					for (Entry<Participant, OvisniStack> entry : stackMap.entrySet()) {
						entry.getKey().getPlayer().damage(entry.getValue().stack * 2, getPlayer());
						entry.getValue().stop(false);
					}
					stackMap.clear();
					cooldownTimer.start();
					return true;
				} else {
					getPlayer().sendMessage("§2맹독 카운터§f를 가진 플레이어가 존재하지 않습니다.");
				}
			} else if (clickType == ClickType.LEFT_CLICK) {
				getPlayer().sendMessage("§e===== §2맹독 카운터§f 수치 §e=====");
				stackMap.forEach((key, value) -> getPlayer().sendMessage("§f"+key.getPlayer().getName() + " §7: §2" + value.stack));
				getPlayer().sendMessage( "§e========================");
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

		private int stack;
		private final Participant target;
		private final IHologram hologram;
		private final int maxCounter = MAX_COUNTER_CONFIG.getValue();

		private OvisniStack(Participant target) {
			super();
			this.setPeriod(TimeUnit.TICKS, 1);
			this.target = target;
			final Player targetPlayer = target.getPlayer();
			this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
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
			if (arg0 % (20*DELAY.getValue()) == 0) {
				targetPlayer.damage(stack, getPlayer());
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
