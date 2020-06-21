package cokes86.addon.gamemodes.boomchallenge;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "폭탄", rank = Rank.SPECIAL, species = Species.SPECIAL)
public class BoomAbility extends AbilityBase implements TargetHandler {

	public BoomAbility(Participant arg0) throws IllegalStateException {
		super(arg0);
	}

	@Override
	public void TargetSkill(Material arg0, LivingEntity arg1) {
		if (arg0.equals(Material.IRON_INGOT)) {
			if (arg1 instanceof Player) {
				Player target = (Player) arg1;
				if (getGame().getClass().isAssignableFrom(BoomChallenge.class)) {
					BoomChallenge game = (BoomChallenge) getGame();
					if (game.isParticipating(target)) {
						Random r = new Random();
						int get = r.nextInt(100);
						SoundLib.ENTITY_GENERIC_EXPLODE.broadcastSound();
						if (get < 50) {
							getPlayer().setHealth(0.0);
						} else {
							target.setHealth(0.0);
						}
					}
				}
			}
		}
	}

}
