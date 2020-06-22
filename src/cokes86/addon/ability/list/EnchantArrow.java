package cokes86.addon.ability.list;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "인챈트 애로우",
		rank = Rank.S,
		species = Species.HUMAN,
		explain = {
				"활로 플레이어를 적중할 시 인챈트 스택이 1씩 상승하며,",
				"스택당 추가 $[damage]%의 대미지를 입힙니다. (최대 7회)",
				"적중에 실패할 시 인챈트 스택이 0이 됩니다.",
				"인챈트 스택이 0인 상태로 적중에 실패할 시 고정 2의 대미지를 입습니다.",
				"자신이 쏜 화살은 명중 시 바로 사라집니다.",
				"※능력 아이디어: RainStar_"
})
public class EnchantArrow extends AbilityBase {
	int enchantStack = 0;
	ActionbarChannel ac = newActionbarChannel();
	public static Config<Integer> damage = new Config<Integer>(EnchantArrow.class, "추가대미지(%)", 15) {

		@Override
		public boolean Condition(Integer value) {
			return value > 0;
		}
		
	};

	public EnchantArrow(Participant participant) {
		super(participant);
	}
	
	public void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			ac.update("인챈트 스택: "+enchantStack);
		default:
		}
	}
	
	@SubscribeEvent(priority = 5)
	private void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity().getShooter().equals(getPlayer())) {
			if (e.getHitEntity() == null) {
				SoundLib.ENTITY_VILLAGER_NO.playSound(getPlayer());
				if (enchantStack == 0) {
					getPlayer().setHealth(Math.max(0.0, getPlayer().getHealth()-2));
				} else {
					enchantStack = 0;
				}
			}
			e.getEntity().remove();
			ac.update("인챈트 스택: "+enchantStack);
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter().equals(getPlayer())) {
				e.setDamage(e.getDamage()*(1+enchantStack*damage.getValue()/100.0));
				enchantStack++;
				ac.update("인챈트 스택: "+enchantStack);
				arrow.remove();
			}
		}
	}
}
