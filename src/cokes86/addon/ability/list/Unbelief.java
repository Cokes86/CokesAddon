package cokes86.addon.ability.list;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;

@AbilityManifest(name = "불신", rank = Rank.A, species = Species.HUMAN, explain = {
		"$[cool]마다 공격 1회를 무효로 하는 방어막을 생성합니다. (최대 1개)", "방어막을 가지고 있는 동안 나약함1 디버프를 부여합니다.",
		"방어막을 가지고 있지 않는 동안 힘1 버프를 부여합니다.", "※능력 아이디어: RainStar_" })
public class Unbelief extends AbilityBase {
	public static Config<Integer> cool = new Config<Integer>(Unbelief.class, "방어막생성시간", 30, 2) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};

	ActionbarChannel shield = newActionbarChannel();
	RGB color = RGB.of(0, 0, 0);

	public Unbelief(Participant arg0) {
		super(arg0);
	}

	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ShieldOff.start();
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		ShieldOff.start();
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (ShieldOn.isRunning()) {
				e.setDamage(0);
				ShieldOn.stop(false);
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	Timer ShieldOff = new Timer(cool.getValue()) {
		@Override
		protected void onEnd() {
			shield.update("보호막 생성!");
			getPlayer().sendMessage("보호막이 생성되었습니다.");
			getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			ShieldOn.start();
		}
		
		protected void onSilentEnd() {
			getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
		}

		@Override
		protected void run(int arg0) {
			shield.update("보호막 생성중 (남은 시간: " + TimeUtil.parseTimeAsString(getFixedCount())+")");
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0));
		}
	}, ShieldOn = new Timer() {
		@Override
		protected void run(int arg0) {
			for (Location l : Circle.iteratorOf(getPlayer().getLocation(), 1, 20).iterable()) {
				for (int a= 0; a<5;a++) {
					l.setY(LocationUtil.getFloorYAt(l.getWorld(), getPlayer().getLocation().getY(), l.getBlockX(), l.getBlockZ()) + (0.1+0.2*a));
					ParticleLib.REDSTONE.spawnParticle(l, color);
				}

				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0));
			}
		}

		@Override
		protected void onEnd() {
			getPlayer().removePotionEffect(PotionEffectType.WEAKNESS);
			ShieldOff.start();
		}
		
		@Override
		protected void onSilentEnd() {
			getPlayer().removePotionEffect(PotionEffectType.WEAKNESS);
		}
	}.setPeriod(TimeUnit.TICKS, 5);
}
