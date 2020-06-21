package cokes86.addon.ability.list;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.DamagePlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "미르",
		rank = Rank.B,
		species = Species.HUMAN,
		explain = {"철괴로 상대방을 우클릭 시 아래 3가지 중 1가지를 상대방에게 발동합니다. $[cool]",
		"◦ 상대방의 위치에 용암을 $[ac1]간 생성합니다.",
		"◦ 상대방에게 고정 $[ac2] 대미지를 줍니다.",
		"◦ 상대방에게 위더 디버프를 $[ac3]간 부여합니다.",
		"※제작자 자캐 기반 능력자"}
)
public class Mir extends AbilityBase implements TargetHandler {
	public static Config<Integer> cool = new Config<Integer>(Mir.class,"쿨타임", 20 ,1) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	},
	ac1 = new Config<Integer>(Mir.class,"용암생성시간",1,2) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	},
	ac3 = new Config<Integer>(Mir.class,"디버프시간", 7,2) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	public static Config<Double> ac2 = new Config<Double>(Mir.class, "고정대미지", 3.0) {
		@Override
		public boolean Condition(Double value) {
			return value > 0.0;
		}
	};

	Player target = null;

	CooldownTimer c = new CooldownTimer(cool.getValue());

	public Mir(Participant arg0) {
		super(arg0);
	}

	@Override
	public void TargetSkill(Material arg0, LivingEntity arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1 instanceof Player && !c.isCooldown()) {
			Random r = new Random();
			int active = r.nextInt(3);
			Player target = (Player) arg1;
			if (active == 0) {
				this.target = target;
				Lava_Attack.start();
			} else if (active == 1) {
				DamagePlusUtil.penetratingDamage(ac2.getValue(), target, getPlayer());
			} else if (active == 2) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ac3.getValue()*20, 0));
			}
			SoundLib.PIANO.playInstrument(getPlayer(), new Note(1, Note.Tone.A, false));
			c.start();
		}
	}

	Timer Lava_Attack = new Timer(ac1.getValue()) {
		Location l;
		Material m;

		@Override
		protected void onEnd() {
			l.getBlock().setType(m);
		}

		@Override
		protected void run(int arg0) {
		}

		@Override
		protected void onStart() {
			l = target.getLocation();
			m = l.getBlock().getType();
			l.getBlock().setType(Material.LAVA);
		}
	};
}
