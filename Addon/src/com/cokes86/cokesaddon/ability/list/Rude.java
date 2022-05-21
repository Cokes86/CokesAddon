package com.cokes86.cokesaddon.ability.list;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.CokesAbility.Config.Condition;
import com.cokes86.cokesaddon.event.CEntityDamageEvent;
import com.cokes86.cokesaddon.util.AttributeUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@AbilityManifest(name = "진상", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 우클릭 시 1초마다 $[RUDE_RANGE]블럭 내 모든 플레이어의",
        "인벤토리를 뒤엎어버린 후, 1의 관통 대미지를 줍니다.",
        "이는 $[RUDE_DURATION]간 반복되며, 이미 한 번 뒤엎은 플레이어가 있다면",
        "더이상 뒤엎지 않습니다.",
        "지속시간동안 이동속도가 매우 느려지지만, 받는 대미지가 $[DAMAGE]% 감소합니다 $[RUDE_COOLDOWN]",
        "갑옷과, 양 손의 아이템은 뒤엎지 않습니다."
})
public class Rude extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RUDE_RANGE = Config.of(Rude.class, "범위", 10, FunctionalInterfaceUnit.positive());
    private static final Config<Integer> RUDE_DURATION = Config.of(Rude.class, "지속시간", 5, Condition.TIME);
    private static final Config<Integer> RUDE_COOLDOWN = Config.of(Rude.class, "쿨타임", 60, Condition.COOLDOWN);
    private static final Config<Integer> DAMAGE = Config.of(Rude.class, "받는_대미지_감소량(%)", 20, FunctionalInterfaceUnit.between(0, 100, false));
    private final Cooldown cooldown = new Cooldown(RUDE_COOLDOWN.getValue());
    private final RudeDuration duration = new RudeDuration();

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())
                    || (getGame() instanceof DeathManager.Handler &&
                    ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
                return false;
            }
            if (getGame() instanceof Teamable) {
                final Teamable teamGame = (Teamable) getGame();
                final AbstractGame.Participant entityParticipant = teamGame.getParticipant(
                        entity.getUniqueId()), participant = getParticipant();
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
                        || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return false;
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
        private final List<Player> inventory = new ArrayList<>();
        private double knockback;
        private final ItemStack nullItemStack = new ItemStack(Material.AIR);
        final Random random = new Random();

        public RudeDuration() {
            super(RUDE_DURATION.getValue(), cooldown);
        }

        @Override
        protected void onDurationStart() {
            knockback = AttributeUtil.getKnockbackResistance(getPlayer());
        }

        @Override
        protected void onDurationProcess(int i) {
            PotionEffects.SLOW.addPotionEffect(getPlayer(), 30, 4, true);
            int range = RUDE_RANGE.getValue();
            for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
                if (!inventory.contains(player)) {
                    inventory.add(player);
                    shuffleInventory(player);
                }
            }
            SoundLib.ENTITY_DRAGON_FIREBALL_EXPLODE.playSound(getPlayer().getLocation());
        }

        @Override
        protected void onDurationEnd() {
            inventory.clear();
            PotionEffects.SLOW.removePotionEffect(getPlayer());
        }

        @Override
        protected void onDurationSilentEnd() {
            inventory.clear();
            AttributeUtil.setKnockbackResistance(getPlayer(), knockback);
            PotionEffects.SLOW.removePotionEffect(getPlayer());
        }

        private void shuffleInventory(Player player) {
            List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
            items.remove(player.getInventory().getHelmet());
            items.remove(player.getInventory().getChestplate());
            items.remove(player.getInventory().getLeggings());
            items.remove(player.getInventory().getBoots());
            items.remove(player.getInventory().getItemInOffHand());
            items.removeIf(Objects::isNull);
            items = new ArrayList<>(items);
            int mainhand = player.getInventory().getHeldItemSlot();
            List<Integer> index = getRandomNumber(items.size(), mainhand);

            for (int a = 0; a < 36; a++) {
                if (a==mainhand) continue;
                if (index.contains(a)) {
                    ItemStack pickes = random.pick(items);
                    player.getInventory().setItem(a, pickes);
                    items.remove(pickes);
                } else {
                    player.getInventory().setItem(a, nullItemStack);
                }
            }
        }

        private List<Integer> getRandomNumber(int count, int ignore) {
            List<Integer> result = new ArrayList<>();
            while (result.size() < count) {
                int a = random.nextInt(36);
                if (!result.contains(a) && ignore != a) {
                    result.add(a);
                }
            }
            return result;
        }
    }
}
