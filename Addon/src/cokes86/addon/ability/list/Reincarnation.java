package cokes86.addon.ability.list;

import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.ParticleLib.RGB;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.event.entity.EntityRegainHealthEvent;

@AbilityManifest(name = "리인카네이션", rank = Rank.S, species = Species.OTHERS, explain = {
		"자신이 죽을 위기에 처했을 때, 이를 무시하고 체력이 1로 고정됩니다. $[cooldown]",
		"지속시간 $[duration]동안 상대방에게 주는 대미지가 $[damage]% 증가합니다.",
		"지속시간동안 상대방에게 총 $[hit]번 공격에 성공했을 경우",
		"지속시간이 종료 시 체력이 20이 되어 부활합니다.",
		"그러지 못하였을 경우 사망합니다.",
		"※능력 아이디어: Sato207" })
public class Reincarnation extends AbilityBase {
	ActionbarChannel ac = newActionbarChannel();

	public static Config<Integer> duration = new Config<Integer>(Reincarnation.class, "지속시간", 20, 2) {

		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}

	}, cooldown = new Config<Integer>(Reincarnation.class, "쿨타임", 900, 1) {

		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}

	}, damage = new Config<Integer>(Reincarnation.class, "추가대미지(%)", 50) {

		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}

	}, hit = new Config<Integer>(Reincarnation.class, "타격횟수", 10) {

		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}

	};

	int hitted = 0;

	public Reincarnation(Participant arg0) {
		super(arg0);
		reincarnation.register();
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (reincarnation.isRunning())
				e.setCancelled(true);
			else if (!reincarnation.isRunning() && getPlayer().getHealth() - e.getFinalDamage() <= 0
					&& !cool.isRunning() && !e.isCancelled()) {
				e.setDamage(0);
				getPlayer().setHealth(1);
				reincarnation.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}

	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && reincarnation.isRunning()) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		Entity damager = e.getDamager();
		if (damager instanceof Projectile) {
			Projectile projectile = (Projectile) damager;
			if (projectile.getShooter() instanceof Entity) {
				damager = (Entity) projectile.getShooter();
			}
		}

		if (e.getEntity() instanceof Player && damager.equals(getPlayer())) {
			Player target = (Player) e.getEntity();
			if (reincarnation.isRunning() && getGame().isParticipating(target) && !e.isCancelled()) {
				hitted += 1;
				e.setDamage(e.getDamage() * (1 + damage.getValue() / 100.0D));
				if (hitted == hit.getValue()) {
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
				} else if (hitted == hit.getValue() / 2) {
					SoundLib.PIANO.playInstrument(getPlayer(), new Note(2, Note.Tone.A, false));
				} else if (hitted == hit.getValue() / 5) {
					SoundLib.PIANO.playInstrument(getPlayer(), new Note(1, Note.Tone.E, false));
				} else if (hitted == hit.getValue() / 10) {
					SoundLib.PIANO.playInstrument(getPlayer(), new Note(1, Note.Tone.C, false));
				}
			}
		}
	}

	@SubscribeEvent(priority = 6)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	AbilityTimer reincarnation = new AbilityTimer(duration.getValue() * 20) {

		protected void onStart() {
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer());
		}

		@Override
		protected void run(int arg0) {
			getPlayer().setHealth(1);
			ac.update("지속 시간: " + TimeUtil.parseTimeAsString(getFixedCount()) + " 히트횟수: "
					+ (hitted >= hit.getValue() ? "§a" + hitted : hitted) + "/ " + hit.getValue());

			if (arg0 % 5 == 0) {
				for (Location l : Circle.iteratorOf(getPlayer().getLocation(), 3, 3 * 6).iterable()) {
					l.setY(LocationUtil.getFloorYAt(l.getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.1);
					ParticleLib.REDSTONE.spawnParticle(l, new RGB(140, 2, 120));

					l.setY(LocationUtil.getFloorYAt(l.getWorld(), l.getY(), l.getBlockX(), l.getBlockZ()) + 0.8);
					ParticleLib.REDSTONE.spawnParticle(l, new RGB(140, 2, 120));
				}
			}
		}

		@Override
		protected void onEnd() {
			if (hitted >= hit.getValue()) {
				getPlayer().setHealth(20.00);
			} else {
				getPlayer().setHealth(0);
			}
			hitted = 0;
			cool.start();
			ac.update(null);
		}
	};
	Cooldown cool = new Cooldown(cooldown.getValue());
}
