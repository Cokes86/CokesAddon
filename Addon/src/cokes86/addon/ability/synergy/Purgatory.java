package cokes86.addon.ability.synergy;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.object.AbilitySelect.AbilityCollector;
import daybreak.abilitywar.utils.base.minecraft.Bar;

@AbilityManifest(name="연옥", rank = Rank.S, species = Species.OTHERS, explain= {
		"모든 플레이어는 죄를 씻기 위해 연옥으로 이송될지어니.",
		"철괴 우클릭시 해당 세계를 연옥으로 만들어 10초간 자신 제외 모든 플레이어의 능력이 제거되고",
		"연옥에서는 모든 플레이어는 공격을 할 수 없고 공격받을 수 없습니다.",
		"이후 지속시간이 종료될 시 모든 플레이어는 모든 죄를 씻고 능력을 재추첨받습니다."
})
public class Purgatory extends Synergy implements ActiveHandler {
	Duration timer = new Duration(10) {
		Bar bar;
		
		protected void onDurationStart() {
			bar = new Bar("연옥의 시간", BarColor.WHITE, BarStyle.SOLID);
			for (Participant participant : getGame().getParticipants()) {
				if (participant.equals(getParticipant())) continue;
				if (participant.hasAbility()) {
					if (participant.getAbility().getClass() == Mix.class) {
						Mix mix = (Mix) participant.getAbility();
						mix.removeAbility();
					} else {
						participant.removeAbility();
					}
				}
			}
		}
		
		@Override
		protected void onDurationProcess(int arg0) {
			bar.setProgress(Math.min((double)getFixedCount() / 10, 1.0));
		}
		
		protected void onDurationEnd() {
			bar.remove();
			for (Participant participant : getGame().getParticipants()) {
				try {
					if (participant.getAbility().getClass() == Mix.class) {
						Mix mix = (Mix) participant.getAbility();
						mix.setAbility(getRandomAbility(), getRandomAbility());
					} else {
						participant.setAbility(getRandomAbility());
					}
					participant.getPlayer().sendMessage("당신의 죄는 말끔히 사라졌습니다. 새로운 능력이 부여되었습니다. §a/aw check");
				} catch (Exception ignored) {
					
				}
			}
		}
		
		protected void onDurationSilentEnd() {
			bar.remove();
		}
	};

	public Purgatory(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material arg0, ClickType arg1) {
		if (arg0.equals(Material.IRON_INGOT) && arg1.equals(ClickType.RIGHT_CLICK) && !timer.isDuration()) {
			Bukkit.broadcastMessage("연옥이 열렸습니다. 능력이 모두 제거되었습니다.");
			timer.start();
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (timer.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	public Class<? extends AbilityBase> getRandomAbility() {
		List<Class<? extends AbilityBase>> list = AbilityCollector.EVERY_ABILITY_EXCLUDING_BLACKLISTED.collect(getGame().getClass());
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility()) list.remove(participant.getAbility().getClass());
		}
		Random random = new Random();
		return list.get(random.nextInt(list.size()));
	}
}
