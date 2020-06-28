package cokes86.addon.ability.list;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.DamageUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "아리스", rank = Rank.B, species = Species.HUMAN, explain = {
		"5초마다 §d사슬 카운터§f를 1씩 상승하며 최대 10만큼 상승합니다.",
		"철괴 우클릭시 $[range]블럭 이내 모든 플레이어를 §d사슬 카운터§f만큼 공중에 고정시킵니다. $[cool]",
		"공중에는 (§d사슬 카운터§f/2 + 4)블럭만큼 위로 고정시킵니다.",
		"공중에 고정되어있는 플레이어는 그동안 움직일 수 없으며,",
		"매 2초마다 1의 고정대미지를 입습니다. 그 이외의 대미지는 받지 않습니다.",
		"지속시간과 쿨타임동안 §d사슬 카운터§f는 증가하지 않습니다." })
public class Aris extends AbilityBase implements ActiveHandler {
	public static final Config<Integer> range = new Config<Integer>(Aris.class, "범위", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Aris.class, "쿨타임", 10,1) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	int chain = 0;
	ActionbarChannel ac = newActionbarChannel();
	Timer passive = new Timer() {

		@Override
		protected void run(int arg0) {
			if (!c.isRunning()) {
				if (arg0 % (WRECK.isEnabled(getGame()) ? 2 : 5) == 0) {
					chain++;
					if (chain >= 10)
						chain = 10;
				}
				ac.update("§d사슬 카운터: " + chain);
			} else {
				ac.update(null);
			}
		}
	};

	public void onUpdate(Update update) {
		switch (update) {
		case RESTRICTION_CLEAR:
			passive.start();
		default:
		}
	}

	HashMap<Player, Location> hold = new HashMap<>();
	HashMap<Player, ActionbarChannel> holdAc = new HashMap<>();

	DurationTimer d = new ChainTimer();
	CooldownTimer c = new CooldownTimer(cool.getValue());

	public Aris(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if ((!d.isDuration() || d == null) && chain != 0) {
				if (LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(), range.getValue()).isEmpty()) {
					getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
				} else {
					this.d = new ChainTimer();
					d.start();
					return true;
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player t = (Player) e.getEntity();
			if (hold.containsKey(t)) {
				e.setCancelled(true);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			Player t = (Player) e.getEntity();
			if (hold.containsKey(t) && e.getDamager().equals(getPlayer())) {
				return;
			}
		}
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	class ChainTimer extends DurationTimer {

		public ChainTimer() {
			super(Aris.this.chain*20);
			this.setPeriod(TimeUnit.TICKS, 1);
		}
		
		@Override
		protected void onDurationStart() {
			passive.stop(false);
			ac.update(null);
			for (Player p : LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(),
					range.getValue())) {
				hold.put(p, p.getLocation().clone().add(0, chain/2+4, 0));
				holdAc.put(p, getGame().getParticipant(p).actionbar().newChannel());
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(p);
			}

		}

		@Override
		protected void onDurationProcess(int seconds) {
			for (Player p : hold.keySet()) {
				p.teleport(hold.get(p));
				holdAc.get(p).update("고정 지속시간: " + TimeUtil.parseTimeAsString(getFixedCount()));
				if (seconds % 40 == 0
						&& DamageUtil.canDamage(getPlayer(), p, DamageCause.ENTITY_ATTACK, 1)) {
					p.setHealth(Math.max(0.0, p.getHealth() - 1));
				}
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			hold.clear();
			for (Player p : holdAc.keySet()) {
				holdAc.get(p).unregister();
			}
			holdAc.clear();
			chain = 0;
			c.start();
		}
	}
}
