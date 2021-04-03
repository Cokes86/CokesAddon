package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.CokesUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AbilityManifest(name = "가시", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 우클릭시 $[DURATION]동안 상대방의 화살 공격의 대미지를 0으로 하고,", "무시한 대미지의 $[DAMAGE]%만큼을 상대방에게 되돌려줍니다. $[COOL]"})
public class Thorn extends CokesAbility implements ActiveHandler {
	private static final Config<Integer> COOL = new Config<Integer>(Thorn.class, "쿨타임", 20, Config.Condition.COOLDOWN) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, DURATION = new Config<Integer>(Thorn.class, "지속시간", 5, Config.Condition.TIME) {
		public boolean condition(Integer value) {
			return value >= 0;
		}
	}, DAMAGE = new Config<Integer>(Thorn.class, "반사대미지(%)", 40) {
		public boolean condition(Integer value) {
			return value > 0;
		}
	};
	private final Cooldown cooldown = new Cooldown(COOL.getValue());
	private final Duration duration = new Duration(DURATION.getValue(), cooldown) {
		@Override
		protected void onDurationProcess(int arg0) {}
	};

	public Thorn(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !duration.isDuration()) {
			duration.start();
			return true;
		}
		return false;
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && duration.isRunning()) {
			if (CokesUtil.isInstanceOfArrow(e.getDamager())) {
				Projectile a = (Projectile) e.getDamager();
				if (a.getShooter() instanceof Player && !a.getShooter().equals(getPlayer())) {
					Player shooter = (Player) a.getShooter();
					double dam = e.getDamage();
					Damages.damageArrow(shooter, getPlayer(), (float) (dam * DAMAGE.getValue() / 100));
					SoundLib.PIANO.playInstrument(shooter, new Note(1, Tone.F, false));
					e.setDamage(0);
				}
			}
		}
	}
}
