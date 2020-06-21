package cokes86.addon.gamemodes.boomcatch;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityRestrictionClearEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "폭탄술래",
		rank = AbilityManifest.Rank.SPECIAL,
		species = AbilityManifest.Species.SPECIAL)
public class Seak extends AbilityBase {

	public Seak(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			if (getGame().getClass().isAssignableFrom(BoomCatch.class)) {
				Player target = (Player) e.getEntity();
				BoomCatch gm = (BoomCatch) getGame();
				if (getGame().isParticipating(target)) {
					Participant p = getGame().getParticipant(target);
					if (!p.hasAbility() && !gm.isEliminated(p)) {
						try {
							p.setAbility(Seak.class);
							getParticipant().removeAbility();
							getPlayer().removePotionEffect(PotionEffectType.SPEED);
							Bukkit.broadcastMessage("§a" + p.getPlayer().getName() + "§f님이 §c폭탄§f을 가지게 되었습니다!");
						} catch (Exception ex) {
						}
					}
				}
			}
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onRestrictionClear(AbilityRestrictionClearEvent e) {
		booster.start();
		getPlayer().getInventory().setHelmet(new ItemStack(Material.TNT, 1));
	}

	Timer booster = new Timer() {

		@Override
		protected void run(int arg0) {
			Seak.this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
		}

	};
}
