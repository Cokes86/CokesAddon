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
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.Set;

@AbilityManifest(name = "권투선수R", rank = Rank.A, species = Species.HUMAN, explain = {
        "패시브 - 잽: 상대방을 근거리 공격할 시 상대방을 대상으로 능력이 2초간 활성화되어 사용할 수 있습니다.",
        "근거리 공격 - 스트레이트: 근거리 대미지가 $[STRAIGHT_DAMAGE_INCREMENT] 증가합니다.",
        "  스트레이트는 액티브 능력으로 취급하지 않습니다.",
        "검 우클릭 - 훅: $[HOOK_DAMAGE]의 마법 대미지를 주며, 옆으로 밀쳐냅니다. $[HOOK_COOLDOWN]",
        "검 F키 - 카운터: 1초 내 받았던 근거리 대미지의 $[COUNTER_PERCENTAGE]%를 상대방에게 고정 마법 대미지를 입힙니다. $[COUNTER_COODOWN]",
        "검 Q키 - 어퍼: $[UPPER_DAMAGE]의 마법 대미지를 주며, $[UPPDER_PERCENTAGE]%의 확률로 기절을 1초 부여합니다. $[UPPER_COOLDOWN]",
        "웅크리기 - 더킹: 최대 1초동안 받는 근거리 대미지가 20% 감소합니다.",
        "  더킹 도중 더킹,잽 이외의 능력을 사용할 수 있습니다.",
        "  단, 이때의 모든 근거리 대미지가 $[DUCKING_DAMAGE_DECREMENT] 감소합니다. $[DUCKING_COOLDOWN]"
})
@Beta
public class BoxerR extends CokesAbility implements ActiveHandler {
    private static final Config<Double> STRAIGHT_DAMAGE_INCREMENT = new Config<>(BoxerR.class, "straight-damage-increment", 2.0, PredicateUnit.positive(),
            "# 스트레이트 대미지 증가량", "기본값 : 2.0");

    private static final Config<Float> HOOK_DAMAGE = new Config<>(BoxerR.class, "hook-damage", 5.0f, PredicateUnit.positive(),
            "# 훅 대미지", "기본값 : 5.0");
    private static final Config<Integer> HOOK_COOLDOWN = new Config<>(BoxerR.class, "hook-cooldown", 6, Condition.COOLDOWN,
            "# 훅 쿨타임", "기본값 : 6 (초)");

    private static final Config<Double> COUNTER_PERCENTAGE = new Config<>(BoxerR.class, "counter-percentage", 50.0, PredicateUnit.between(0.0,100.0,false),
            "# 카운터 반사비율", "기본값 : 50.0 (%)");
    private static final Config<Integer> COUNTER_COOLDOWN = new Config<>(BoxerR.class, "counter-cooldown", 20, Condition.COOLDOWN,
            "# 카운터 쿨타임", "기본값 : 20 (초)");

    private static final Config<Float> UPPER_DAMAGE = new Config<>(BoxerR.class, "upper-damage", 7.0f, PredicateUnit.positive(),
            "# 어퍼 대미지", "기본값 : 7.0");
    private static final Config<Double> UPPER_PERCENTAGE = new Config<>(BoxerR.class, "upper-percentage", 30.0, PredicateUnit.between(0.0,100.0,false),
            "# 어퍼 기절 확률", "기본값 : 30.0 (%)");
    private static final Config<Integer> UPPER_COOLDOWN = new Config<>(BoxerR.class, "upper-cooldown", 10, Condition.COOLDOWN,
            "# 어퍼 쿨타임", "기본값 : 10 (초)");

    private static final Config<Float> DUCKING_DAMAGE_DECREMENT = new Config<>(BoxerR.class, "ducking-damage-decrement", 2.0f, PredicateUnit.positive(),
            "# 더킹 이후 대미지 감소량", "기본값 : 2.0");
    private static final Config<Integer> DUCKING_COOLDOWN = new Config<>(BoxerR.class, "ducking-cooldown", 40, Condition.COOLDOWN,
            "# 더킹 쿨타임", "기본값 : 40 (초)");

    private static final Set<Material> swords;
    static {
        if (MaterialX.NETHERITE_SWORD.isSupported()) {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
        } else {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
        }
    }

    private final Jap jap = new Jap();
    private final Ducking ducking = new Ducking();

    private final Cooldown hook_cooldown = new Cooldown(HOOK_COOLDOWN.getValue(), "훅");
    private final Cooldown counter_cooldown = new Cooldown(COUNTER_COOLDOWN.getValue(), "카운터");
    private final Cooldown upper_cooldown = new Cooldown(UPPER_COOLDOWN.getValue(), "어퍼");
    private final Cooldown ducking_cooldown = new Cooldown(DUCKING_COOLDOWN.getValue(), "더킹");

    private double damage = 0;

    @Override
    public boolean usesMaterial(Material material) {
        return swords.contains(material);
    }

    public BoxerR(Participant arg0) {
        super(arg0);
    }

    public boolean ableUseAbility() {
        return jap.isRunning() || ducking.isRunning();
    }

    public boolean stop() {
        return jap.stop(false) || ducking.stop(false);
    }

    public Player getTargetedPlayer() {
        return jap.isRunning() ? jap.player : (ducking.isRunning() ? ducking.player : null);
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
            if (!ableUseAbility()) {  //잽
                jap.setPlayer((Player) e.getEntity()).start();
            } else {  //스트레이트
                if (e.getEntity().equals(getTargetedPlayer())) {
                    e.setDamage(e.getDamage() + STRAIGHT_DAMAGE_INCREMENT.getValue() - (ducking.isRunning() ? DUCKING_DAMAGE_DECREMENT.getValue() : 0.0f));
                    stop();
                }
            }
        }

        if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
            damage = e.getFinalDamage();
        }
    }

    @SubscribeEvent  //더킹
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
        AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, null, null);
        Bukkit.getPluginManager().callEvent(pre);
        if (e.getPlayer().equals(getPlayer()) && ableUseAbility() && e.isSneaking() && !pre.isCancelled() && !ducking_cooldown.isCooldown()) {
            jap.stop(false);
            ducking.setPlayer(jap.player).start();
            Bukkit.getPluginManager().callEvent(new AbilityActiveSkillEvent(this, null, null));
        }
    }

    @SubscribeEvent  // 카운터
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() != null && swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
            e.setCancelled(true);
            AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, e.getOffHandItem().getType(), null);
            Bukkit.getPluginManager().callEvent(pre);
            if (ableUseAbility() && !pre.isCancelled() && !counter_cooldown.isCooldown()) {
                Player player = getTargetedPlayer();
                float finaldamage = (float) (damage * COUNTER_PERCENTAGE.getValue() / 100.0);

                Damages.damageMagic(player, getPlayer(), false, finaldamage - (ducking.isRunning() ? DUCKING_DAMAGE_DECREMENT.getValue() : 0.0f));
                stop();
                counter_cooldown.start();

                Bukkit.getPluginManager().callEvent(new AbilityActiveSkillEvent(this, e.getOffHandItem().getType(), null));
            }
        }
    }

    @SubscribeEvent // 어퍼
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if(e.getPlayer().equals(getPlayer()) && swords.contains(e.getItemDrop().getItemStack().getType())) {
            e.setCancelled(true);
            AbilityPreActiveSkillEvent pre = new AbilityPreActiveSkillEvent(this, e.getItemDrop().getItemStack().getType(), null);
            Bukkit.getPluginManager().callEvent(pre);
            if (ableUseAbility() && !pre.isCancelled() && !upper_cooldown.isCooldown()) {
                Player player = getTargetedPlayer();
                Damages.damageMagic(player, getPlayer(), false, UPPER_DAMAGE.getValue() - (ducking.isRunning() ? DUCKING_DAMAGE_DECREMENT.getValue() : 0.0f));

                double percentage = new Random().nextDouble()*100;
                if (percentage < UPPER_PERCENTAGE.getValue()) {
                    Stun.apply(getGame().getParticipant(player), TimeUnit.SECONDS, 1);
                }
                stop();
                upper_cooldown.start();

                Bukkit.getPluginManager().callEvent(new AbilityActiveSkillEvent(this, e.getItemDrop().getItemStack().getType(), null));
            }
        }
    }

    @Override  // 훅
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (swords.contains(material) && clickType == ClickType.RIGHT_CLICK) {
            if (ableUseAbility() && !hook_cooldown.isCooldown()) {
                Player player = getTargetedPlayer();

                Damages.damageMagic(player, getPlayer(), false, HOOK_DAMAGE.getValue() - (ducking.isRunning() ? DUCKING_DAMAGE_DECREMENT.getValue() : 0.0f));
                Vector direction = player.getLocation().getDirection();
                player.setVelocity(new Vector(-direction.getZ(), 0 , direction.getX()).multiply(1.5));
                stop();
                return hook_cooldown.start();
            }
        }
        return false;
    }

    class Jap extends AbilityTimer {
        private Player player;
        private final ActionbarChannel channel = newActionbarChannel();
        public Jap() {
            super(40);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void run(int count) {
            channel.update("능력 사용 가능 : "+ Formatter.formatCooldown(getFixedCount()));
        }

        @Override
        protected void onEnd() {
            channel.update("");
        }

        @Override
        protected void onSilentEnd() {
            channel.update("");
        }

        public Jap setPlayer(Player player) {
            this.player = player;
            return this;
        }
    }

    class Ducking extends AbilityTimer {
        private final ActionbarChannel channel = newActionbarChannel();
        private Player player;
        public Ducking() {
            super(20);
            setPeriod(TimeUnit.TICKS, 1);
        }

        @Override
        protected void run(int count) {
            channel.update("더킹중");
            if (!getPlayer().isSneaking()) {
                stop(false);
            }
        }

        @Override
        protected void onEnd() {
            channel.update("");
        }

        @Override
        protected void onSilentEnd() {
            channel.update("");
        }

        public Ducking setPlayer(Player player) {
            this.player = player;
            return this;
        }
    }
}
