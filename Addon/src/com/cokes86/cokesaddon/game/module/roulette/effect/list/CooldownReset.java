package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.CokesAddon;
import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.ability.event.AbilityCooldownResetEvent;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.addon.AddonLoader;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantEvent;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;

@RouletteManifest(name = "쿨타임 초기화", defaultPriority = 3)
public class CooldownReset extends RouletteSingleEffect{

	@Override
	protected void effect(Participant participant) {
		final AbilityBase ability = participant.getAbility();
		if (ability != null) {
			for (GameTimer timer : ability.getTimers()) {
				if (timer instanceof Cooldown.CooldownTimer) {
					timer.stop(false);
				}
			}
			Bukkit.getPluginManager().callEvent(new AbilityCooldownResetEvent(participant.getAbility()));
			if (CokesAddon.isLoadAddon("RainStarAddon")) {
				Addon rainstar = AddonLoader.getAddon("RainStarAddon");
				if (CokesAddon.getVersionCheck(rainstar.getDescription().getVersion(), "1.8.1")) {
					try {
						Bukkit.getPluginManager().callEvent(Class.forName("rainstar.abilitywar.system.event.ChronosCooldownResetEvent").asSubclass(ParticipantEvent.class).getConstructor(Participant.class).newInstance(participant));
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
			Bukkit.broadcastMessage("[룰렛] §b"+participant.getPlayer().getName()+"§f의 쿨타임이 §e초기화§f됩니다.");
		}
	}
}
