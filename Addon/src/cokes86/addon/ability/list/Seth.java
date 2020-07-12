package cokes86.addon.ability.list;

import java.text.DecimalFormat;
import java.util.ArrayList;

import cokes86.addon.utils.LocationPlusUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "세트", rank = Rank.S, species = Species.GOD, explain = { "게임 중 플레이어가 사망할때마다 전쟁스택이 1씩 쌓입니다.",
		"전쟁스택이 남아있는 생존자 대비 일정 비율 이상일 시 힘버프가 주어집니다.", "철괴 우클릭시 자신 기준 7블럭 이내 모든 플레이어를 자신의 위치로 이동시킨 후",
		"나약함1 디버프를 $[debuff] 부여합니다. $[cool]", "※$[buff1]% 이상 : 힘1  $[buff2]% 이상 : 힘2  $[buff3]% 이상 : 힘3" })
public class Seth extends AbilityBase implements ActiveHandler {
	int max;
	int kill = 0;
	DecimalFormat df = new DecimalFormat("0.00");
	public static Config<Integer> buff1 = new Config<Integer>(Seth.class, "힘.1단계", 25) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, buff2 = new Config<Integer>(Seth.class, "힘.2단계", 75) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, buff3 = new Config<Integer>(Seth.class, "힘.3단계", 150) {
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	}, cool = new Config<Integer>(Seth.class, "쿨타임", 60, 1) {
		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, debuff = new Config<Integer>(Seth.class, "디버프시간", 10, 2) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 1;
		}
	};

	static {
		if (!((buff1.getValue() < buff2.getValue()) && (buff2.getValue() < buff3.getValue()))) {
			buff1.setValue(buff1.getDefaultValue());
			buff2.setValue(buff2.getDefaultValue());
			buff3.setValue(buff3.getDefaultValue());
		}
	}

	CooldownTimer c = new CooldownTimer(cool.getValue());

	ActionbarChannel ac = newActionbarChannel();

	Timer Passive = new Timer() {
		@Override
		protected void run(int arg0) {
			max = getGame().getParticipants().size();
			ac.update(df.format((double) kill * 100 / max) + "% (" + kill + "/" + max + ")");

			if (kill * 100 / max >= buff1.getValue() && kill * 100 / max < buff2.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0));
			} else if (kill * 100 / max >= buff2.getValue() && kill * 100 / max < buff3.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 1));
			} else if (kill * 100 / max >= buff3.getValue()) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 2));
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	public Seth(Participant arg0) {
		super(arg0);
	}
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			Passive.start();
		}
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!c.isCooldown()) {
				ArrayList<Player> ps = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 7, 7, LocationPlusUtil.STRICT(getParticipant()));
				for (Player p : ps) {
					p.teleport(getPlayer().getLocation());
					p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, debuff.getValue() * 20, 0));
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(p);
				}
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
				c.start();
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			kill += 1;
		}
	}
}
