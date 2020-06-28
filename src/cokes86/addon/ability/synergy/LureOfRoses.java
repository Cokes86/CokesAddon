package cokes86.addon.ability.synergy;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cokes86.addon.configuration.synergy.Config;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;

@AbilityManifest(name="장미의 유혹", rank = Rank.S, species= Species.OTHERS, explain= {
		"상대방을 공격할 시 가시 카운터를 1씩 올리고,",
		"카운터*0.1의 추가데미지를 줍니다. (최대 5데미지)",
		"또한 모든 받는 데미지가 (카운터)%만큼 증가합니다. (최대 50%)",
		"쿨타임이 아닐 때 자신이 죽을 위기에 처할 시 모든 카운터를 소비하고",
		"반경 10블럭 이내 모든 플레이어에게 소비한 카운터*0.5초의",
		"블라인드 효과를 준 후 지속시간동안 자신은 무적상태가 되며,",
		"0.5초마다 채력을 1씩 회복합니다. $[cool]"
})
public class LureOfRoses extends Synergy {
	int counter=0;
	
	SettingObject<Integer> cool = new Config<Integer>(LureOfRoses.class, "쿨타임", 300, 1) {

		@Override
		public boolean condition(Integer arg0) {
			return arg0>=0;
		}
		
	};

	public LureOfRoses(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent(onlyRelevant=true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1+ Math.min(counter, 50.0)/100));
			if (duration.isRunning()) {e.setCancelled(true);}
			else if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				e.setDamage(0);
				duration.setPeriod(TimeUnit.TICKS, 10).start();
				counter = 0;
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant=true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		Entity attacker = e.getDamager();
		if (attacker instanceof Projectile) attacker = (Entity) ((Projectile) attacker).getShooter();
		if (attacker.equals(getPlayer()) && e.getEntity() instanceof Player && getGame().getParticipant((Player) e.getEntity()) != null) {
			counter += 1;
			e.setDamage(e.getDamage()+counter*0.1);
		}
	}
	
	@SubscribeEvent(onlyRelevant=true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	CooldownTimer cooldown = new CooldownTimer(300);
	DurationTimer duration = new DurationTimer(counter, cooldown) {

		@Override
		protected void onDurationProcess(int arg0) {
			for (Player p : LocationUtil.getNearbyPlayers(getPlayer(), 10, 10)) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
			}
			getPlayer().setHealth(getPlayer().getHealth()+1);
		}
		
	};
}
