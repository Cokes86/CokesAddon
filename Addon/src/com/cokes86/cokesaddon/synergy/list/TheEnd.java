package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.BroadBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@AbilityManifest(name = "디 엔드", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 우클릭시 모든 플레이어가 스폰으로 이동한 뒤",
		"자신의 좌표가 $[du]동안 공개됩니다.",
		"지속시간동안 자신은 받는 대미지가 $[reduce]% 감소하며",
		"지속시간 종료시 자신이 사망하지 않았다면 자신과 팀을 제외한",
		"모든 플레이어의 체력을 0으로 변경합니다.",
		"해당 능력은 사망하거나 지속시간 종료시 비활성화됩니다."
})
public class TheEnd extends CokesSynergy implements ActiveHandler {
	public static final Config<Integer> du = Config.of(TheEnd.class, "지속시간", 40, Config.Condition.TIME);
	public static final Config<Double> reduce = Config.of(TheEnd.class, "감소대미지(%)", 50.0, FunctionalInterfaces.between(0.0, 100.0, false));
	private final Map<Participant, ActionbarChannel> acs = new HashMap<>();
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
	private final ActionbarChannel ac = newActionbarChannel();
	private BroadBar bar;
	private final AbilityTimer timer = new AbilityTimer(du.getValue() * 20) {
		protected void onStart() {
			Location spawn = Settings.getSpawnLocation().toBukkitLocation();
			for (Participant p : getGame().getParticipants()) {
				if (predicate.test(p.getPlayer())) {
					p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, du.getValue() * 20, 0));
					p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, du.getValue() * 20, 1));
					acs.put(p, p.actionbar().newChannel());
				}
				p.getPlayer().teleport(spawn);
			}
			String a = "x: " + (int) getPlayer().getLocation().getX() + " y: " + (int) getPlayer().getLocation().getY()
					+ " z: " + (int) getPlayer().getLocation().getZ();
			bar = new BroadBar("디 엔드 " + getPlayer().getName() + " 위치 " + a, BarColor.GREEN, BarStyle.SOLID);

			getPlayer().teleport(spawn);
		}

		@Override
		protected void run(int arg0) {
			bar.setProgress(Math.min(1.0D, (double) getFixedCount() / du.getValue()));
			String a = "x: " + (int) getPlayer().getLocation().getX() + " y: " + (int) getPlayer().getLocation().getY()
					+ " z: " + (int) getPlayer().getLocation().getZ();
			bar.setTitle("디 엔드 " + getPlayer().getName() + " 위치 " + a);
			for (ActionbarChannel ac : acs.values()) {
				ac.update("§a디 엔드 타임 : " + TimeUtil.parseTimeAsString(getFixedCount()));
			}
			ac.update("§a디 엔드 타임 : " + TimeUtil.parseTimeAsString(getFixedCount()));
		}

		protected void onEnd() {
			for (Participant p : acs.keySet()) {
				p.getPlayer().setHealth(0.0);
			}
			onSilentEnd();
		}

		protected void onSilentEnd() {
			for (ActionbarChannel ac : acs.values()) {
				ac.unregister();
			}
			acs.clear();
			bar.unregister();
			ac.update(null);
			setRestricted(true);
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();

	public TheEnd(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!timer.isRunning()) {
				timer.start();
				Bukkit.broadcastMessage("디 엔드가 세상의 종말을 알립니다.");
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(getPlayer()) && timer.isRunning()) {
			setRestricted(true);
			timer.stop(true);
		}
	}

	@SubscribeEvent
	public void onCEntityDamage(CEntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && timer.isRunning()) {
			e.setDamage(e.getDamage() * (100 - reduce.getValue()) / 100);
		}
	}
}
