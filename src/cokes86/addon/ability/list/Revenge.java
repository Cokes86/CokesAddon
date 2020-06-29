package cokes86.addon.ability.list;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import cokes86.addon.configuration.ability.Config;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;

@AbilityManifest(
		name = "복수",
		rank = Rank.A,
		species = Species.HUMAN,
		explain = {"상대방을 공격할 시 최근에 플레이어에게 받았던 대미지의 $[per]% 만큼의 고정대미지를 상대방에게 추가적으로 입힙니다."}
)
public class Revenge extends AbilityBase {
	DecimalFormat df = new DecimalFormat("0.00");
	double finalDamage = 0;
	public static Config<Integer> per = new Config<Integer>(Revenge.class, "반사대미지(%)", 40) {
		@Override
		public boolean Condition(Integer value) {
			return value >= 0;
		}
	};
	
	ActionbarChannel ac = newActionbarChannel();

	public Revenge(Participant participant) {
		super(participant);
	}
	
	public void onUpdate(Update update) {
		switch(update) {
		case RESTRICTION_CLEAR:
			ac.update(ChatColor.BLUE+ "반사고정대미지 : "+df.format(finalDamage * per.getValue() / (double)100));
		default:
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			finalDamage = e.getFinalDamage();
			ac.update(ChatColor.BLUE+ "반사고정대미지 : "+df.format(finalDamage * per.getValue() / (double)100));
		}
		
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			double plus = finalDamage * per.getValue() / (double)100;
			if (plus != 0) {
				Player p = (Player) e.getEntity();
				p.setHealth(Math.max(0.0, p.getHealth()-e.getFinalDamage()-plus));
				e.setDamage(0);
			}
		}
	}
}
