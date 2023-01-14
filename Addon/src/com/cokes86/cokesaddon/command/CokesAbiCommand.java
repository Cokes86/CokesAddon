package com.cokes86.cokesaddon.command;

import com.cokes86.cokesaddon.ability.AddonAbilityFactory;
import daybreak.abilitywar.Command;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.Messager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CokesAbiCommand extends Command {

    @Override
    protected boolean onCommand(CommandSender sender, String command, String[] args) {
        if (GameManager.isGameRunning() && sender instanceof Player && GameManager.getGame().isParticipating((Player) sender) && args.length >= 1) {
            if (GameManager.getGame() instanceof AbstractMix) {
                final String[] names = String.join(" ", Arrays.copyOfRange(args, 0, args.length)).split(",");
                if (names.length != 2) {
                    Messager.sendErrorMessage(sender, "능력이 두 개 보다 많이 입력되었거나 적게 입력되었습니다.");
                    return false;
                }
                names[0] = names[0].trim();
                names[1] = names[1].trim();
                AbstractMix.MixParticipant participant = ((AbstractMix) GameManager.getGame()).getParticipant((Player) sender);
                try {
                    Class<? extends AbilityBase> class1 = AddonAbilityFactory.getTestAbilityByName(names[0]) != null ? AddonAbilityFactory.getTestAbilityByName(names[0]) : AbilityList.getByString(names[0]);
                    Class<? extends AbilityBase> class2 = AddonAbilityFactory.getTestAbilityByName(names[1]) != null ? AddonAbilityFactory.getTestAbilityByName(names[1]) : AbilityList.getByString(names[1]);
                    if (participant.getAbility() == null) {
                        participant.setAbility(Mix.class);
                    }
                    participant.getAbility().setAbility(class1, class2);
                    Bukkit.broadcastMessage("§e" + sender.getName() + "§a님이 §f" + sender.getName() + "§a님에게 능력을 임의로 부여하였습니다.");
                } catch (ReflectiveOperationException e) {
                    Messager.sendErrorMessage(sender, "능력 설정 도중 오류가 발생하였습니다.");
                    if (Configuration.Settings.DeveloperSettings.isEnabled()) e.printStackTrace();
                } catch (NullPointerException e) {
                    Messager.sendErrorMessage(sender, "존재하지 않는 능력을 입력하였습니다.");
                    if (Configuration.Settings.DeveloperSettings.isEnabled()) e.printStackTrace();
                }
                return true;
            } else {
                final String name = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
                Bukkit.broadcastMessage(name);
                Class<? extends AbilityBase> class1 = AddonAbilityFactory.getTestAbilityByName(name);
                AbstractGame.Participant participant = GameManager.getGame().getParticipant((Player) sender);
                try {
                    participant.setAbility(class1);
                    Bukkit.broadcastMessage("§e" + sender.getName() + "§a님이 §f" + sender.getName() + "§a님에게 능력을 임의로 부여하였습니다.");
                } catch (ReflectiveOperationException e) {
                    Messager.sendErrorMessage(sender, "능력 설정 도중 오류가 발생하였습니다.");
                    if (Configuration.Settings.DeveloperSettings.isEnabled()) e.printStackTrace();
                } catch (NullPointerException e) {
                    Messager.sendErrorMessage(sender, "존재하지 않는 테스트 능력입니다.");
                    if (Configuration.Settings.DeveloperSettings.isEnabled()) e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }
}
