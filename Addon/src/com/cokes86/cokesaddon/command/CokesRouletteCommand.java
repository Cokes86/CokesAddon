package com.cokes86.cokesaddon.command;

import com.cokes86.cokesaddon.game.module.roulette.Roulette;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.cokes86.cokesaddon.game.module.roulette.wizard.RouletteMainWizard;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;

public class CokesRouletteCommand extends Command {

    public CokesRouletteCommand() {
        super(Condition.OP, Condition.PLAYER);
    }

    @Override
    protected boolean onCommand(CommandSender sender, String command, String[] args) {
        if (args.length == 0) {
            help(sender, command, args);
            return true;
        } else if(args.length == 1) {
            if (args[0].equalsIgnoreCase("config")) {
                new RouletteMainWizard((Player) sender, AbilityWar.getPlugin()).show();
                return true;
            } else if (args[0].equalsIgnoreCase("start")) {
                if (GameManager.isGameRunning()) {
                    AbstractGame game = GameManager.getGame();
                    if (!game.hasModule(Roulette.class)) {
                        game.addModule(new Roulette(game));
                    }
                    Roulette roulette = game.getModule(Roulette.class);
                    if (roulette.isRunning()) {
                        Messager.sendErrorMessage(sender, "룰렛 게임모듈이 이미 진행중입니다.");
                    } else {
                        roulette.start();
                    }
                } else {
                    Messager.sendErrorMessage(sender, "게임이 진행되고 있지 않습니다.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stop")) {
                if (GameManager.isGameRunning()) {
                    AbstractGame game = GameManager.getGame();
                    if (game.hasModule(Roulette.class)) {
                        Roulette roulette = game.getModule(Roulette.class);
                        if (roulette.isRunning()) {
                            roulette.stop();
                            roulette.unregister();
                        }
                    } else {
                        Messager.sendErrorMessage(sender, "룰렛 게임모듈이 이미 종료중입니다.");
                    }
                } else {
                    Messager.sendErrorMessage(sender, "게임이 진행되고 있지 않습니다.");
                }
                return true;
            }
        } else {
            help(sender, command, args);
            return true;
        }
        return false;
    }

    private void help(CommandSender sender, String command, String[] args) {
        sender.sendMessage(new String[]{
                Formatter.formatTitle(ChatColor.GOLD, ChatColor.YELLOW, "능력자 전쟁 코크스 애드온 - 룰렛"),
                Formatter.formatCommand(command + " cokes roulette", "config", "룰렛에 대한 설정을 엽니다.", true),
                Formatter.formatCommand(command + " cokes roulette", "start", "룰렛 모듈을 강제로 실행시킵니다.", true),
                Formatter.formatCommand(command + " cokes roulette", "stop", "룰렛 모듈을 강제로 종료시킵니다.", true)
        });
    }
}
