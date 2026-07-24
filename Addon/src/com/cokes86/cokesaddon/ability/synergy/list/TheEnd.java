package com.cokes86.cokesaddon.ability.synergy.list;

import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.list.Xyz;
import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.CokesUtil;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Collection;

@AbilityManifest(name = "디 엔드", rank = Rank.S, species = Species.HUMAN, explain = {
		"게임 중 1회에 한하여 철괴 우클릭 시",
		"자신을 제외한 모든 플레이어는 자신에게 이동합니다.",
		"이후 자신이 사망할 때 까지 공격력이 $[DAMAGE] 상승합니다.",
		"다른 디 엔드에게는 해당 효과가 적용되지 않습니다.",
		"또한 이미 디 엔드가 1번 사용했다면 그 플레이어가 사망할 때 까지",
		"해당 효과는 사용할 수 없습니다."
})
public class TheEnd extends CokesSynergy implements ActiveHandler {
	private static final Config<Double> DAMAGE = Config.percent(Xyz.class, "damage", 0.5);
	private boolean use = false;

	public TheEnd(Participant arg0) throws IllegalStateException {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0 == Material.IRON_INGOT && arg1 == ClickType.RIGHT_CLICK && !use) {
			for (Participant participant : getGame().getParticipants()) {
				if (participant.getAbility() == null) continue;
				for (AbstractGame.GameTimer timer : participant.getAbility().getRunningTimers()) {
					if (timer instanceof XyzTimer) {
						getPlayer().sendMessage("이미 다른 디 엔드가 능력을 사용하였습니다.");
						return false;
					}
				}
			}
			for (Participant participant : getGame().getParticipants()) {
				if (participant.equals(getParticipant())) continue;
				participant.getPlayer().teleport(getPlayer().getLocation());
				participant.getPlayer().sendMessage("디 엔드 §e"+getPlayer().getName()+"§f이(가) 당신에게 §5저주§f를 내렸습니다.");
				participant.getPlayer().sendMessage("§5저주§f는 §e"+getPlayer().getName()+"§f의 사망 시 까지 지속됩니다.");
			}
			XyzTimer xyz = new XyzTimer(getGame().getParticipants());
			xyz.start();
			use = !use;
			return true;
		}
		return false;
	}

	public class XyzTimer extends AbilityTimer implements Listener {
		private final Collection<? extends Participant> participants;
		public XyzTimer(Collection<? extends Participant> participants) {
			super();
			this.participants = participants;
			Bukkit.getPluginManager().registerEvents(XyzTimer.this, AbilityWar.getPlugin());
			this.setPeriod(TimeUnit.TICKS, 1);
			this.setBehavior(RestrictionBehavior.PAUSE_RESUME);
		}

		@Override
		protected void run(int arg0) {
		}

		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			if (e.getEntity().equals(getPlayer())) {
				Bukkit.broadcastMessage("디 엔드가 사망하였습니다.");
				stop(true);
			}
		}

		@EventHandler
		public void onEntityDamageByEntity(CEntityDamageEvent e) {
			Entity damager = CokesUtil.getDamager(e.getDamager());
			Entity victim = e.getEntity();
			Participant victimParticipant = getGame().isParticipating(victim.getUniqueId()) ? getGame().getParticipant(victim.getUniqueId()) : null;

			if (victimParticipant == null) return;
			if (damager == null) return;

			if (damager.equals(getPlayer()) && participants.contains(victimParticipant)) {
				AbilityBase base = victimParticipant.getAbility();
				if (base instanceof Mix) {
					Mix mix = (Mix) base;
					if (mix.hasSynergy() && mix.getSynergy().getName().equals("디 엔드")) return;
				}
				e.setDamage(e.getDamage() * (1+DAMAGE.getValue()));
			}
		}
	}
}
