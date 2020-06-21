package cokes86.addon.ability.list;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.Bar;

@AbilityManifest(
		name="엑시즈",
		rank = AbilityManifest.Rank.S,
		species = AbilityManifest.Species.DEMIGOD,
		explain = {
				"철괴 우클릭 시 게임스폰으로 이동하고 자신의 좌표가 공개됩니다.",
				"자신을 제외한 모든 플레이어가 게임스폰으로 이동한 후",
				"신속2, 힘1을 $[du] 부여합니다.",
				"단, 버프 시간동안 엑시즈를 죽이지 못하였을 시",
				"버프를 가지고 있던 플레이어는 사망합니다.",
				"해당 능력은 한 번 사용 후 비활성화됩니다.",
				"※능력 아이디어: HappyAngels"
		}
		)
public class Xyz extends AbilityBase implements ActiveHandler {
	public static Config<Integer> du = new Config<Integer>(Xyz.class,"엑시즈타임", 40, 2) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	
	boolean xyz = true;
	Map<Participant, ActionbarChannel> acs = new HashMap<>();
	Bar bar;
	ActionbarChannel ac = newActionbarChannel();

	public Xyz(Participant arg0) throws IllegalStateException {
		super(arg0);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (!timer.isRunning()) {
				timer.start();
				Bukkit.broadcastMessage("엑시즈 타임이 시작되었습니다.");
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(getPlayer()) && timer.isRunning()) {
			xyz = false;
			timer.stop(true);
		}
	}
	
	Timer timer = new Timer(du.getValue() * 20) {
		protected void onStart() {
			for (Participant p : getGame().getParticipants()) {
				if (!p.equals(getParticipant())) {
					p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, du.getValue()*20, 0));
					p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, du.getValue()*20, 1));
					acs.put(p, p.actionbar().newChannel());
					Location spawn = Settings.getSpawnLocation();
					p.getPlayer().teleport(spawn);
				}
			}
			String a = "x: " + (int)getPlayer().getLocation().getX() + " y: " + (int)getPlayer().getLocation().getY()
					+ " z: " + (int)getPlayer().getLocation().getZ();
			bar= new Bar("엑시즈 "+getPlayer().getName()+" 위치 "+a, BarColor.GREEN, BarStyle.SOLID);
			
			Location spawn = Settings.getSpawnLocation();
			getPlayer().teleport(spawn);
		}

		@Override
		protected void run(int arg0) {
			bar.setProgress(Math.min(1.0D, (double)getFixedCount()/du.getValue()));
			String a = "x: " + (int)getPlayer().getLocation().getX() + " y: " + (int)getPlayer().getLocation().getY()
					+ " z: " + (int)getPlayer().getLocation().getZ();
			bar.setTitle("엑시즈 "+getPlayer().getName()+" 위치 "+a);
			for (ActionbarChannel ac : acs.values()) {
				ac.update("§a엑시즈 타임 : "+TimeUtil.parseTimeAsString(getFixedCount()));
			}
			ac.update("§a엑시즈 타임 : "+TimeUtil.parseTimeAsString(getFixedCount()));
		}
		
		protected void onEnd() {
			if (xyz) {
				for (Participant p : acs.keySet()) {
					p.getPlayer().setHealth(0.0);
				}
			}
			onSilentEnd();
		}
		
		protected void onSilentEnd() {
			for (ActionbarChannel ac : acs.values()) {
				ac.unregister();
			}
			acs.clear();
			bar.remove();
			ac.update(null);
			setRestricted(true);
		}
	}.setPeriod(TimeUnit.TICKS, 1);
}
