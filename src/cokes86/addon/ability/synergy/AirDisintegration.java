package cokes86.addon.ability.synergy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import cokes86.addon.configuration.synergy.Config;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Scheduled;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mixability.synergy.Synergy;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil.Predicates;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "공중 분해", rank = Rank.A, species = Species.HUMAN, explain = {
		"5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 10만큼 상승합니다.",
		"철괴 우클릭시 §d사슬 카운터§f를 전부 소모하여 플레이어를 (§d사슬 카운터§f/2+4)블럭만큼 공중에 고정시킨 후",
		"3초마다 고정된 플레이어 중 한명에게 이동하고 누적된 §d사슬 카운터§f만큼의 대미지를 준 후 떨어트립니다.",
		"자신은 이후 1초간 공중에 날 수 있으며 능력 사용 이후 1회에 한정해 낙하대미지를 받지 않습니다."
})
public class AirDisintegration extends Synergy implements ActiveHandler {
	public static Config<Integer> range = new Config<Integer>(AirDisintegration.class, "범위", 7) {
		@Override
		public boolean condition(Integer arg0) {
			return arg0 > 0;
		}
	};

	int chain = 0;
	ActionbarChannel ac = newActionbarChannel();
	private final Predicate<Entity> STRICT_PREDICATE = Predicates.STRICT(getPlayer());
	private LinkedList<LivingEntity> entities = null;
	
	@Scheduled
	Timer passive = new Timer() {

		@Override
		protected void run(int arg0) {
			if (arg0 % (WRECK.isEnabled(getGame()) ? 2 : 5) == 0) {
				chain++;
				if (chain >= 10)
					chain = 10;
			}
			ac.update("§d사슬 카운터: " + chain);
		}

	};
	
	private final Timer skill = new Timer(chain) {
		Map<LivingEntity, Location> stun = new HashMap<>();
		
		public void onStart() {
			for (LivingEntity entity : entities) {
				stun.put(entity, entity.getLocation().clone().add(0, chain/2 + 4, 0));
			}
		}

		@Override
		public void run(int count) {
			if (entities != null) {
				if (!entities.isEmpty()) {
					for (LivingEntity entity : entities) {
						entity.teleport(stun.get(entity));
					}
					if (count % 3 == 0) {
						LivingEntity e = entities.remove();
						getPlayer().teleport(e);
						e.damage(chain, getPlayer());
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
						SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
						stun.remove(e);
					}
				} else {
					stop(false);
				}
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1);

	public AirDisintegration(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && chain > 0) {
			this.entities = new LinkedList<>(LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), range.getValue(), range.getValue(), STRICT_PREDICATE));
			if (entities.size() > 0) {
				skill.start();
				return true;
			} else {
				getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&f" + range.getValue() + "칸 이내에 &a엔티티&f가 존재하지 않습니다."));
			}
		}
		return false;
	}

}
