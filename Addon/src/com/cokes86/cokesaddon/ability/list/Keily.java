package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

@AbilityManifest(name = "케일리", rank = AbilityManifest.Rank.S, species = AbilityManifest.Species.HUMAN, explain = {
		"자신은 폭발공격을 받지 않습니다.",
		"$[dura]마다 스위치를 1개씩 얻으며 최대 $[max_switch]개까지 증가합니다.",
		"철괴 우클릭 시 스위치를 전부 소모해 자신의 위치에서 (소모한 스위치 * $[fuse])의 위력으로 폭발하고",
		"$[duration]간 공중에 날 수 있습니다. $[cool]",
		"또한 능력 사용 직후 1회에 한해 낙하대미지를 받지 않습니다."
})
@Lite
public class Keily extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> dura = Config.of(Keily.class, "카운터생성주기", 45, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
	private static final Config<Integer> cool = Config.of(Keily.class, "쿨타임", 45, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
	private static final Config<Integer> duration = Config.of(Keily.class, "비행지속시간", 2, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
	private static final Config<Integer> max_switch = Config.of(Keily.class, "최대_스위치", 3, FunctionalInterfaces.positive());
	private static final Config<Double> fuse = Config.of(Keily.class, "폭발_위력", 1.3, FunctionalInterfaces.positive());
	private final int count = Wreck.isEnabled(GameManager.getGame()) ? (int) ((100 - Configuration.Settings.getCooldownDecrease().getPercentage()) / 100.0 * dura.getValue()) : dura.getValue();
	private boolean falling = false;
	private final Cooldown c = new Cooldown(cool.getValue());
	private final ActionbarChannel channel = newActionbarChannel();
	private int switchCounter = 0;
	private final Duration flying = new Duration(duration.getValue(), c) {

		@Override
		protected void onDurationStart() {
			getPlayer().setAllowFlight(true);
			getPlayer().setFlying(true);
		}

		@Override
		protected void onDurationProcess(int arg0) {

		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
			getPlayer().setAllowFlight(false);
			getPlayer().setFlying(false);
			falling = true;
		}

	};
	private final AbilityTimer stackAdder = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (!c.isRunning() && !flying.isRunning()) {
				if (switchCounter < 3) {
					switchCounter++;
				}
				channel.update(ChatColor.DARK_GREEN.toString().concat(Strings.repeat("●", switchCounter).concat(Strings.repeat("○", max_switch.getValue() - switchCounter))));
			}
		}
	}.setPeriod(TimeUnit.SECONDS, count);

	public Keily(Participant arg0) {
		super(arg0);
		stackAdder.register();
	}

	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			stackAdder.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (switchCounter > 0 && !flying.isRunning() && !c.isCooldown()) {
				getPlayer().getWorld().createExplosion(getPlayer().getLocation(), switchCounter * fuse.getValue().floatValue(), false);
				switchCounter = 0;
				channel.update(ChatColor.DARK_GREEN.toString().concat(Strings.repeat("●", switchCounter).concat(Strings.repeat("○", max_switch.getValue() - switchCounter))));
				flying.start();
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION))
				e.setCancelled(true);
			else if (e.getCause().equals(DamageCause.FALL) && falling) {
				e.setCancelled(true);
				falling = false;
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			}
		}
	}

	@SubscribeEvent(priority = 9)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (flying.isRunning()) return;
		if (falling && getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
			falling = false;
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		}
	}
}
