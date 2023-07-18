package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

@AbilityManifest(name = "엑시즈", rank = Rank.S, species = Species.HUMAN, explain = {
		"게임 중 1회에 한하여 철괴 우클릭 시",
		"주변 $[RANGE]블럭 이내 플레이어는 자신에게 이동합니다.",
		"이후 자신이 사망할 때 까지 이동된 플레이어는",
		"$[DOT_DAMAGE_PERIOD]초마다 $[DOT_DAMAGE]의 관통대미지를 받는",
		"§5저주§f를 부여받습니다."
})
@Lite
public class Xyz extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> RANGE = Config.of(Xyz.class, "range", 10, FunctionalInterfaces.positive(),
			"능력 대상 범위",
			"기본값: 10 (블럭)");
	private static final Config<Integer> DOT_DAMAGE_PERIOD = Config.of(Xyz.class, "dot-damage-period", 50, FunctionalInterfaces.positive(), FunctionalInterfaces.tickToSecond());
	private static final Config<Double> DOT_DAMAGE = Config.of(Xyz.class, "dot-damage", 2.5, FunctionalInterfaces.positive());
	private final Predicate<Entity> predicate = entity -> {
		if (entity == null || entity.equals(getPlayer())) return false;
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
		return false;
	};
	private boolean use = false;

	public Xyz(Participant arg0) throws IllegalStateException {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !use) {
			List<Player> targets = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), RANGE.getValue(), RANGE.getValue(), predicate);
			if (targets.size() != 0) {
				for (Player player : targets) {
					player.teleport(getPlayer().getLocation());
					new XyzTimer(getGame().getParticipant(player));
				}
				use = !use;
				return true;
			} else {
				getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
			}
		}
		return false;
	}

	private class XyzTimer extends AbilityTimer implements Listener {
		private final Participant participant;
		private final ActionbarChannel channel;
		public XyzTimer(Participant participant) {
			super();
			this.participant = participant;
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			this.setPeriod(TimeUnit.TICKS, 1);
			this.channel = participant.actionbar().newChannel();
			start();
		}

		@Override
		protected void onStart() {
			participant.getPlayer().sendMessage("엑시즈 §e"+getPlayer().getName()+"§f이(가) 당신에게 §5저주§f를 내렸습니다.");
			participant.getPlayer().sendMessage("§5저주§f는 §e"+getPlayer().getName()+"§f의 사망 시 까지 지속됩니다.");
		}

		@Override
		protected void run(int arg0) {
			channel.update("§5저주");
			if (arg0 % DOT_DAMAGE_PERIOD.getValue() == 0) {
				Damages.damageFixed(participant.getPlayer(), getPlayer(), DOT_DAMAGE.getValue().floatValue());
			}
		}

		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
			channel.unregister();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			channel.unregister();
		}

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			if (e.getEntity().equals(getPlayer())) {
				participant.getPlayer().sendMessage("엑시즈가 사망하였습니다.");
				stop(true);
			}
		}
	}
}
