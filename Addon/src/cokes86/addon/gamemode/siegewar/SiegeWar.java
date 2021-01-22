package cokes86.addon.gamemode.siegewar;

import daybreak.abilitywar.game.team.TeamGame;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;

import java.util.*;

public class SiegeWar extends TeamGame{
    protected SiegeWar(String[] args) {
        super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS(), args);
    }

    @Override
    protected void progressGame(int i) {

    }
}
