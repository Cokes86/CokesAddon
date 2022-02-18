package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@AbilityManifest(name = "하모니", rank = Rank.C, species = Species.HUMAN, explain = {
		"$[duration]마다 주변 $[range]블럭 이내의 플레이어의 수의 반만큼 체력을 회복하고,",
		"그 주변 플레이어 역시 0.5의 체력이 증가합니다.",
		"주변 3명 이상의 플레이어가 아래 3개의 조건 중 하나 이상을 만족할 경우",
		"이 능력은 2배의 효과를 가집니다.",
		"  ● 모든 플레이어의 능력의 등급이 모두 같거나 다르다.",
		"  ● 모든 플레이어의 능력의 종족이 모두 같거나 다르다.",
		"  ● 모든 플레이어의 능력이 §c코크스 애드온§f의 능력이다.",
		"  믹스 능력자의 경우, 같은 위치에 해당하는 능력만 해당한다.",
		"§8[§7HIDDEN§8] §b완벽한 조화§f: 완벽한 조화라는 것은 무엇일까?"
})
public class Harmony extends CokesAbility {
	private static final Config<Integer> duration = new Config<>(Harmony.class, "주기", 5, Config.Condition.TIME);
	private static final Config<Integer> range = new Config<>(Harmony.class, "범위", 10, PredicateUnit.positive());
	private boolean hidden = false;

	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				return !game.getDeathManager().isExcluded(entity.getUniqueId());
			}
			Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof Teamable) {
				Teamable game = (Teamable) getGame();
				return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};


	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int arg0) {
			List<Player> near = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
			Map<Rank, Integer> rankMap = new HashMap<>();
			Map<Species, Integer> speciesMap = new HashMap<>();
			int cokes = 0;
			boolean enhance = true;
			if (near.size() >= 3) {
				for (Player p : near) {
					Participant participant = getGame().getParticipant(p);
					if (participant instanceof AbstractMix.MixParticipant && getParticipant() instanceof AbstractMix.MixParticipant) {
						Mix me = ((AbstractMix.MixParticipant) getParticipant()).getAbility();
						Mix mix = ((AbstractMix.MixParticipant) participant).getAbility();

						if (me != null && me.hasAbility() && (me.getFirst().getClass().equals(Harmony.class) || me.getSecond().getClass().equals(Harmony.class))) {
							if (mix != null && mix.hasAbility() && !mix.hasSynergy()) {
								AbilityBase base = me.getFirst().getClass().equals(Harmony.class) ? mix.getFirst() : mix.getSecond();

								Rank rank = base.getRank();
								rankMap.put(rank, rankMap.getOrDefault(rank, 0)+1);

								//종족
								Species species = base.getSpecies();
								speciesMap.put(species, speciesMap.getOrDefault(species, 0)+1);

								//코크스
								if (base instanceof CokesAbility) {
									cokes++;
								}
							} else {
								enhance = false;
							}
						}
					} else {
						if (participant.getAbility() != null) {
							//등급
							Rank rank = participant.getAbility().getRank();
							rankMap.put(rank, rankMap.getOrDefault(rank, 0)+1);

							//종족
							Species species = participant.getAbility().getSpecies();
							speciesMap.put(species, speciesMap.getOrDefault(species, 0)+1);

							//코크스
							if (participant.getAbility() instanceof CokesAbility) {
								cokes++;
							}
						}
					}
				}

				if (enhance) {
					boolean first = true, second = true, third = true;
					if (rankMap.keySet().size() != 1) {
						for (int i : rankMap.values()) {
							if (i != 1) {
								first = false;
								break;
							}
						}
					}
					if (speciesMap.keySet().size() != 1) {
						for (int i : speciesMap.values()) {
							if (i != 1) {
								second = false;
								break;
							}
						}
					}
					if (cokes != near.size()) {
						third = false;
					}

					enhance = first || second || third;

					if (first && second && third && !hidden) {
						SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
						getPlayer().sendMessage("§8[§7HIDDEN§8] §f당신은 모든 조화로움을 받아냈습니다.");
						getPlayer().sendMessage("§8[§7HIDDEN§8] §b완벽한 조화§f를 달성하였습니다.");
						hidden = true;
						this.setPeriod(TimeUnit.SECONDS, duration.getValue()/2);
					}
				}
			} else {
				enhance = false;
			}

			for (Player p : near) {
				Healths.setHealth(p, p.getHealth() + (enhance ? 1 : 0.5));
			}
			Healths.setHealth(getPlayer(), getPlayer().getHealth() + near.size() / (enhance ? 1.0 : 2.0));
		}
	}.setPeriod(TimeUnit.SECONDS, duration.getValue());

	public Harmony(Participant arg0) {
		super(arg0);
		passive.register();
	}

	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
}
