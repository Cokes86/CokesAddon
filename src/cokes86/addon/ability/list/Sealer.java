package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "봉인자",
		rank = Rank.S,
		species = Species.HUMAN,
		explain = {"철괴로 상대방을 우클릭할 시 상대방의 능력을 $[dura]간 비활성화시킵니다. $[cool]",
		"이미 비활성화되어있는 능력에겐 이 능력이 발동하지 않습니다.",
		"봉인한 능력의 등급에 따라 자신에게 각종 버프를 10초간 부여합니다."}
)
public class Sealer extends AbilityBase implements TargetHandler {
	public static Config<Integer> cool = new Config<Integer>(Sealer.class,"쿨타임",120, 1) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	},
	dura = new Config<Integer>(Sealer.class, "지속시간", 5, 2) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};
	
	Participant target = null;
	
	CooldownTimer c = new CooldownTimer(cool.getValue());
	DurationTimer t = new DurationTimer(dura.getValue(), c) {
		ActionbarChannel ac;
		
		@Override
		protected void onDurationStart() {
			target.getAbility().setRestricted(true);
			target.getPlayer().sendMessage("봉인자가 당신의 능력을 봉인했습니다.");
			SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(target.getPlayer());
			ac = target.actionbar().newChannel();
		}

		@Override
		protected void onDurationProcess(int seconds) {
			ac.update("능력 활성화까지 "+TimeUtil.parseTimeAsString(getFixedCount())+" 남음");
			target.getPlayer().sendMessage("§a능력 활성화까지 "+TimeUtil.parseTimeAsString(getFixedCount())+" 남음");
			SoundLib.BLOCK_ANVIL_PLACE.playSound(target.getPlayer());
		}

		@Override
		protected void onDurationEnd() {
			target.getAbility().setRestricted(false);
			ac.unregister();
		}
	};

	public Sealer(Participant participant) {
		super(participant);
	}

	@Override
	public void TargetSkill(Material mt, LivingEntity entity) {
		if (mt.equals(Material.IRON_INGOT) && !t.isDuration() && !c.isCooldown()) {
			if (entity instanceof Player) {
				Player p = (Player) entity;
				target = getGame().getParticipant(p);
				if (target.hasAbility() && !target.getAbility().isRestricted()) {
					t.start();
					getPlayer().sendMessage(p.getName()+"님의 능력을 봉인하였습니다.");
					target.getPlayer().sendMessage("당신의 능력이 봉인되었습니다.");
					AbilityBase ab = target.getAbility();
					if (ab.getRank().equals(Rank.C)) {
						getPlayer().sendMessage("§eC 등급 §f봉인! 나약함1 버프를 10초간 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
					} else if (ab.getRank().equals(Rank.B)) {
						getPlayer().sendMessage("§bB 등급 §f봉인! 재생1 버프를 10초간 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
					} else if (ab.getRank().equals(Rank.A)) {
						getPlayer().sendMessage("§aA 등급 §f봉인! 힘1 버프를 10초간 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 0));
					} else if (ab.getRank().equals(Rank.S)) {
						getPlayer().sendMessage("§dS 등급 §f봉인! 힘2 버프를 10초간 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1));
					} else if (ab.getRank().equals(Rank.SPECIAL)) {
						getPlayer().sendMessage("§dSPECIAL 등급 §f봉인! 힘1 버프를 10초간 부여합니다!");
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 0));
					}
				} else {
					getPlayer().sendMessage("상대방의 능력이 없거나 이미 비활성화되어있는 상태입니다.");
				}
			}
		}
	}
}
