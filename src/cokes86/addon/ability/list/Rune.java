package cokes86.addon.ability.list;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.DamagePlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.DamageUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "룬", rank = Rank.A, species = Species.HUMAN, explain = {
		"철괴 우클릭시 자신 주위 $[range]블럭 이내 랜덤한 1명에게 1의 고정데미지를 줍니다.", "이 행위는 0.25초 간격으로 $[damage]번 반복합니다. $[cool]",
		"※제작자 자캐 기반 능력자" })
public class Rune extends AbilityBase implements ActiveHandler {
	public static Config<Integer> damage = new Config<Integer>(Rune.class, "반복횟수", 7) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(Rune.class, "쿨타임", 60, 1) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, range = new Config<Integer>(Rune.class, "범위", 5) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};

	CooldownTimer c = new CooldownTimer(cool.getValue());
	DurationTimer d = new DurationTimer(damage.getValue(), c) {

		@Override
		protected void onDurationProcess(int seconds) {
			ArrayList<Player> ps = LocationUtil.getNearbyPlayers(getPlayer(), range.getValue(), range.getValue());
			if (ps.size() > 0) {
				int a = new Random().nextInt(ps.size());
				Player target = ps.get(a);
				if (!target.isDead() && DamageUtil.canDamage(getPlayer(), target, DamageCause.ENTITY_ATTACK, 1)) {
					DamagePlusUtil.penetratingDamage(1, target, getPlayer());
					SoundLib.XYLOPHONE.playInstrument(getPlayer(), new Note(1, Note.Tone.C, false));
					SoundLib.XYLOPHONE.playInstrument(target, new Note(1, Note.Tone.C, false));
				}
			}

			for (Location l : Circle.iteratorOf(getPlayer().getLocation(), range.getValue(), range.getValue() * 9)
					.iterable()) {
				l.setY(LocationUtil.getFloorYAt(l.getWorld(), getPlayer().getLocation().getY(), l.getBlockX(),
						l.getBlockZ()) + 0.1);
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
