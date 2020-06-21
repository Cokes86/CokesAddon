package cokes86.addon.ability.list;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name="크리퍼조종사",
		rank=AbilityManifest.Rank.S,
		species=AbilityManifest.Species.HUMAN,
		explain= {
				"자신은 $[life]의 크리퍼를 조종합니다.",
				"철괴 우클릭 시 자신 위치에 충전된 크리퍼를 터트리며 공격합니다. $[cool]",
				"자신은 폭발공격을 받지 않으며, 크리퍼를 모두 소진 시 사망합니다.",
				"자신이 공격받을 때 고정 $[damage]대미지를 추가로 받으며",
				"자신이 사망할 위기에 처할 때 크리퍼 하나를 소모하며 부활합니다."
		}
)
public class CreeperController extends AbilityBase implements ActiveHandler {
	Creeper creeper;
	private static final Config<Integer> life = new Config<Integer>(CreeperController.class, "크리퍼수", 3) {

		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
		
	}, damage = new Config<Integer>(CreeperController.class, "추가고정대미지", 2) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	}, cool = new Config<Integer>(CreeperController.class, "쿨타임", 30, 1) {
		public boolean Condition(Integer value) {
			return value > 0;
		}
	};
	
	int lifes = life.getValue();
	ActionbarChannel channel = newActionbarChannel();
	CooldownTimer c = new CooldownTimer(cool.getValue());

	public CreeperController(Participant arg0) {
		super(arg0);
	}
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			t.start();
			break;
		default:
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onAbilityRestrictionClear(AbilityRestrictionClearEvent e) {
		t.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	Timer t = new Timer() {

		@Override
		protected void run(int arg0) {
			
			channel.update("크리퍼 : "+lifes+"마리");
			
			if (lifes == 0) {
				getPlayer().setHealth(0.0);
				t.stop(false);
			}
		}
		
	};

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK)) {
			if (lifes > 0 && !c.isCooldown()) {
				lifes -= 1;
				getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 6F, false);
				
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) e.setCancelled(true);
			else {
				if (getPlayer().getHealth()-e.getFinalDamage()-damage.getValue() <= 0 && lifes > 0) {
					e.setDamage(0);
					getPlayer().setHealth(20.0);
					lifes -= 1;
					SoundLib.ENTITY_CREEPER_DEATH.playSound(LocationUtil.getNearbyPlayers(getPlayer(), 10, 10));
				} else {
					getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth()-e.getFinalDamage()-damage.getValue()));
					e.setDamage(0);
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}
