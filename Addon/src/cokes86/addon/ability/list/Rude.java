package cokes86.addon.ability.list;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.util.AttributeUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.annotations.Beta;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Predicate;

@AbilityManifest(name = "진상", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN, explain = {
        "철괴 우클릭 시 1초마다 $[RUDE_RANGE]블럭 내 모든 플레이어의",
        "인벤토리를 뒤엎어버린 후, 고정 1대미지를 줍니다.",
        "이는 $[RUDE_DURATION]간 반복되며, 이미 한 번 뒤엎은 플레이어가 있다면",
        "더이상 뒤엎지 않습니다.",
        "지속시간동안 이동속도가 매우 느려지지만, 넉백당하지 않습니다. $[RUDE_COOLDOWN]",
        "갑옷과, 양 손의 아이템은 뒤엎지 않습니다."
})
@Beta
public class Rude extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> RUDE_RANGE = new Config<>(Rude.class, "범위", 10, a -> a > 0);
    private static final Config<Integer> RUDE_DURATION = new Config<>(Rude.class, "지속시간", 5, Config.Condition.TIME);
    private static final Config<Integer> RUDE_COOLDOWN = new Config<>(Rude.class, "쿨타임", 60, Config.Condition.COOLDOWN);
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

    private class RudeDuration extends Duration implements Listener {
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
            AttributeUtil.setKnockbackResistance(getPlayer(), 0);
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
            AttributeUtil.setKnockbackResistance(getPlayer(), knockback);
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
