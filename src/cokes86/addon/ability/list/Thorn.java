package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.DamagePlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "가시",
		rank = Rank.A,
		species = Species.OTHERS,
		explain = {"철괴 우클릭시 $[du]동안 상대방의 화살 공격의 대미지를 0으로 하고,",
		"무시한 대미지의 $[damage]%만큼을 상대방에게 되돌려줍니다. $[cool]"}
)
public class Thorn extends AbilityBase implements ActiveHandler {
	private static Config<Integer> cool = new Config<Integer>(Thorn.class, "쿨타임", 20, 1) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	},
			du = new Config<Integer>(Thorn.class, "지속시간",5 ,2) {
				public boolean Condition(Integer value) {
					return value >= 0;
				}
			},
			damage = new Config<Integer>(Thorn.class, "반사대미지(%)", 20) {
				public boolean Condition(Integer value) {
					return value > 0;
				}
			};

	public Thorn(Participant participant) throws IllegalStateException {
		super(participant);
	}
	
	CooldownTimer c = new CooldownTimer(cool.getValue());
	DurationTimer d = new DurationTimer(du.getValue(), c) {
		@Override
		protected void onDurationProcess(int arg0) {
		}
	};
	

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown() && !d.isDuration()) {
				d.start();
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && d.isRunning()) {
			if (e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				if (a.getShooter() instanceof Player) {
					Player shooter = (Player) a.getShooter();
					double dam = e.getFinalDamage();
					DamagePlusUtil.penetratingDamage(dam* damage.getValue() / (float)100, shooter, getPlayer());
					SoundLib.PIANO.playInstrument(shooter, new Note(1, Tone.F, false));
					e.setDamage(0);
				}
			}
		}
	}
}
