package cokes86.addon.ability.list;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.ability.Config;
import cokes86.addon.utils.DamagePlusUtil;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.minecraft.Bar;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(
		name = "신의가호",
		rank = Rank.S,
		species = Species.HUMAN,
		explain = {"철괴 우클릭 시 자신의 정체를 공개하고 $[dura]동안 대기시간을 가집니다.",
		"대기시간동안 자신은 움직일 수 없습니다.",
		"해당 대기시간이 끝날 시 자신을 제외한 모든 플레이어는 사망합니다.",
		"대기시간이 지속시간의 1/2만큼 남았을 때, 자신에게 발광효과를 부여합니다.",
		"대기시간이 지속시간의 1/4만큼 남았을 때, 모두에게 자신의 좌표를 실시간으로 공개합니다.",
		"자신이 사망하거나 대기시간이 끝날 경우 해당 능력은 비활성화됩니다."}
)
public class GodsBless extends AbilityBase implements ActiveHandler {
	public static Config<Integer> dura = new Config<Integer>(GodsBless.class, "대기시간", 4) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return TimeUtil.parseTimeAsString(getValue()*60);
		}
	};
	ActionbarChannel ac = newActionbarChannel();
	
	Timer god = new Timer(dura.getValue() * 60) {
		Bar bar = null;

		@Override
		protected void run(int Count) {
			int c = getFixedCount();
			String a = "x: " + (int)getPlayer().getLocation().getX() + " y: " + (int)getPlayer().getLocation().getY()
					+ " z: " + (int)getPlayer().getLocation().getZ();
			if (c <= dura.getValue() * 30) {
				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dura.getValue() * 30, 0));
			}
			if (c == dura.getValue() * 15) {
				Bukkit.broadcastMessage("신이 당신을 부르고 있습니다.");
				bar = new Bar("신의가호 "+getPlayer().getName()+ "의 위치 / "+a, BarColor.YELLOW, BarStyle.SEGMENTED_10);
			}
			if (c < dura.getValue() * 15) {
				bar.setTitle("신의가호 "+getPlayer().getName()+ "의 위치 "+a);
				bar.setProgress(Math.min(Count / dura.getValue() * 15.00, 1.0D));
			}
			ac.update(ChatColor.translateAlternateColorCodes('&', "&6대기 시간 &f: &e" + TimeUtil.parseTimeAsString(c)));
			
			if (c == dura.getValue() * 8 || (c <= 5 && c >= 1)) {
				Bukkit.broadcastMessage("신의가호가 신을 부르기 §c"+ TimeUtil.parseTimeAsString(c)+" §f전");
			}
		}

		@Override
		protected void onEnd() {
			for (Participant p : getGame().getParticipants()) {
				if (!p.getPlayer().equals(getPlayer())) {
					DamagePlusUtil.penetratingDamage(20, p.getPlayer(), getPlayer());
				}
			}
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (bar != null) bar.remove();
			GodsBless.this.setRestricted(true);
			PotionEffects.GLOWING.removePotionEffect(getPlayer());
		}
	};

	public GodsBless(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material mt, ClickType ct) {
		if (mt.equals(Material.IRON_INGOT) && ct.equals(ClickType.RIGHT_CLICK) && !god.isRunning()) {
			god.start();
			Bukkit.broadcastMessage("신의가호가 신을 부르기 시작했습니다.");
			Bukkit.broadcastMessage(dura.getValue()+"분 뒤 모든 플레이어가 사망합니다.");
			return true;
		}
		return false;
	}

	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (god.isRunning())
				e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (god.isRunning()) {
				Bukkit.broadcastMessage("신의가호가 신을 부르지 못하였습니다.");
				god.stop(true);
			}
		}
	}
}
