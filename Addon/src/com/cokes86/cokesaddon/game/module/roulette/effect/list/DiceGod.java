package com.cokes86.cokesaddon.game.module.roulette.effect.list;

import com.cokes86.cokesaddon.game.module.roulette.RouletteManifest;
import com.cokes86.cokesaddon.game.module.roulette.effect.RouletteSingleEffect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RouletteManifest(name = "5초간 재생/힘/신속/저항/구속/나약함 중 하나 부여", defaultPriority = 2)
public class DiceGod extends RouletteSingleEffect {
    @Override
    public void effect(Participant participant) {
        Random random = new Random();
        int a = random.nextInt(6);
        Player player = participant.getPlayer();
        switch(a) {
            case 0: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §c재생 §f효과를 받습니다.");
                PotionEffects.REGENERATION.addPotionEffect(player, 100, 1, true);
                break;
            }
            case 1: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §6힘 §f효과를 받습니다.");
                PotionEffects.INCREASE_DAMAGE.addPotionEffect(player, 100, 1, true);
                break;
            }
            case 2: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §3저항 §f효과를 받습니다.");
                PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(player, 100, 1, true);
                break;
            }
            case 3: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §b신속 §f효과를 받습니다.");
                PotionEffects.SPEED.addPotionEffect(player, 100, 1, true);
                break;
            }
            case 4: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §8구속 §f효과를 받습니다.");
                PotionEffects.SLOW.addPotionEffect(player, 100, 1, true);
                break;
            }
            case 5: {
                Bukkit.broadcastMessage("[룰렛] §b"+player.getName()+"§f이(가) 5초간 §7나약함 §f효과를 받습니다.");
                PotionEffects.WEAKNESS.addPotionEffect(player, 100, 1, true);
            }
        }
    }
}
