package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.version.VersionNotSupportedException;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PhantomThiefKit {
    private static final IPhantom instance;

    static {
        try {
            instance = Class.forName("cokes86.addon.ability.list.phantomthief." + ServerVersion.getName())
                    .asSubclass(IPhantom.class).getConstructor().newInstance();
        } catch (Exception e) {
            throw new VersionNotSupportedException();
        }
    }
    public static void show(AbilityBase owner) {
    	instance.show(owner);
    }
    
    public static void hide(AbilityBase owner) {
    	instance.hide(owner);
    }
    
    public static void onPlayerJoin(AbilityBase owner, PlayerJoinEvent e) {
    	instance.onPlayerJoin(owner, e);
    }
    
    public static void onPlayerQuit(AbilityBase owner, PlayerQuitEvent e) {
    	instance.onPlayerQuit(owner, e);
    }
    
    public static IPhantom getInstance() {
    	return instance;
    }
}
