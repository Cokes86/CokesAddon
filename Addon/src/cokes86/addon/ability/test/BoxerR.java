package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import cokes86.addon.ability.CokesAbility.Config.Condition;
import cokes86.addon.util.PredicateUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

@AbilityManifest(name = "권투선수R", rank = Rank.A, species = Species.HUMAN, explain = {
        "패시브 - 잽: 검으로 공격 시, 모든 근거리 대미지가 $[JAP_DAMAGE_DECREMENT_PERCENTAGE]% 감소합니다.",
        "  잽을 포함한 모든 스킬 사용 이후, 2초간 다른 스킬을 사용할 수 있습니다.",
        "  이때, 잽을 제외한 스킬을 사용 시, 모든 스킬의 쿨타임이 1초 감소합니다.",
        "검 우클릭 - 스트레이트: 들고 있는 검의 $[STRAIGHT_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후 강하게 밀쳐냅니다. $[STRAIGHT_COOLDOWN]",
        "검 들고 F키 - 카운터: 들고 있는 검의 $[COUNTER_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후",
        "  최근에 받은 근거리 대미지의 $[COUNTER_THORN_PERCENTAGE]%의 고정 마법 대미지를 줍니다. $[COUNTER_COOLDOWN]",
        "검 들고 Q키 - 어퍼: 들고 있는 검의 $[UPPER_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후,",
        "  $[UPPER_STUN_PERCENTAGE]%의 확률로 기절을 1초 부여합니다. $[UPPER_COOLDOWN]",
        "웅크리기 - 더킹: 최대 1초간 받는 근거리 대미지가 $[DUCKING_DEFENCE_PERCENTAGE]% 감소합니다.",
        "  이후 잽을 제외한 연계되는 스킬의 대미지가 $[DUCKING_DAMAGE_DECREMENT] 감소합니다. $[DUCKING_COOLDOWN]",
        "[HIDDEN] 콤비네이션: 특정 스킬의 순서로 연계할 경우, 스킬의 대미지, 확률이 강화됩니다."
})
@Beta
public class BoxerR extends CokesAbility implements ActiveHandler {
    private static final Set<Material> swords;
    static {
        if (MaterialX.NETHERITE_SWORD.isSupported()) {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
        } else {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
        }
    }

    private static final Config<Double> JAP_DAMAGE_DECREMENT_PERCENTAGE = new Config<>(BoxerR.class, "jap-damage-decrement-percentage", 15.0,
            PredicateUnit.between(0.0,100.0,false),
            "# 잽 근거리 대미지 감소량", "# 기본값: 15.0 (%)");
    private static final Config<Double> STRAIGHT_DAMAGE_PERCENTAGE = new Config<>(BoxerR.class, "straight-damage-percentage", 70.0,
            PredicateUnit.positive(),
            "# 스트레이트 검 비례 대미지", "# 기본값: 70.0 (%)");
    private static final Config<Double> COUNTER_DAMAGE_PERCENTAGE = new Config<>(BoxerR.class, "counter-damage-percentage", 50.0,
            PredicateUnit.positive(),
            "# 카운터 검 비례 대미지", "# 기본값: 50.0 (%)");
    private static final Config<Double> COUNTER_THORN_PERCENTAGE = new Config<>(BoxerR.class, "counter-thorn-percentage", 50.0,
            PredicateUnit.between(0.0,100.0,true),
            "# 카운터 반사 대미지", "# 기본값: 50.0 (%)");
    private static final Config<Double> UPPER_DAMAGE_PERCENTAGE = new Config<>(BoxerR.class, "upper-damage-percentage", 70.0,
            PredicateUnit.positive(),
            "# 어퍼 검 비례 대미지", "# 기본값: 70.0 (%)");
    private static final Config<Double> UPPER_STUN_PERCENTAGE = new Config<>(BoxerR.class, "upper-stun-percentage", 30.0,
            PredicateUnit.between(0.0,100.0,true),
            "# 어퍼 기절 확률", "# 기본값: 30.0 (%)");
    private static final Config<Double> DUCKING_DEFENCE_PERCENTAGE = new Config<>(BoxerR.class, "ducking-defence-percentage", 50.0,
            PredicateUnit.positive(),
            "# 더킹 대미지 감소량", "# 기본값: 50.0 (%)");

    private static final Config<Integer> STRAIGHT_COOLDOWN = new Config<>(BoxerR.class, "straight-cooldown", 5, Condition.COOLDOWN,
            "# 스트레이트 쿨타임", "# 기본값: 5 (초)");
    private static final Config<Integer> COUNTER_COOLDOWN = new Config<>(BoxerR.class, "counter-cooldown", 7, Condition.COOLDOWN,
            "# 카운터 쿨타임", "# 기본값: 7 (초)");
    private static final Config<Integer> UPPER_COOLDOWN = new Config<>(BoxerR.class, "upper-cooldown", 10, Condition.COOLDOWN,
            "# 어퍼 쿨타임", "# 기본값: 10 (초)");
    private static final Config<Integer> DUCKING_COOLDOWN = new Config<>(BoxerR.class, "ducking-cooldown", 20, Condition.COOLDOWN,
            "# 더킹 쿨타임", "# 기본값: 20 (초)");

    private static final Config<Integer> DUCKING_DAMAGE_DECREMENT = new Config<>(BoxerR.class, "ducking-damage-decrement", 2, Condition.COOLDOWN,
            "# 더킹 이후 스킬 대미지 감소량", "# 기본값: 2");

    private final Cooldown straight_cooldown = new Cooldown(STRAIGHT_COOLDOWN.getValue(), "스트레이트");
    private final Cooldown counter_cooldown = new Cooldown(COUNTER_COOLDOWN.getValue(), "카운터");
    private final Cooldown upper_cooldown = new Cooldown(UPPER_COOLDOWN.getValue(), "어퍼");
    private final Cooldown ducking_cooldown = new Cooldown(DUCKING_COOLDOWN.getValue(), "더킹");


    private float thorn = 0;
    private final SkillTimer skillTimer = new SkillTimer();

    @Override
    public boolean usesMaterial(Material material) {
        return swords.contains(material);
    }

    public BoxerR(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            e.setDamage(e.getDamage() * (1 - JAP_DAMAGE_DECREMENT_PERCENTAGE.getValue() / 100.0));
            skillTimer.setParticipant(getGame().getParticipant(e.getEntity().getUniqueId())).addCombination('L').start();
        }

        if (e.getEntity().equals(getPlayer()) && getGame().isParticipating(e.getDamager().getUniqueId())) {
            thorn = (float) e.getFinalDamage();
            if (skillTimer.ducking) {
                e.setDamage(e.getDamage() * (1 - DUCKING_DEFENCE_PERCENTAGE.getValue() / 100.0));
            }
        }
    }

    @Override  //스트레이트
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (swords.contains(material) && clickType == ClickType.RIGHT_CLICK) {
            if (skillTimer.isRunning()) {
                float sword = SwordDamage.valueOf(material.name()).getDamage();
                int sharpness = getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                skillTimer.addCombination('R').start();

                double percentage = STRAIGHT_DAMAGE_PERCENTAGE.getValue();
                if (skillTimer.combination.size() >= 3 && skillTimer.getRecentCombination(3).equals("LSR")) {
                    percentage += 15.0;
                    getPlayer().sendMessage("스트레이트 콤비네이션!");
                }
                float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * percentage / 100.0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));
                skillTimer.participant.getPlayer().setVelocity(skillTimer.participant.getPlayer().getLocation().getDirection().clone().multiply(-2.5));

                skillTimer.setDucking(false);
                return straight_cooldown.start();
            }
        }
        return false;
    }

    @SubscribeEvent // 카운터
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() != null && swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
            Material sword_material = e.getOffHandItem().getType();
            ItemStack sword_itemstack = e.getOffHandItem();
            e.setCancelled(true);

            AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, sword_material, null);
            Bukkit.getPluginManager().callEvent(pre);
            if (skillTimer.isRunning() && !pre.isCancelled()) {
                float sword = SwordDamage.valueOf(sword_material.name()).getDamage();
                int sharpness = sword_itemstack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                skillTimer.addCombination('F').start();

                double damage_percentage = COUNTER_DAMAGE_PERCENTAGE.getValue();
                double thorn_percentage = COUNTER_THORN_PERCENTAGE.getValue();
                if (skillTimer.combination.size() >= 3 && skillTimer.getRecentCombination(3).equals("LSF")) {
                    damage_percentage += 15.0;
                    thorn_percentage += 5.0;
                    getPlayer().sendMessage("카운터 콤비네이션!");
                }

                float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * damage_percentage / 100.0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));
                skillTimer.participant.getPlayer().setNoDamageTicks(0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), true, (float) (thorn * thorn_percentage / 100.0f));

                skillTimer.setDucking(false);
                counter_cooldown.start();

                AbilityActiveSkillEvent active = new AbilityActiveSkillEvent(this, sword_material, null);
                Bukkit.getPluginManager().callEvent(active);
            }
        }
    }

    @SubscribeEvent // 어퍼
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (swords.contains(e.getItemDrop().getItemStack().getType()) && e.getPlayer().equals(getPlayer())) {
            ItemStack sword_itemstack = e.getItemDrop().getItemStack();
            Material sword_material = sword_itemstack.getType();
            e.setCancelled(true);

            AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, sword_material, null);
            Bukkit.getPluginManager().callEvent(pre);
            if (skillTimer.isRunning() && !pre.isCancelled()) {
                float sword = SwordDamage.valueOf(sword_material.name()).getDamage();
                int sharpness = sword_itemstack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                skillTimer.addCombination('Q').start();

                double damage_percentage = UPPER_DAMAGE_PERCENTAGE.getValue();
                double stun_percentage = UPPER_STUN_PERCENTAGE.getValue();
                if (skillTimer.combination.size() >= 3 && skillTimer.getRecentCombination(3).equals("LSQ")) {
                    damage_percentage += 15.0;
                    stun_percentage += 20.0;
                    getPlayer().sendMessage("어퍼 콤비네이션!");
                }

                float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * damage_percentage / 100.0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));

                double chance = new Random().nextDouble() * 99;
                if (chance <= stun_percentage) {
                    Stun.apply(skillTimer.participant, TimeUnit.SECONDS, 1);
                }

                skillTimer.setDucking(false);
                upper_cooldown.start();

                AbilityActiveSkillEvent active = new AbilityActiveSkillEvent(this, sword_material, null);
                Bukkit.getPluginManager().callEvent(active);
            }
        }
    }

    @SubscribeEvent  //더킹
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
        if (e.getPlayer().equals(getPlayer()) && e.isSneaking()) {
            AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, null, null);
            Bukkit.getPluginManager().callEvent(pre);
            if (skillTimer.isRunning() && !pre.isCancelled()) {
                skillTimer.addCombination('S').setDucking(true).start();
                ducking_cooldown.start();
                AbilityActiveSkillEvent active = new AbilityActiveSkillEvent(this, null, null);
                Bukkit.getPluginManager().callEvent(active);
            }
        }
    }

    private class SkillTimer extends AbilityTimer {
        private Participant participant;
        private final List<Character> combination = new ArrayList<>();
        private boolean ducking = false;
        private final ActionbarChannel channel = newActionbarChannel();
        private final DecimalFormat format = new DecimalFormat("0.##");
        public SkillTimer(){
            super(40);
            setPeriod(TimeUnit.TICKS, 1);
        }

        public SkillTimer setParticipant(Participant participant) {
            this.participant = participant;
            return this;
        }

        public SkillTimer addCombination(char c) {
            combination.add(c);
            return this;
        }

        public SkillTimer setDucking(boolean ducking) {
            this.ducking = ducking;
            return this;
        }

        public String getRecentCombination(int count) {
            StringBuilder result = new StringBuilder();
            for (int a = count; a > 0; a--) {
                result.append(combination.get(combination.size() - a));
            }
            return result.toString();
        }

        @Override
        public boolean start() {
            if (isRunning()) {
                channel.update("");
                stop(true);

                if (getRecentCombination(1).equals("L") || getRecentCombination(1).equals("S")) {
                    straight_cooldown.setCount(straight_cooldown.getCount() - 1);
                    counter_cooldown.setCount(counter_cooldown.getCount() - 1);
                    upper_cooldown.setCount(upper_cooldown.getCount() - 1);
                }
            }
            return super.start();
        }

        @Override
        protected void run(int count) {
            if (count <= 20 && ducking) {
                ducking = false;
            }
            channel.update("스킬 사용 가능 시간 : §c"+format.format(count/20.0)+"초");
        }

        @Override
        protected void onEnd() {
            combination.clear();
            ducking = false;
            super.onEnd();
        }

        @Override
        public boolean isRunning() {
            if (!super.isRunning()) {
                getPlayer().sendMessage(ChatColor.RED + "아직 스킬을 사용할 수 없습니다.");
            }
            return super.isRunning();
        }
    }

    private enum SwordDamage {
        WOODEN_SWORD(4), STONE_SWORD(5), IRON_SWORD(6), GOLDEN_SWORD(4), DIAMOND_SWORD(7), NETHERITE_SWORD(8);

        private final int damage;
        public int getDamage() {
            return damage;
        }

        SwordDamage(int damage) {
            this.damage = damage;
        }
    }
}