package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.ability.AbilityBase;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public interface IPhantom {

	void show(AbilityBase owner);
	void hide(AbilityBase owner);
    void injectPlayer(AbilityBase owner, Player player);
    void onPlayerJoin(AbilityBase owner, PlayerJoinEvent e);
    void onPlayerQuit(AbilityBase owner, PlayerQuitEvent e);
}
