package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.Config;
import com.cokes86.cokesaddon.ability.decorate.Lite;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.FunctionalInterfaces;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@AbilityManifest(name = "진상", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "§7철괴 우클릭 §8- §c뒤집어 엎기§f: 1초마다 $[RUDE_RANGE]블럭 내 모든 플레이어에게",
        "  주 손에 있는 아이템을 인벤토리 내의 아이템으로 변경한 후, ",
        "  $[RUDE_DAMAGE]의 관통 대미지를 주며 $[RUDE_DURATION]회 반복합니다. $[RUDE_COOLDOWN]",
        "  지속시간동안 이동속도가 매우 느려지지만, 받는 대미지가 $[DAMAGE]% 감소합니다.",
        "§7상태이상 §8- §3셔플§f: 양손과 갑옷을 제외한 모든 인벤토리가 뒤섞이며, 위치를 조정할 수 없습니다.",
        "  상태이상이 사라지면, 상태이상이 적용되기 전으로 돌아갑니다."
})
@Lite
public class Rude extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RUDE_RANGE = Config.of(Rude.class, "범위", 5, FunctionalInterfaces.positive());
    private static final Config<Integer> RUDE_DURATION = Config.of(Rude.class, "지속시간", 4, FunctionalInterfaces.positive(), FunctionalInterfaces.TIME);
    private static final Config<Float> RUDE_DAMAGE = Config.of(Rude.class, "damage", 1.5f, FunctionalInterfaces.positive());
    private static final Config<Integer> RUDE_COOLDOWN = Config.of(Rude.class, "쿨타임", 60, FunctionalInterfaces.positive(), FunctionalInterfaces.COOLDOWN);
    private static final Config<Double> DAMAGE = Config.of(Rude.class, "받는_대미지_감소량(%)", 10d, FunctionalInterfaces.chance(false, false));
    private final Cooldown cooldown = new Cooldown(RUDE_COOLDOWN.getValue());
    private final RudeDuration duration = new RudeDuration();

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())
                    || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                return false;
            }
            if (getGame() instanceof Teamable) {
                final Teamable teamGame = (Teamable) getGame();
                final AbstractGame.Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return true;
    };

    public Rude(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (!cooldown.isCooldown() && !duration.isDuration()) {
                return duration.start();
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onEntityDamage(CEntityDamageEvent e) {
        if (e.getEntity().equals(getPlayer()) && duration.isRunning()) {
            e.setDamage(e.getDamage() * (1 - DAMAGE.getValue() / 100.0));
        }
    }

    private class RudeDuration extends Duration {

        public RudeDuration() {
            super(RUDE_DURATION.getValue(), cooldown);
        }

        @Override
        protected void onDurationProcess(int i) {
            PotionEffects.SLOW.addPotionEffect(getPlayer(), 30, 4, true);
            int range = RUDE_RANGE.getValue();
            for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
                Damages.damageFixed(player, getPlayer(), RUDE_DAMAGE.getValue());
                swapRandomItemWithMainHand(player);
            }
            SoundLib.ENTITY_DRAGON_FIREBALL_EXPLODE.playSound(getPlayer().getLocation());
        }

        @Override
        protected void onDurationEnd() {
            PotionEffects.SLOW.removePotionEffect(getPlayer());
        }

        @Override
        protected void onDurationSilentEnd() {
            PotionEffects.SLOW.removePotionEffect(getPlayer());
        }

        public void swapRandomItemWithMainHand(Player player) {
            PlayerInventory inv = player.getInventory();

            List<Integer> candidates = new ArrayList<>();

            // 플레이어 인벤토리(핫바 포함, 메인손 슬롯 제외)
            for (int slot = 0; slot < 36; slot++) {
                // 현재 선택된 핫바 슬롯(메인손)은 제외
                if (slot == inv.getHeldItemSlot()) {
                    continue;
                }

                ItemStack item = inv.getItem(slot);
                if (item != null) {
                    candidates.add(slot);
                }
            }

            // 교환할 아이템이 없으면 종료
            if (candidates.isEmpty()) {
                return;
            }

            int slot = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

            ItemStack mainHand = inv.getItemInMainHand();
            ItemStack target = inv.getItem(slot);

            inv.setItemInMainHand(target);
            inv.setItem(slot, mainHand);
        }
    }
}
