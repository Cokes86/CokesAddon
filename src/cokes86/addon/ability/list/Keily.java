package cokes86.addon.ability.list;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Scheduled;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.object.WRECK;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "케일리", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"$[dura]마다 스위치를 1개씩 얻으며 최대 3개까지 얻습니다.", "철괴 우클릭 시 스위치 1개를 소모해 자신의 위치에서 3F의 위력으로 폭발하고",
		"2초간 공중에 날 수 있습니다. $[cool]", "자신은 폭발공격을 받지 않습니다." })
public class Keily extends AbilityBase implements ActiveHandler {
	private static final Config<Integer> cool = new Config<Integer>(Keily.class, "쿨타임", 45, 1) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, dura = new Config<Integer>(Keily.class, "카운터생성주기", 30, 2) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	@Scheduled
	private final Timer stackAdder = new Timer() {
		@Override
		protected void run(int count) {
			if (switchCounter < 3) {
				switchCounter++;
				channel.update(ChatColor.DARK_RED.toString().concat(Strings.repeat("●", switchCounter)
						.concat(Strings.repeat("○", Math.max(3 - switchCounter, 0)))));
			}
		}
	}.setPeriod(TimeUnit.SECONDS, WRECK.isEnabled(getGame()) ? dura.getValue() / 2 : dura.getValue());

	ActionbarChannel channel = newActionbarChannel();
	CooldownTimer c = new CooldownTimer(cool.getValue());
	int switchCounter = 0;

	public Keily(Participant arg0) {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown() && switchCounter > 0) {
				getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 3F, false);
				switchCounter--;
				c.start();
				new Timer(2) {

					@Override
					protected void onStart() {
						getPlayer().setFlying(true);
					}

					@Override
					protected void run(int arg0) {

					}

					@Override
					protected void onEnd() {
						getPlayer().setFlying(false);
					}

					@Override
					protected void onSilentEnd() {
						getPlayer().setFlying(false);
					}

				}.start();
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION))
				e.setCancelled(true);
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}
