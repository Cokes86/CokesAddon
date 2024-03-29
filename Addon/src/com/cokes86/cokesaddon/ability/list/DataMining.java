package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.*;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AbilityManifest(name = "데이터마이닝", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"§7패시브 §8- §c딥러닝§f: 플레이어의 능력사용여부, 전투여부를 알 수 있습니다.",
		"  게임에 참가중인 플레이어가 능력을 사용할 때마다 §e마이닝 스택§f이 1 상승하며,",
		"  받는 대미지 감소, 주는 대미지 증가 중 하나가 인원에 반비례하여 상승합니다.",
		"  각각 최대 받는 대미지 $[defenseUp]% 감소, 주는 대미지 $[damageUp] 증가를 부여합니다.",
		"  자신 또는 다른 데이터마이닝의 스킬로는 §e마이닝 스택§f이 증가하지 않습니다.",
		"§7패시브 §8- §c강화학습§f: 액티브 스킬이 존재하지 않는 플레이어 수에 따라",
		"  매 $[duration]마다 §e마이닝 스택§f을 자동으로 얻습니다.",
		"§7패시브 §8- §c스캐닝§f: 모든 플레이어의 능력과 체력을 알 수 있습니다.",
		"  그 정보는 각 플레이어의 머리 위에 표기됩니다.",
		"  자기 자신의 정보는 알 수 없습니다.",
		"§7금괴 우클릭 §8- §c리절트§f: 최대 스택, 스택당 상승치를 확인합니다.",
		"[아이디어 제공자 §bRainStar_§f]"
}, summarize = {
		"매 일정시간마다 혹은 타 플레이어가 능력 사용 시 스택을 얻습니다.",
		"스택을 얻음으로써 자신은 주는 대미지가 증가하거나 받는 대미지가 감소합니다.",
		"자신을 제외한 모든 플레이어의 능력을 머리 위에 표기됩니다."
})
@Tips(tip = {
		"모든 플레이어의 능력을 가장 먼저 알 수 있을 뿐 더러",
		"전투 현황, 능력 사용 여부, 이에 따른 추가적인 버프로",
		"전투가 지속될 수록 점차 강해지는 능력"
}, strong= {
		@Description(explain = { "참가자가 많을 수록 버프를 얻기 쉽다." }, subject = "많은 인원")
}, weak = {
		@Description(explain = { "참가자가 적을 수록 버프를 얻기 어렵다." }, subject = "적은 인원")
},
		stats = @Stats(offense = Level.FIVE, survival = Level.FIVE, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.EIGHT), difficulty = Difficulty.NORMAL)
@NotAvailable(AbstractTripleMix.class)
@Materials(materials = Material.GOLD_INGOT)
public class DataMining extends CokesAbility implements ActiveHandler {
	private static final Config<Double> damageUp = Config.of(DataMining.class, "max-damage-increment", 1.1, FunctionalInterfaces.positive(),
			"마이닝 스택이 최대치일 때 상대에게 주는 대미지 증가량",
			"기본값: 1.1");
	private static final Config<Double> defenseUp = Config.of(DataMining.class, "max-receive-damage-decrement", 25.0, FunctionalInterfaces.chance(false, false),
			"마이닝 스택이 최대치일 때 상대에게 받는 대미지 감소량",
			"기본값: 25.0 (%)");
	private static final Config<Integer> player_value = Config.of(DataMining.class, "stack-per-participant", 6, FunctionalInterfaces.positive(),
			"게임 참가인원수 당 스택의 최대치",
			"인원이 1명일 경우 최대 마이닝스택이 해당 설정값이며",
			"인원이 늘수록 배로 늘어납니다.",
			"기본값: 6");
	private static final Config<Integer> duration = Config.of(DataMining.class, "auto-stack-increase-duration", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME,
			"강화학습으로 얻는 스택의 시간",
			"기본값: 60 (초)");
	private final DecimalFormat df = new DecimalFormat("0.##");
	private int damage_count = 0;
	private int defense_count = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final int max_count = (getGame().getParticipants().size() - 1) * player_value.getValue();
	private final List<Scanning> scanningList = new ArrayList<>();

	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int abc) {
			if (getNoActiveHandler() != 0) {
				for (int a = 0 ; a < getNoActiveHandler(); a++) {
					if ((damage_count + defense_count) < max_count) {
						Active();
					}
				}
				final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
				final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
				ac.update("§e마이닝 스택§f: " + (damage_count + defense_count) + " (추가대미지: " + df.format(damage_value) + "  피해감소: " + df.format(defense_value) + "%)");
			}
		}
	}.setInitialDelay(TimeUnit.SECONDS, duration.getValue()).setPeriod(TimeUnit.SECONDS, duration.getValue()).register();

	public DataMining(Participant arg0) {
		super(arg0);
	}

	public void Active() {
		int count_max = max_count/2;
		if (damage_count == count_max) {
			defense_count++;
			return;
		}
		else if (defense_count == count_max) {
			damage_count++;
			return;
		}
		final Random random = new Random();
		final double randomDouble = random.nextDouble() * 99;
		final int weight = (damage_count - defense_count)/count_max;

		if (randomDouble > 50.0 - weight/2.0) {
			defense_count++;
		} else {
			damage_count++;
		}
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
			final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
			ac.update("§e마이닝 스택§f: " + (damage_count + defense_count) + " (추가대미지: " + df.format(damage_value) + "  피해감소: " + df.format(defense_value) + "%)");
			passive.start();
			for (Participant participant : getGame().getParticipants()) {
				if (participant.equals(getParticipant())) continue;
				scanningList.add(new Scanning(participant));
			}
		} else {
			for (Scanning scanning : scanningList) {
				scanning.stop(true);
			}
			scanningList.clear();
		}
	}

	@SubscribeEvent
	private void onAbilityActiveSkill(AbilityActiveSkillEvent e) {
		if (!e.getParticipant().equals(getParticipant())) {
			if ((damage_count + defense_count) < max_count) {
				Active();
			}
			final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
			final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
			ac.update("§e마이닝 스택§f: " + (damage_count + defense_count) + " (추가대미지: " + df.format(damage_value) + "  피해감소: " + df.format(defense_value) + "%)");
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.GOLD_INGOT && clickType == ClickType.RIGHT_CLICK) {
			getPlayer().sendMessage("최대 §e마이닝 스택§f: "+max_count);
			getPlayer().sendMessage("§e마이닝 스택§f상승 시 대미지 상승량: "+damageUp.getValue()*2 / max_count);
			getPlayer().sendMessage("§e마이닝 스택§f상승 시 방어력 상승량: "+defenseUp.getValue()*2 / max_count);
		}
		return false;
	}

	public int getNoActiveHandler() {
		int result = 0;
		for (Participant participant : getGame().getParticipants()) {
			if (participant.getAbility()!=null) {
				AbilityBase ability = participant.getAbility();
				if (ability instanceof Mix) {
					Mix mix = (Mix) ability;
					if (!(mix.getFirst() instanceof ActiveHandler && mix.getSecond() instanceof ActiveHandler)) {
						result++;
					}
				} else if (!(ability instanceof ActiveHandler)) {
					result++;
				}
			}
		}
		return result;
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 - (defenseUp.getValue()*2 / max_count * defense_count)/100.0));
		}

		Entity attacker = e.getDamager();
		if (attacker != null) {
			if (NMS.isArrow(attacker)) {
				Projectile arrow = (Projectile) attacker;
				if (arrow.getShooter() instanceof Entity) {
					attacker = (Entity) arrow.getShooter();
				}
			}
			if (attacker.equals(getPlayer())) {
				e.setDamage(e.getDamage() + damageUp.getValue()*2 / max_count * damage_count);
			}
		}
	}

	private class Scanning extends AbilityTimer {
		private final IHologram hologram;
		private final Participant participant;

		public Scanning(Participant participant) {
			super();
			this.participant = participant;

			this.hologram = NMS.newHologram(getPlayer().getWorld(), getPlayer().getLocation().clone().add(0,2.2,0));

			String abilityName = "-";
			if (participant.getAbility() != null) {
				abilityName = "§f"+participant.getAbility().getDisplayName();
				if (participant.getAbility() instanceof Mix) {
					Mix mix = (Mix) participant.getAbility();
					if (mix.hasSynergy()) {
						abilityName = "§e"+mix.getSynergy().getDisplayName();
					} else if (mix.getFirst() != null && mix.getSecond() != null) {
						abilityName = "§f"+mix.getFirst().getDisplayName()+" + "+mix.getSecond().getDisplayName();
					}
				}
			}
			hologram.setText(abilityName + "  §c♥"+(int)participant.getPlayer().getHealth());
			hologram.display(getPlayer());
			setPeriod(TimeUnit.TICKS, 1);
			start();
		}

		@Override
		protected void run(int count) {
			if (!hologram.isUnregistered()) {
				String abilityName = "-";
				if (participant.getAbility() != null) {
					abilityName = "§f"+participant.getAbility().getDisplayName();
					if (participant.getAbility() instanceof Mix) {
						Mix mix = (Mix) participant.getAbility();
						if (mix.hasSynergy()) {
							abilityName = "§e"+mix.getSynergy().getDisplayName();
						} else if (mix.getFirst() != null && mix.getSecond() != null) {
							abilityName = "§f"+mix.getFirst().getDisplayName()+" + "+mix.getSecond().getDisplayName();
						}
					}
				}
				hologram.setText(abilityName + "  §c♥"+(int)participant.getPlayer().getHealth());
				hologram.teleport(participant.getPlayer().getLocation().clone().add(0,2.2,0));
				if (NMS.isInvisible(participant.getPlayer())) {
					hologram.hide(getPlayer());
				} else {
					hologram.display(getPlayer());
				}
			}

			super.run(count);
		}

		@Override
		protected void onEnd() {
			hologram.unregister();
		}

		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
	}
}
