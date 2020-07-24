package cokes86.addon.ability.list.phantomthief;

import daybreak.abilitywar.game.AbstractGame;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class PhantomMathod {
    private final PhantomThief thief;

    public PhantomMathod(PhantomThief thief) {
        this.thief = thief;
    }

    protected abstract void show();
    protected abstract void hide();
    protected abstract void injectPlayer(Player player);
    protected abstract void onPlayerJoin(PlayerJoinEvent e);
    protected abstract void onPlayerQuit(PlayerQuitEvent e);

    public AbstractGame.Participant getParticipant() {
        return thief.getParticipant();
    }

    public Player getPlayer() {
        return thief.getPlayer();
    }
}
