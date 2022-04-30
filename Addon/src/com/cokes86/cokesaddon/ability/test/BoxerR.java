package com.cokes86.cokesaddon.ability.test;

import com.cokes86.cokesaddon.ability.CokesAbility;
import com.cokes86.cokesaddon.ability.CokesAbility.Config.Condition;
import com.cokes86.cokesaddon.util.CokesUtil;
import com.cokes86.cokesaddon.util.FunctionalInterfaceUnit;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

@AbilityManifest(name = "권투선수R", rank = Rank.A, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c잽§f: 검으로 공격 시 대미지가 $[JAP_DAMAGE_DECREMENT_PERCENTAGE]% 감소합니다.",
        "  잽을 포함한 모든 스킬 사용 이후, 2초간 다른 스킬을 사용할 수 있습니다.",
        "  이때, 잽을 제외한 스킬을 사용 시, 모든 스킬의 쿨타임이 1초 감소하며, 무적틱을 무시합니다.",
        "  §c잽§f과 §e더킹을 제외한 스킬을 사용할 시, 최근에 공격한 상대방을 바라보고 있어야 합니다.",
        "§7검 우클릭 §8- §e스트레이트§f: 들고 있는 검의 $[STRAIGHT_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후 강하게 밀쳐냅니다. $[STRAIGHT_COOLDOWN]",
        "§7검 들고 F키 §8- §e카운터§f: 들고 있는 검의 $[COUNTER_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후",
        "  최근에 받은 근거리 대미지의 $[COUNTER_THORN_PERCENTAGE]%의 고정 마법 대미지를 줍니다. $[COUNTER_COOLDOWN]",
        "§7검 들고 Q키 §8- §e어퍼§f: 들고 있는 검의 $[UPPER_DAMAGE_PERCENTAGE]%의 마법 대미지를 준 후,",
        "  $[UPPER_STUN_PERCENTAGE]%의 확률로 기절을 1초 부여합니다. $[UPPER_COOLDOWN]",
        "§7웅크리기 §8- §e더킹§f: 최대 1초간 받는 근거리 대미지가 $[DUCKING_DEFENCE_PERCENTAGE]% 감소합니다.",
        "  이후 §c잽§f을 제외한 연계되는 스킬의 대미지가 $[DUCKING_DAMAGE_DECREMENT] 감소합니다. $[DUCKING_COOLDOWN]",
        "§8[§7HIDDEN§8] §b콤비네이션§f: 특정 순서로 스킬을 연계할 경우, 스킬의 효과가 강화됩니다."
})
@Beta
public class BoxerR extends CokesAbility implements TargetHandler {
    private static final Set<Material> swords = CokesUtil.getSwords();

    private static final String prefix = "[§b권투선수§f] ";

    //공격, 확률 컨피그
    private static final Config<Double> JAP_DAMAGE_DECREMENT_PERCENTAGE = Config.of(BoxerR.class, "jap-damage-decrement-percentage", 15.0,
            FunctionalInterfaceUnit.between(0.0,100.0,false),
            "# 잽 근거리 대미지 감소량", "# 기본값: 15.0 (%)");
    private static final Config<Double> STRAIGHT_DAMAGE_PERCENTAGE = Config.of(BoxerR.class, "straight-damage-percentage", 110.0,
            FunctionalInterfaceUnit.positive(),
            "# 스트레이트 검 비례 대미지", "# 기본값: 110.0 (%)");
    private static final Config<Double> COUNTER_DAMAGE_PERCENTAGE = Config.of(BoxerR.class, "counter-damage-percentage", 80.0,
            FunctionalInterfaceUnit.positive(),
            "# 카운터 검 비례 대미지", "# 기본값: 80.0 (%)");
    private static final Config<Double> COUNTER_THORN_PERCENTAGE = Config.of(BoxerR.class, "counter-thorn-percentage", 50.0,
            FunctionalInterfaceUnit.between(0.0,100.0,true),
            "# 카운터 반사 대미지", "# 기본값: 50.0 (%)");
    private static final Config<Double> UPPER_DAMAGE_PERCENTAGE = Config.of(BoxerR.class, "upper-damage-percentage", 90.0,
            FunctionalInterfaceUnit.positive(),
            "# 어퍼 검 비례 대미지", "# 기본값: 90.0 (%)");
    private static final Config<Double> UPPER_STUN_PERCENTAGE = Config.of(BoxerR.class, "upper-stun-percentage", 30.0,
            FunctionalInterfaceUnit.between(0.0,100.0,true),
            "# 어퍼 기절 확률", "# 기본값: 30.0 (%)");
    private static final Config<Double> DUCKING_DEFENCE_PERCENTAGE = Config.of(BoxerR.class, "ducking-defence-percentage", 50.0,
            FunctionalInterfaceUnit.positive(),
            "# 더킹 대미지 감소량", "# 기본값: 50.0 (%)");

    //쿨타임 컨피그
    private static final Config<Integer> STRAIGHT_COOLDOWN = Config.of(BoxerR.class, "straight-cooldown", 5, Condition.COOLDOWN,
            "# 스트레이트 쿨타임", "# 기본값: 5 (초)");
    private static final Config<Integer> COUNTER_COOLDOWN = Config.of(BoxerR.class, "counter-cooldown", 7, Condition.COOLDOWN,
            "# 카운터 쿨타임", "# 기본값: 7 (초)");
    private static final Config<Integer> UPPER_COOLDOWN = Config.of(BoxerR.class, "upper-cooldown", 10, Condition.COOLDOWN,
            "# 어퍼 쿨타임", "# 기본값: 10 (초)");
    private static final Config<Integer> DUCKING_COOLDOWN = Config.of(BoxerR.class, "ducking-cooldown", 20, Condition.COOLDOWN,
            "# 더킹 쿨타임", "# 기본값: 20 (초)");

    //더킹 댐감 컨피그
    private static final Config<Integer> DUCKING_DAMAGE_DECREMENT = Config.of(BoxerR.class, "ducking-damage-decrement", 2, Condition.COOLDOWN,
            "# 더킹 이후 스킬 대미지 감소량", "# 기본값: 2");

    //콤비네이션 추가 컨피그
    private static final Config<Double> COMBINATION_STRAIGHT_DAMAGE_INCREMENT = Config.of(BoxerR.class, "combination.straight.damage-increment", 15.0, FunctionalInterfaceUnit.positive(),
            "# 스트레이트 콤비네이션 대미지 증가량", "# 기본값: 15.0 (%p)");
    private static final Config<Double> COMBINATION_COUNTER_DAMAGE_INCREMENT = Config.of(BoxerR.class, "combination.counter.damage-increment", 15.0, FunctionalInterfaceUnit.positive(),
            "# 카운터 콤비네이션 대미지 증가량", "# 기본값: 15.0 (%p)");
    private static final Config<Double> COMBINATION_COUNTER_THORN_INCREMENT = Config.of(BoxerR.class, "combination.counter.thorn-increment", 5.0, FunctionalInterfaceUnit.positive(),
            "# 카운터 콤비네이션 반사 대미지 증가량", "# 기본값: 5.0 (%p)");
    private static final Config<Double> COMBINATION_UPPER_DAMAGE_INCREMENT = Config.of(BoxerR.class, "combination.upper.damage-increment", 15.0, FunctionalInterfaceUnit.positive(),
            "# 어퍼 콤비네이션 대미지 증가량", "# 기본값: 15.0 (%p)");
    private static final Config<Double> COMBINATION_UPPER_STUN_INCREMENT = Config.of(BoxerR.class, "combination.upper.stun-increment", 10.0, FunctionalInterfaceUnit.positive(),
            "# 어퍼 콤비네이션 기절 확률 증가량", "# 기본값: 10.0 (%p)");
    private static final Config<Double> COMBINATION_JPH_STUN_INCREMENT = Config.of(BoxerR.class, "combination.jap-punch-hook.stun-increment", 15.0, FunctionalInterfaceUnit.positive(),
            "# 잽, 펀치, 훅! 콤비네이션 기절 확률 증가량", "# 기본값: 15.0 (%p)");
    private static final Config<Double> COMBINATION_FIND_GAP_STUN_INCREMENT = Config.of(BoxerR.class, "combination.find-gap.stun-increment", 20.0, FunctionalInterfaceUnit.positive(),
            "# 빈틈 발견! 콤비네이션 기절 확률 증가량", "# 기본값: 20.0 (%p)");

    private final Cooldown straight_cooldown = new Cooldown(STRAIGHT_COOLDOWN.getValue(), "스트레이트");
    private final Cooldown counter_cooldown = new Cooldown(COUNTER_COOLDOWN.getValue(), "카운터");
    private final Cooldown upper_cooldown = new Cooldown(UPPER_COOLDOWN.getValue(), "어퍼");
    private final Cooldown ducking_cooldown = new Cooldown(DUCKING_COOLDOWN.getValue(), "더킹");

    private float thorn = 0;
    private final SkillTimer skillTimer = new SkillTimer();

    private long latest = 0;

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
                final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
                return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
            }
        }
        return false;
    };

    @Override
    public boolean usesMaterial(Material material) {
        return swords.contains(material);
    }

    public BoxerR(Participant arg0) {
        super(arg0);
    }

    @SubscribeEvent  // 잽, 더킹 댐감
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().equals(getPlayer()) && getGame().isParticipating(e.getEntity().getUniqueId())) {
            if (e.getCause() == DamageCause.MAGIC) return;
            e.setDamage(e.getDamage() * (1 - JAP_DAMAGE_DECREMENT_PERCENTAGE.getValue() / 100.0));
            skillTimer.setParticipant(getGame().getParticipant(e.getEntity().getUniqueId())).addCombination('L').start();
        }

        if (e.getEntity().equals(getPlayer()) && getGame().isParticipating(e.getDamager().getUniqueId())) {
            thorn = (float) e.getFinalDamage();
            if (skillTimer.isRunning() && skillTimer.ducking) {
                e.setDamage(e.getDamage() * (1 - DUCKING_DEFENCE_PERCENTAGE.getValue() / 100.0));
            }
        }
    }

    @Override  //스트레이트
    public void TargetSkill(Material material, LivingEntity livingEntity) {
        final long current = System.currentTimeMillis();
        if (swords.contains(material) && skillTimer.isAbleSkill() && livingEntity.equals(skillTimer.participant.getPlayer()) && straight_cooldown.isCooldown() && current - latest <= 250) {
            float sword = SwordDamage.valueOf(material.name()).getDamage();
            int sharpness = getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
            skillTimer.addCombination('R').start();

            double percentage = STRAIGHT_DAMAGE_PERCENTAGE.getValue();
            if (skillTimer.combination.size() >= 3 && CokesUtil.xor_encoding(skillTimer.getRecentCombination(3), 0x415701).equals("坍坒坓")) {
                percentage += COMBINATION_STRAIGHT_DAMAGE_INCREMENT.getValue();
                getPlayer().sendMessage(prefix + "스트레이트 콤비네이션!");
                getPlayer().sendMessage(prefix + "대미지 + "+ COMBINATION_STRAIGHT_DAMAGE_INCREMENT.getValue()+"%p");
            }
            float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * percentage / 100.0);
            skillTimer.participant.getPlayer().setNoDamageTicks(0);
            Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));
            skillTimer.participant.getPlayer().setVelocity(skillTimer.participant.getPlayer().getLocation().getDirection().clone().multiply(-2.5));

            skillTimer.setDucking(false);
            straight_cooldown.start();
            latest = current;
        }
    }

    @SubscribeEvent // 카운터
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        final long current = System.currentTimeMillis();
        if (e.getOffHandItem() != null && swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
            Material sword_material = e.getOffHandItem().getType();
            ItemStack sword_itemstack = e.getOffHandItem();
            e.setCancelled(true);
            LivingEntity entity = LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 4, predicate);

            if (skillTimer.isAbleSkill() && counter_cooldown.isCooldown() && entity.equals(skillTimer.participant.getPlayer()) && current - latest <= 250) {
                float sword = SwordDamage.valueOf(sword_material.name()).getDamage();
                int sharpness = sword_itemstack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                skillTimer.addCombination('F').start();

                double damage_percentage = COUNTER_DAMAGE_PERCENTAGE.getValue();
                double thorn_percentage = COUNTER_THORN_PERCENTAGE.getValue();
                if (skillTimer.combination.size() >= 3 && CokesUtil.xor_encoding(skillTimer.getRecentCombination(3), 0x27f9a5).equals("里臨泥")) {
                    damage_percentage += COMBINATION_COUNTER_DAMAGE_INCREMENT.getValue();
                    thorn_percentage += COMBINATION_COUNTER_THORN_INCREMENT.getValue();
                    getPlayer().sendMessage(prefix + "카운터 콤비네이션!");
                    getPlayer().sendMessage(prefix + "대미지 + "+ COMBINATION_COUNTER_DAMAGE_INCREMENT.getValue()+"%p, 반사대미지 + "+ COMBINATION_COUNTER_THORN_INCREMENT.getValue()+"%p");
                }

                skillTimer.participant.getPlayer().setNoDamageTicks(0);
                float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * damage_percentage / 100.0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));
                skillTimer.participant.getPlayer().setNoDamageTicks(0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), true, (float) (thorn * thorn_percentage / 100.0f));

                skillTimer.setDucking(false);
                counter_cooldown.start();
                latest = current;
            }
        }
    }

    @SubscribeEvent // 어퍼
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        final long current = System.currentTimeMillis();
        if (swords.contains(e.getItemDrop().getItemStack().getType()) && e.getPlayer().equals(getPlayer())) {
            ItemStack sword_itemstack = e.getItemDrop().getItemStack();
            Material sword_material = sword_itemstack.getType();
            e.setCancelled(true);
            LivingEntity entity = LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 4, predicate);

            if (skillTimer.isAbleSkill() && upper_cooldown.isCooldown() && entity.equals(skillTimer.participant.getPlayer()) && current - latest <= 250) {
                float sword = SwordDamage.valueOf(sword_material.name()).getDamage();
                int sharpness = sword_itemstack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                skillTimer.addCombination('Q').start();

                double damage_percentage = UPPER_DAMAGE_PERCENTAGE.getValue();
                double stun_percentage = UPPER_STUN_PERCENTAGE.getValue();
                if (skillTimer.combination.size() >= 3 && CokesUtil.xor_encoding(skillTimer.getRecentCombination(3), 0x565673).equals("嘿嘠嘢")) {
                    damage_percentage += COMBINATION_UPPER_DAMAGE_INCREMENT.getValue();
                    stun_percentage += COMBINATION_UPPER_STUN_INCREMENT.getValue();
                    getPlayer().sendMessage(prefix + "어퍼 콤비네이션!");
                    getPlayer().sendMessage(prefix + "대미지 + "+ COMBINATION_UPPER_DAMAGE_INCREMENT.getValue()+"%p, 기절 확률 + "+ COMBINATION_UPPER_STUN_INCREMENT.getValue()+"%p");
                }
                if (skillTimer.combination.size() >= 3 && CokesUtil.xor_encoding(skillTimer.getRecentCombination(3), 0xb1117e).equals("ᄲᄬᄯ")) {
                    stun_percentage += COMBINATION_JPH_STUN_INCREMENT.getValue();
                    getPlayer().sendMessage(prefix + "잽, 펀치, 훅!");
                    getPlayer().sendMessage(prefix + "기절 확률 + "+ COMBINATION_UPPER_STUN_INCREMENT.getValue()+"%p");
                }

                if (skillTimer.combination.size() >= 4 && CokesUtil.xor_encoding(skillTimer.getRecentCombination(4), 0xceb510).equals("땜땂땃땁")) {
                    stun_percentage += COMBINATION_FIND_GAP_STUN_INCREMENT.getValue();
                    getPlayer().sendMessage(prefix + "빈틈 발견!");
                    getPlayer().sendMessage(prefix + "기절 확률 + "+ COMBINATION_FIND_GAP_STUN_INCREMENT.getValue()+"%p");
                }

                skillTimer.participant.getPlayer().setNoDamageTicks(0);
                float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(sword, sharpness) * damage_percentage / 100.0);
                Damages.damageMagic(skillTimer.participant.getPlayer(), getPlayer(), false, damage - (skillTimer.ducking ? DUCKING_DAMAGE_DECREMENT.getValue() : 0));

                double chance = new Random().nextDouble() * 99;
                if (chance <= stun_percentage) {
                    Stun.apply(skillTimer.participant, TimeUnit.SECONDS, 1);
                }

                skillTimer.setDucking(false);
                upper_cooldown.start();
                latest = current;
            }
        }
    }

    @SubscribeEvent  //더킹
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
        if (e.getPlayer().equals(getPlayer()) && e.isSneaking()) {
            if (skillTimer.isAbleSkill() && ducking_cooldown.isCooldown()) {
                skillTimer.addCombination('S').setDucking(true).start();
                ducking_cooldown.start();
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

                if (!getRecentCombination(1).equals("L") || !getRecentCombination(1).equals("S")) {
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

        public boolean isAbleSkill() {
            if (!isRunning()) {
                getPlayer().sendMessage(ChatColor.RED + "아직 스킬을 사용할 수 없습니다.");
            }
            return isRunning();
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