package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.function.Predicate;

@AbilityManifest(name = "불신", rank = Rank.A, species = Species.HUMAN, explain = {
		"게임 중 1회에 한해 $[range]칸 이내의 상대방을 바라본 체 철괴로 우클릭 시 불신 전용 2인 팀을 만듭니다.",
		"팀을 만든 이후 2초간 무적상태가 된 후 팀끼리는 서로 공격할 수 없습니다.",
		"팀을 맺은 상대방이 자신을 $[hit]회 타격할 시 팀이 깨지며",
		"팀을 맺었던 플레이어에게 주는 대미지가 $[damage] 증가합니다.",
		"[아이디어 제공자 §bRainStar_§f]"
})
public class Unbelief extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> hit = new Config<Integer>(Unbelief.class, "공격횟수", 5) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, damage = new Config<Integer>(Unbelief.class, "추가대미지", 2) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, range = new Config<Integer>(Unbelief.class, "우클릭범위", 10) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	private final Predicate<Entity> predicate = entity -> {
		if (entity.equals(getPlayer())) return false;
		if (entity instanceof Player) {
			if (!getGame().isParticipating(entity.getUniqueId())) return false;
			AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
			if (getGame() instanceof DeathManager.Handler) {
				DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
			}
			if (getGame() instanceof Teamable) {
				Teamable game = (Teamable) getGame();
				return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
			}
			return target.attributes().TARGETABLE.getValue();
		}
		return true;
	};
	private boolean attackable = false;
	private final ActionbarChannel notice = newActionbarChannel();
	private Participant teammate;
	private ActionbarChannel teamNotice = null;
	private int hitted = 0;
	private final AbilityTimer invTimer = new AbilityTimer(2) {};

	public Unbelief(Participant arg0) {
		super(arg0);
	}

	public void onUpdate(Update update) {
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			if (teamNotice != null) teamNotice.unregister();
		}
	}

	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getParticipant().equals(getParticipant()) || (teammate != null && e.getParticipant().equals(teammate))) {
			notice.update(null);
			if (teamNotice != null) teamNotice.unregister();
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity) {
				damager = (Entity) arrow.getShooter();
			}
		}

		if (teammate != null) {
			if (invTimer.isRunning() && (e.getEntity().equals(teammate.getPlayer()) || e.getEntity().equals(getPlayer()))) {
				e.setCancelled(true);
			}

			if (damager.equals(teammate.getPlayer()) && e.getEntity().equals(getPlayer())) {
				if (!attackable) {
					hitted += 1;
					e.setCancelled(true);
					damager.sendMessage("팀을 공격할 수 없습니다.");
					if (hitted == hit.getValue()) {
						SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(getPlayer());
						SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(teammate.getPlayer());

						getPlayer().sendMessage("팀이 깨졌습니다. 이제 당신은 " + teammate.getPlayer().getName() + "님을 믿지 못합니다.");
						teammate.getPlayer().sendMessage("팀이 깨졌습니다. 이제 공격할 수 있습니다.");
						notice.update(null);
						teamNotice.unregister();
						attackable = true;
					}
				}
			} else if (damager.equals(getPlayer()) && e.getEntity().equals(teammate.getPlayer())) {
				if (attackable) {
					e.setDamage(e.getDamage() + damage.getValue());
				} else {
					e.setCancelled(true);
					damager.sendMessage("팀을 공격할 수 없습니다.");
				}
			}
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (teammate == null && material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), range.getValue(), predicate);
			if (player != null) {
				teammate = getGame().getParticipant(player.getUniqueId());
				teamNotice = teammate.actionbar().newChannel();

				getPlayer().sendMessage("§e" + player.getName() + "§f님과 팀을 맺습니다.");
				player.sendMessage("§e" + getPlayer().getName() + "§f님과 팀을 맺습니다. 팀을 총 " + hit.getValue() + "번 공격할 시 팀이 깨집니다.");

				notice.update("§e팀 §f: " + player.getName());
				teamNotice.update("§e팀 §f: " + getPlayer().getName());

				invTimer.start();
			}
		}
		return false;
	}
}
