package com.cokes86.cokesaddon.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.cokes86.cokesaddon.game.module.roulette.wizard.RouletteMainWizard;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;

public class CokesRouletteCommand extends Command {

    @Override
    protected boolean onCommand(CommandSender sender, String command, String[] args) {
        if (sender instanceof Player) {
            new RouletteMainWizard((Player) sender, AbilityWar.getPlugin()).show();
			return true;
        }
        return false;
    }
    
}
