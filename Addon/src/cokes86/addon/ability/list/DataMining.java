package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.*;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.text.DecimalFormat;
import java.util.Random;

@AbilityManifest(name = "데이터마이닝", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"§7패시브 §8- §c딥러닝§f: 플레이어의 능력사용여부, 전투여부를 알 수 있습니다.",
		"  게임에 참가중인 플레이어가 능력을 사용할 때마다 §e마이닝 스택§f이 1 상승하며,",
		"  받는 대미지 감소, 주는 대미지 증가 중 하나가 인원에 반비례하여 상승합니다.",
		"  각각 최대 받는 대미지 $[defenseUp]% 감소, 주는 대미지 $[damageUp] 증가를 부여합니다.",
		"  자신 또는 다른 데이터마이닝의 스킬로는 §e마이닝 스택§f이 증가하지 않습니다.",
		"§7패시브 §8- §c강화학습§f: 액티브 스킬이 존재하지 않는 플레이어 수에 따라",
		"  매 $[duration]마다 §e마이닝 스택§f을 자동으로 얻습니다.",
		"§7철괴 우클릭 §8- §c스캐닝§f: 자신을 제외한 모든 플레이어의 능력을 확인합니다.",
		"§7철괴 좌클릭 §8- §c뮤트§f: §c딥러닝§f으로 인한 알림을 컨트롤합니다.",
		"  웅크리고 사용 시 전투와 스택 획득 여부를, 그 이외의 경우는 스킬사용의 여부를",
		"  끄고 킬 수 있습니다.",
		"§7금괴 우클릭 §8- §c리절트§f: 최대 스택, 스택당 상승치를 확인합니다.",
		"[아이디어 제공자 §bRainStar_§f]"
}, summarize = {
		"매 일정시간마다 혹은 타 플레이어가 능력 사용 시 스택을 얻습니다.",
		"스택을 얻음으로써 자신은 주는 대미지가 증가하거나 받는 대미지가 감소합니다.",
		"철괴 우클릭 시 자신을 제외한 모든 플레이어의 능력을 볼 수 있습니다."
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
@Materials(materials = {Material.IRON_INGOT, Material.GOLD_INGOT})
public class DataMining extends CokesAbility implements ActiveHandler {
	private static final Config<Double> damageUp = new Config<>(DataMining.class, "최대주는대미지성장치", 2.5, PredicateUnit.positive());
	private static final Config<Double> defenseUp = new Config<>(DataMining.class, "최대받는대미지감소성장치", 25.00, PredicateUnit.positive(), "#단위: %");
	private static final Config<Integer> player_value = new Config<>(DataMining.class, "인원별_스택치", 4, PredicateUnit.positive());
	private static final Config<Integer> duration = new Config<>(DataMining.class, "자동스택추가주기", 60, Config.Condition.TIME);
	private final DecimalFormat df = new DecimalFormat("#.##");
	private int damage_count = 0;
	private int defense_count = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private boolean pvpMessage = true, skillMessage= true;
	private final int max_count = (getGame().getParticipants().size() - 1) * player_value.getValue();

	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int abc) {
			if (getNoActiveHandler() != 0) {
				boolean up = false;
				for (int a = 0 ; a < getNoActiveHandler(); a++) {
					if ((damage_count + defense_count) < max_count) {
						Active();
						up = true;
					}
				}
				if (pvpMessage && up) getPlayer().sendMessage("자동으로 §e마이닝 스택§f을 획득하였습니다.");
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
		final Random random = new Random();
		final double randomDouble = random.nextDouble() * 2;
		if (randomDouble > 1) {
			if (damage_count == max_count / 2) {
				defense_count++;
			} else {
				damage_count++;
			}
		} else {
			if (defense_count == max_count / 2) {
				damage_count++;
			} else {
				defense_count++;
			}
		}
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
			final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
			ac.update("§e마이닝 스택§f: " + (damage_count + defense_count) + " (추가대미지: " + df.format(damage_value) + "  피해감소: " + df.format(defense_value) + "%)");
			passive.start();
		}
	}

	@SubscribeEvent
	private void onAbilityActiveSkill(AbilityActiveSkillEvent e) {
		if (!e.getParticipant().equals(getParticipant())) {
			if (skillMessage) getPlayer().sendMessage("§e" + e.getPlayer().getName() + "§f님이 능력을 사용하였습니다.");
			if ((damage_count + defense_count) < max_count) {
				Active();
			}
			final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
			final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
			ac.update("§e마이닝 스택§f: " + (damage_count + defense_count) + " (추가대미지: " + df.format(damage_value) + "  피해감소: " + df.format(defense_value) + "%)");
		}
	}

	@SubscribeEvent(priority = 4)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		final double damage_value = damageUp.getValue()*2 / max_count * damage_count;
		final double defense_value = defenseUp.getValue()*2 / max_count * defense_count;
		if (e.getEntity() instanceof Player) {
			Player entity = (Player) e.getEntity();
			Entity damager = e.getDamager();
			if (damager instanceof Arrow) {
				Arrow arrow = (Arrow) damager;
				if (arrow.getShooter() instanceof Entity) {
					damager = (Entity) arrow.getShooter();
				}
			}

			if (damager instanceof Player) {
				if (damager.equals(getPlayer())) {
					e.setDamage(e.getDamage() + damage_value);
				} else if (entity.equals(getPlayer())) {
					e.setDamage(e.getDamage() * (100.0 - defense_value) / 100.0);
				}

				if (!e.isCancelled()) {
					if (pvpMessage) getPlayer().sendMessage(
							"§e" + damager.getName() + "§f(§c♥" + df.format(((Player) damager).getHealth()) + "§f)님이 §e" + entity.getName()
									+ "§f(§c♥" + df.format(entity.getHealth()) + "§f)님을 공격! (대미지: " + df.format(e.getFinalDamage()) + ")");
				}
			}
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			AbstractGame game = getGame();
			getPlayer().sendMessage("§2===== §a능력자 목록 §2=====");
			int count = 0;
			for (Participant p : game.getParticipants()) {
				daybreak.abilitywar.ability.AbilityBase ability = p.getAbility();
				if (p.equals(getParticipant()))
					continue;
				if (ability == null)
					continue;
				if (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(p.getPlayer()))
					continue;
				String name;
				if (ability instanceof Mix) {
					Mix mix = (Mix) ability;
					if (!mix.hasAbility()) continue;
					if (mix.hasSynergy()) {
						Synergy synergy = mix.getSynergy();
						Pair<AbilityRegistration, AbilityRegistration> base = SynergyFactory
								.getSynergyBase(synergy.getRegistration());
						name = "§e" + synergy.getDisplayName() + " §f(§e" + base.getLeft().getManifest().name() + " §f+ §e"
								+ base.getRight().getManifest().name() + "§f)";
					} else {
						if (mix.getFirst() != null && mix.getSecond() == null) {
							name = "§e" + mix.getFirst().getDisplayName();
						} else if (mix.getFirst() == null && mix.getSecond() != null) {
							name = "§e" + mix.getSecond().getDisplayName();
						} else {
							name = "§e" + mix.getFirst().getDisplayName() + " §f+ §e" + mix.getSecond().getDisplayName();
						}
					}
				} else {
					name = ability.getDisplayName();
				}

				count++;
				getPlayer().sendMessage("§e" + count + ". §f" + p.getPlayer().getName() + " §7: §e" + name);
			}
			getPlayer().sendMessage("§2========================");
		} else if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK) {
			if (getPlayer().isSneaking()) {
				pvpMessage = !pvpMessage;
				if (pvpMessage) getPlayer().sendMessage("전투, 스택 획득 여부 메세지를 볼 수 있게 됩니다.");
				else getPlayer().sendMessage("전투, 스택 획득 여부 메세지를 볼 수 없게 됩니다.");
			} else {
				skillMessage = !skillMessage;
				if (skillMessage) getPlayer().sendMessage("스킬 사용 여부 메세지를 볼 수 있게 됩니다.");
				else getPlayer().sendMessage("스킬 사용 여부 메세지를 볼 수 없게 됩니다.");
			}
		} else if (material == Material.GOLD_INGOT && clickType == ClickType.RIGHT_CLICK) {
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
				daybreak.abilitywar.ability.AbilityBase ability = participant.getAbility();
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

}
