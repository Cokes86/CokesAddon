package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.effects.GodsPressure;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "세트", rank = Rank.S, species = Species.GOD, explain = {
		"§7패시브 §8- §c전쟁의 여왕§r: 게임 중 플레이어가 사망할때마다 전쟁스택이 1씩 쌓입니다.",
		"  남아있는 생존자 대비 얻은 전쟁스택에 비례하여",
		"  자신이 상대방에게 주는 대미지가 증가합니다 (최대 $[MAX_DAMAGE] 증가)",
		"§7철괴 우클릭 §8- §c전원 집합§r: 자신 기준 $[RANGE]블럭 이내 모든 플레이어를 자신의 위치로 이동시킨 후",
		"  이동시킨 플레이어들에게 신의 프레셔 상태이상을 $[DEBUFF] 부여합니다.",
		"  이미 신의 프레셔 디버프를 보유한 플레이어는 전원 집합으로 이동하지 않습니다. $[COOL]",
		"§7상태이상 §8- §c신의 프레셔§r: 상대방에게 주는 대미지가 세트의 전쟁스택에",
	    "  반비례하여 감소합니다. 이 수치는 최소 0, 최대 $[DEBUFF_MAX]만큼 감소합니다.",
		"  세트가 아닌 플레이어가 사용할 경우, 1만큼만 감소합니다."
})
public class Seth extends CokesAbility implements ActiveHandler {
	private final List<Participant> participants = new ArrayList<>(getGame().getParticipants());
	private int kill = 0;
	private final DecimalFormat df = new DecimalFormat("0.00");
	public static final Config<Integer> MAX_DAMAGE = new Config<>(Seth.class, "추가대미지", 9, value -> value >= 0),
			COOL = new Config<>(Seth.class, "쿨타임", 60, Config.Condition.COOLDOWN),
			DEBUFF = new Config<>(Seth.class, "디버프시간", 5, Config.Condition.TIME),
			RANGE = new Config<>(Seth.class, "범위", 7, value -> value > 0),
			DEBUFF_MAX = new Config<>(Seth.class, "감소_최대치", 4, integer -> integer>0);

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
		}
		return true;
	};

	private final Cooldown cooldown = new Cooldown(COOL.getValue());
	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public Seth(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			final int range = Seth.RANGE.getValue();
			final List<Player> list = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate);
			if (list.size() > 0) {
				for (Player player : list) {
					Participant participant = getGame().getParticipant(player);
					GodsPressure.apply(participant, TimeUnit.TICKS, DEBUFF.getValue()*20, this);
				}
				cooldown.start();
				return true;
			} else {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
			}
		}
		return false;
	}

	@SubscribeEvent(priority = 99)
	private void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			kill += 1;

			if (getGame() instanceof DeathManager.Handler) {
				final DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
					participants.remove(getGame().getParticipant(e.getEntity().getUniqueId()));
				}
			}

			actionbarChannel.update(df.format((double) kill * 100 / participants.size()) + "% (" + kill + "/" + participants.size() + ")");
		}
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			final double damage = Math.min(MAX_DAMAGE.getValue(), kill * 3 / participants.size());
			e.setDamage(e.getDamage() + damage);
		}
	}
	
	public int getKill() {
		return kill;
	}
	
	public int getParticipantSize() {
		return participants.size();
	}
}
