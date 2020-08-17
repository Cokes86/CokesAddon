package cokes86.addon.ability.list;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.object.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "룬", rank = Rank.A, species = Species.HUMAN, explain = {
		"철괴 우클릭시 자신 주위 $[range]블럭 이내 랜덤한 1명에게 1의 고정데미지를 줍니다.", "이 행위는 0.25초 간격으로 $[damage]번 반복합니다. $[cool]",
		"※제작자 자캐 기반 능력자" })
public class Rune extends CokesAbility implements ActiveHandler {
	public static Config<Integer> damage = new Config<Integer>(Rune.class, "반복횟수", 7) {
		@Override
		public boolean condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Rune.class, "쿨타임", 60, 1) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, range = new Config<Integer>(Rune.class, "범위", 5) {
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

	Cooldown c = new Cooldown(cool.getValue());
	Duration d = new Duration(damage.getValue(), c) {

		@Override
		protected void onDurationProcess(int seconds) {
			ArrayList<Player> ps = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range.getValue(), range.getValue(), predicate);
			if (ps.size() > 0) {
				int a = new Random().nextInt(ps.size());
				Player target = ps.get(a);
				if (!target.isDead() && Damages.canDamage(target, getPlayer(), DamageCause.ENTITY_ATTACK, 1)) {
					if (target.getHealth() > 1) {
						target.setHealth(target.getHealth()-1);
					} else {
						target.damage(20, getPlayer());
					}
					SoundLib.XYLOPHONE.playInstrument(getPlayer(), new Note(1, Note.Tone.C, false));
					SoundLib.XYLOPHONE.playInstrument(target, new Note(1, Note.Tone.C, false));
				}
			}

			for (Location l : Circle.iteratorOf(getPlayer().getLocation(), range.getValue(), range.getValue() * 9).iterable()) {
				l.setY(LocationUtil.getFloorYAt(l.getWorld(), getPlayer().getLocation().getY(), l.getBlockX(), l.getBlockZ()) + 0.1);
				ParticleLib.REDSTONE.spawnParticle(l, new ParticleLib.RGB(0, 179, 255));
			}
		}
	}.setPeriod(TimeUnit.TICKS, 5);

	public Rune(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown() && !d.isRunning()) {
				d.start();
				return true;
			}
		}
		return false;
	}

}
