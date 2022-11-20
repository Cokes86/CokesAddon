package com.cokes86.cokesaddon.command;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.math.NumberUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CokesCommand {
    Command cokesMain;

    public CokesCommand() {
        cokesMain = new Command() {
            @Override
            protected boolean onCommand(CommandSender sender, String command, String[] args) {
                if (args.length > 0) {
                    if (NumberUtil.isInt(args[0])) {
                        sendCommandHelp(sender, command, Integer.parseInt(args[0]));
                    } else {
                        Messager.sendErrorMessage(sender, "존재하지 않는 페이지입니다.");
                    }
                } else {
                    sendCommandHelp(sender, command, 1);
                }
                return true;
            }

            private void sendCommandHelp(CommandSender sender, String command, int page) {
                if (page == 1) {
                    sender.sendMessage(new String[]{
                            Formatter.formatTitle(ChatColor.GOLD, ChatColor.YELLOW, "능력자 전쟁 코크스 애드온"),
                            Formatter.formatCommand(command, "cokes abi [능력]", "자신에게 [능력] 능력을 부여합니다. [능력] 중에는 코크스 애드온의 개발 예정 능력도 포함됩니다.", true)});
                } else {
                    Messager.sendErrorMessage(sender, "존재하지 않는 페이지입니다.");
                }
            }
        };
        cokesMain.addSubCommand("abi", new CokesAbiCommand());
        AbilityWar.getPlugin().getCommands().getMainCommand().addSubCommand("cokes", cokesMain);
    }
}
