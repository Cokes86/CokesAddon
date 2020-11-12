package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PhantomMathod {
    private final IPhantom instance;

    public PhantomMathod() {
        try {
        	instance = Class.forName("cokes86.addon.ability.list.phantomthief." + ServerVersion.getName())
        			.asSubclass(IPhantom.class).getConstructor().newInstance();
        } catch (Exception e) {
        	throw new VersionNotSupportedException();
        }
    }

    public void show(AbilityBase owner) {
    	instance.show(owner);
    }
    
    public void hide(AbilityBase owner) {
    	instance.hide(owner);
    }
    
    public void onPlayerJoin(AbilityBase owner, PlayerJoinEvent e) {
    	instance.onPlayerJoin(owner, e);
    }
    
    public void onPlayerQuit(AbilityBase owner, PlayerQuitEvent e) {
    	instance.onPlayerQuit(owner, e);
    }
    
    public IPhantom getInstance() {
    	return instance;
    }
}
