package cokes86.addon.ability.list;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import cokes86.addon.ability.Test;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.library.ParticleLib;

@AbilityManifest(name = "하모니", rank = Rank.C, species = Species.HUMAN, explain= {
		"5초마다 주변 10블럭 이내의 플레이어의 수/2 만큼 체력을 회복하고,",
		"그 주변 플레이어 역시 0.5의 체력을 증가시켜줍니다."
})
@Test
public class Harmony extends AbilityBase {

	public Harmony(Participant arg0) {
		super(arg0);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onAbilityRestrictionClear(AbilityRestrictionClearEvent e) {
		passive.start();
	}
	
	protected void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			passive.start();
			break;
		default:
		}
	}
	
	Timer passive = new Timer() {	
		@Override
		protected void run(int arg0) {
			int a = 0;
			for (Player p : LocationUtil.getNearbyPlayers(getPlayer(), 10, 10)) {
				a++;
				p.setHealth(Math.min(p.getHealth()+0.5, getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
				for (Location l : Line.iteratorBetween(getPlayer().getLocation(), p.getLocation(), 10).iterable()) {
					ParticleLib.HEART.spawnParticle(l);
				}
			}
			getPlayer().setHealth(Math.min(getPlayer().getHealth()+a/2, getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
		}
	}.setPeriod(TimeUnit.SECONDS, 5);
}
