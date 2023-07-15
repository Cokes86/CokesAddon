package com.cokes86.cokesaddon.ability.synergy.list;

import com.cokes86.cokesaddon.ability.synergy.CokesSynergy;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.list.mix.synergy.game.SynergyGame.SynergyParticipant;
import daybreak.abilitywar.game.manager.object.AbilitySelect.AbilityCollector;
import daybreak.abilitywar.utils.base.minecraft.BroadBar;
import daybreak.abilitywar.utils.base.random.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AbilityManifest(name = "연옥", rank = Rank.S, species = Species.OTHERS, explain = {
		"모든 플레이어는 죄를 씻기 위해 연옥으로 이송될지어니.",
		"철괴 우클릭시 해당 세계를 연옥으로 만들어 10초간 자신 제외 모든 플레이어의 능력이 제거되고",
		"연옥에서는 모든 플레이어는 공격을 할 수 없고 공격받을 수 없습니다.",
		"이후 지속시간이 종료될 시 모든 플레이어는 모든 죄를 씻고 능력을 재추첨받습니다."
})
public class Purgatory extends CokesSynergy implements ActiveHandler {
	private final Random random = new Random();
	private final Map<Participant, Boolean> wasSynergy = new HashMap<>();
	private final Duration timer = new Duration(10) {
		private BroadBar bar;

		protected void onDurationStart() {
			bar = new BroadBar("연옥의 시간", BarColor.WHITE, BarStyle.SOLID);
			for (Participant participant : getGame().getParticipants()) {
				if (participant.equals(getParticipant())) continue;
				if (participant.getAbility() != null) {
					if (participant.getAbility() instanceof Mix) {
						Mix mix = (Mix) participant.getAbility();
						wasSynergy.put(participant, mix.hasSynergy());
						mix.removeAbility();
					} else if (participant instanceof SynergyParticipant) {
						wasSynergy.put(participant, true);
						participant.removeAbility();
					} else {
						participant.removeAbility();
						wasSynergy.put(participant, false);
					}
				}
			}
		}

		@Override
		protected void onDurationProcess(int arg0) {
			bar.setProgress(Math.min((double) getFixedCount() / 10, 1.0));
		}

		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		protected void onDurationSilentEnd() {
			bar.unregister();
			for (Participant participant : getGame().getParticipants()) {
				try {
					if (participant instanceof AbstractMix.MixParticipant) {
						changeAbility((AbstractMix.MixParticipant) participant);
					} else if (participant instanceof SynergyParticipant) {
						participant.setAbility(getRandomSynergy());
					} else {
						participant.setAbility(getRandomAbility());
					}
				} catch (Exception ignored) {}
			}
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

	@SubscribeEvent(childs = {EntityDamageByBlockEvent.class, EntityDamageByEntityEvent.class})
	public void onEntityDamage(EntityDamageEvent e) {
		if (timer.isRunning()) {
			e.setCancelled(true);
		}
	}

	public Class<? extends AbilityBase> getRandomAbility() {
		List<Class<? extends AbilityBase>> list = AbilityCollector.EVERY_ABILITY_EXCLUDING_BLACKLISTED.collect(getGame().getClass());
		for (Participant participant : getGame().getParticipants()) {
			if (participant.getAbility() != null) list.remove(participant.getAbility().getClass());
		}
		return random.pick(list);
	}

	public AbilityFactory.AbilityRegistration getRandomSynergy() {
		List<AbilityFactory.AbilityRegistration> synergies = new ArrayList<>();

		for (AbilityFactory.AbilityRegistration synergy : SynergyFactory.getSynergies()) {
			String name = synergy.getManifest().name();
			if (!Configuration.Settings.isBlacklisted(name) && !name.equals("연옥")) {
				synergies.add(synergy);
			}
		}
		return random.pick(synergies);
	}

	public void changeAbility(AbstractMix.MixParticipant participant) {
		try {
			Mix mix = participant.getAbility();
			if (mix != null) {
				if (wasSynergy.getOrDefault(participant, false)) {
					mix.setSynergy(getRandomSynergy());
				} else {
					mix.setAbility(getRandomAbility(), getRandomAbility());
				}
			}
		} catch (Exception ignored) {

		}
	}
}
