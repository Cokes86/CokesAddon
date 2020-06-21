package cokes86.addon.ability.list;

import java.util.Arrays;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.DamagePlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Scheduled;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "포커",
		rank = AbilityManifest.Rank.B,
		species = AbilityManifest.Species.HUMAN,
		explain = {"철괴 우클릭 시 1~10 사이의 숫자를 3개 뽑습니다. 이 3개의 숫자의 조합에 따라 각종 효과를 얻습니다. $[cool]",
		"탑: 아무런 조합이 되지 않는 경우. 가장 높은 수가 9 또는 10일 경우 신속1 버프를 (높은 수)초 만큼 부여합니다.",
		"§a페어§f : 2개의 숫자가 같은 경우입니다. (페어의 수 * 2)초 만큼 재생3 버프를 부여합니다.",
		"§b스트레이트§f : 3개의 숫자가 연달아 나오는 경우입니다.",
		"다음 공격은 (가장 높은 수)의 대미지를 추가로 입힙니다. (단, 9 10 1 등은 스트레이트가 아님.)",
		"§e트리플§f : 3개의 숫자가 모두 같은 경우입니다.",
		"자신을 제외한 모든 플레이어에게 (트리플의 수 * 1.5)의 관통대미지를 줍니다."}
)
public class Pocker extends AbilityBase implements ActiveHandler {
	int[] num = new int[3];
	private static Config<Integer> cool = new Config<Integer>(Pocker.class, "쿨타임", 30, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};
	int additional = 0;
	ActionbarChannel ac = newActionbarChannel();

	public Pocker(Participant participant) {
		super(participant);
	}
	
	CooldownTimer c = new CooldownTimer(cool.getValue());
	
	@Scheduled
	Timer p = new Timer() {
		@Override
		protected void run(int var1) {
			if (additional > 0) ac.update("다음 추가대미지: "+additional);
			else ac.update(null);
		}
	};

	@Override
	public boolean ActiveSkill(Material materialType, ClickType ct) {
		if (materialType.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				Random r = new Random();
				num[0] = r.nextInt(10)+1;
				num[1] = r.nextInt(10)+1;
				num[2] = r.nextInt(10)+1;
				Arrays.sort(num);
				String result = getPockerName(num);
				int number = getPockerGet(num);
				getPlayer().sendMessage("숫자를 뽑습니다 : "+num[0]+ " "+num[1]+ " "+num[2]);
				if (result.equals("Top")) {
					String str = "";
					if (num[2] == 9 || num[2] == 10) {
						str = str+ "신속 버프를 "+number+"초만큼 부여합니다.";
						getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, number*20, 0));
					}
					getPlayer().sendMessage("이런! 탑입니다! "+str);
				} else if (result.equals("Pair")) {
					getPlayer().sendMessage("좋습니다! §a페어§f입니다! 재생3 버프를 "+(number*2)+"초간 받습니다.");
					getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, number*2*20, 2));
				} else if (result.equals("Straight")) {
					getPlayer().sendMessage("와우! §b스트레이트§f입니다! 다음 공격은 추가적으로 "+(number)+"의 대미지를 줍니다.");
					additional = number;
				} else if (result.equals("Triple")) {
					getPlayer().sendMessage("완벽합니다! §e트리플§f입니다! 자신을 제외한 모든 플레이어에게 "+(number*1.5)+"만큼의 대미지를 줍니다.");
					for (Participant p : getGame().getParticipants()) {
						if (p.equals(getParticipant())) continue;
						DamagePlusUtil.penetratingDamage(number*1.5, p.getPlayer(), getPlayer());
					}
					SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
				}
				c.start();
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			if (additional > 0) {
				e.setDamage(e.getDamage()+additional);
				additional = 0;
			}
		} else if (e.getDamager() instanceof Arrow) {
			if (((Arrow) e.getDamager()).getShooter().equals(getPlayer())) {
				if (additional > 0) {
					e.setDamage(e.getDamage()+additional);
					additional = 0;
				}
			}
		}
	}
	
	public String getPockerName(int[] num) {
		int min = num[0], mid = num[1], max = num[2];
		if (min == mid && mid == max) return "Triple";
		else if (mid == max-1 && min == mid-1) return "Straight";
		else if (min == mid || mid == max || max == min) return "Pair";
		else return "Top";
	}
	
	public int getPockerGet(int[] num) {
		int min = num[0], mid = num[1], max = num[2];
		if (min == mid && mid == max) return max;
		else if (mid == max-1 && min == mid-1) return max;
		else if (min == mid || mid == max) return mid;
		else if (min == max) return max;
		else return max;
	}
}
