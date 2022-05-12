package com.cokes86.cokesaddon.synergy.list;

import com.cokes86.cokesaddon.synergy.CokesSynergy;
import com.cokes86.cokesaddon.synergy.CokesSynergy.Config.Condition;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

//복수 + 복수귀
@AbilityManifest(name = "와신상담", rank = Rank.A, species = Species.UNDEAD, explain = {
        "어떤 목표를 이루기 위해 다가오는 어떠한 고난도 참고 이겨낸다",
        "회복이 불가능한 대신 상대방에게 주는 대미지가 $[DAMAGE_INCREMENT]% 증가합니다.",
        "원한 상태가 아닐 때 사망 시, 원한 상태가 되어 가짜 사망 메시지를 띄우고 다시 부활합니다.",
        "원한 상태가 되고 나서 내가 대상에게 피해를 주거나",
        "내가 대상에게 피해입을 때마다 공격력이 점점 증가합니다.",
        "대상을 처치하는데 성공할 경우 원한 상태가 해제됩니다.",
        "원한 상태에서 철괴 우클릭 시, 대상에게 텔레포트합니다. $[COOLDOWN]"
})
public class Wasinsangdam extends CokesSynergy implements ActiveHandler {
    private static final Config<Double> DAMAGE_INCREMENT = Config.of(Wasinsangdam.class, "damage-increment", 25.0, FunctionalInterfaceUnit.positive());
    private static final Config<Integer> COOLDOWN = Config.of(Wasinsangdam.class, "cooldown", 50, Condition.COOLDOWN);

    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

    private double damage_increment = 0;
    private Participant resentment = null;
    public Wasinsangdam(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && resentment != null && !cooldown.isCooldown()) {
            getPlayer().teleport(resentment.getPlayer().getLocation());
            return cooldown.start();
        }
        return false;
    }

    @SubscribeEvent
    public void onParticipantDeath(ParticipantDeathEvent e) {
        if (e.getParticipant().equals(resentment)) {
            resentment = null;
        }
    }

    @SubscribeEvent
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        if (e.getEntity().equals(getPlayer())) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            if (arrow.getShooter() instanceof Entity) {
                damager = (Entity) arrow.getShooter();
            }
        }

        if (damager.equals(getPlayer())) {
            if (e.getEntity().equals(resentment.getPlayer())) {
                double final_damage = e.getFinalDamage();
                e.setDamage(e.getDamage() * (1 + DAMAGE_INCREMENT.getValue()/100) + damage_increment);
                damage_increment += final_damage * 0.25;
            } else {
                e.setDamage(e.getDamage() * (1 + DAMAGE_INCREMENT.getValue()/100));
            }
        }

        if (damager.equals(resentment.getPlayer()) && e.getEntity().equals(getPlayer())) {
            damage_increment += e.getFinalDamage() * 0.25;
        }

        if (e.getEntity().equals(getPlayer()) && resentment == null && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
            Player killer = getPlayer().getKiller();
            if (killer != null && getGame().isParticipating(killer)) {
                Bukkit.broadcastMessage("§f[§c능력§f] §c" + getPlayer().getName() + "§f님의 능력은 §e와신상담 (복수 + 복수귀)§f였습니다.");
                Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f가 §a" + getPlayer().getKiller().getName() + "§f에게 살해당했습니다. §7컷!");
                Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f는 이제 §a" + getPlayer().getKiller().getName() + "§f에게 §d원한§f를 준비합니다...");
                getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                resentment = getGame().getParticipant(killer);
                e.setCancelled(true);
            }
        }
    }
}
