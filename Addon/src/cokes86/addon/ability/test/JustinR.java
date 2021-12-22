package cokes86.addon.ability.test;

import cokes86.addon.ability.CokesAbility;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AbilityManifest(name = "져스틴R", rank = AbilityManifest.Rank.L, species = AbilityManifest.Species.HUMAN, explain = {
        "패시브 - 두가지 인격: 약 $[PERIOD] 간격으로 자신의 인격이 뒤바뀝니다.",
        "  인격에 따라 각기 다른 효과를 부여받습니다.",
        "  [1인격] -",
        "  [2인격] 타 플레이어에게 받는 근거리 대미지가 120% 증가합니다.",
        "검으로 공격 시 - : 인격에 따라 각기 다른 효과를 가집니다.",
        "  [1인격] 대미지가 $[DAMAGE]로 감소된 체 공격됩니다.",
        "  [2인격] 상대방에게 흑심 카운터를 1개씩 남깁니다. (최대 $[MAX_COUNTER]회)",
        "검 우클릭 - 이거나 받아라: 인격에 따라 각기 다른 효과를 가집니다.",
        "  [1인격] 공격 준비 이후 0.5초 이내에 사용 시, 적을 밀쳐내고 감소했던 대미지를 주고 밀쳐냅니다.",
        "  [2인격] 흑심 토큰을 가지고 있던 플레이어에게 개당 $[DAMAGE]의 고정 대미지를 줍니다. $[GET_THIS_COOLDOWN]",
        "철괴 우클릭 - 탈출: 자신의 인격을 강제로 변경합니다. $[ESCAPE_COOLDOWN]",
        "  이때, 바뀐 인격의 주기는 반으로 감소합니다."
})
@Beta
public class JustinR extends CokesAbility implements ActiveHandler {
    private static final Config<Integer> PERIOD = new Config<>(JustinR.class, "인격주기", 45, Config.Condition.TIME);
    private static final Config<Integer> MAX_COUNTER = new Config<>(JustinR.class, "흑심카운터_최대치", 10, a -> a>1);
    private static final Config<Integer> GET_THIS_COOLDOWN = new Config<>(JustinR.class, "이거나_받아라_2인격_쿨타임", 60, Config.Condition.COOLDOWN);
    private static final Config<Integer> ESCAPE_COOLDOWN = new Config<>(JustinR.class, "탈출_쿨타임", 30, Config.Condition.COOLDOWN);

    private static final Set<Material> swords;
    static {
        if (MaterialX.NETHERITE_SWORD.isSupported()) {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
        } else {
            swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
        }
    }
    private NormalTimer normalTimer = new NormalTimer(getPlayer(), 0);
    private boolean madness = false;
    private final Random r = new Random();
    private final AbstractGame.Participant.ActionbarNotification.ActionbarChannel channel = newActionbarChannel();
    private final Map<AbstractGame.Participant, DarknessStack> stackMap = new ConcurrentHashMap<>();
    private final Cooldown getThisCooldown = new Cooldown(GET_THIS_COOLDOWN.getValue());
    private final Cooldown escapeCooldown = new Cooldown(ESCAPE_COOLDOWN.getValue());
    private final AbilityTimer justin = new AbilityTimer(PERIOD.getValue()*20) {
        @Override
        protected void onStart() {
            int back = r.nextInt(PERIOD.getValue()*10) - PERIOD.getValue()*5;
            setCount(getMaximumCount() + back);
        }

        @Override
        protected void run(int count) {
            channel.update(madness ? "2인격" : "1인격");
        }

        @Override
        protected void onEnd() {
            madness = !madness;
            start();
        }
    }.setPeriod(TimeUnit.TICKS, 1).setBehavior(RestrictionBehavior.PAUSE_RESUME);

    public JustinR(AbstractGame.Participant arg0) {
        super(arg0);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (swords.contains(material) && clickType.equals(ClickType.RIGHT_CLICK)) {
            if (!madness) {
                if (normalTimer.isRunning()) {
                    normalTimer.getDamageable().damage(normalTimer.getDamage(), getPlayer());
                    Location playerLocation = getPlayer().getLocation().clone();
                    normalTimer.getDamageable().setVelocity(normalTimer.getDamageable().getLocation().toVector().subtract(playerLocation.toVector()).normalize().multiply(4.5).setY(0));
                    normalTimer.stop(false);
                    return true;
                }
            } else {
                if (!stackMap.isEmpty() && !getThisCooldown.isCooldown()) {
                    for (DarknessStack stack : stackMap.values()) {
                        stack.startDamage();
                    }
                    stackMap.clear();
                }
            }
        } else if (escapeCooldown.isCooldown() && material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK)) {
            justin.stop(false);
            justin.setCount(justin.getCount()/2);
        }
        return false;
    }

    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType()) && !e.getEntity().equals(getPlayer())) {
            if (!madness) {
                if (normalTimer.isRunning()) {
                    normalTimer.stop(false);
                    if (e.getEntity().equals(normalTimer.getDamageable())) {
                        return;
                    }
                }
                double damage = e.getDamage();
                e.setDamage(damage/10*7);
                normalTimer = new NormalTimer((Damageable) e.getEntity(), damage/10*3);
                normalTimer.start();
            } else {
                if (getGame().isGameStarted() && e.getEntity() instanceof Player) {
                    if (getGame().isParticipating(e.getEntity().getUniqueId())) {
                        final AbstractGame.Participant victim = getGame().getParticipant(e.getEntity().getUniqueId());
                        if (!stackMap.containsKey(victim)) {
                            stackMap.put(victim, new DarknessStack(victim));
                        } else {
                            stackMap.get(victim).addStack();
                        }
                    }
                }
            }
        }

        if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
            e.setDamage(e.getDamage()*1.2);
        }
    }

    @Override
    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            justin.start();
        }
    }

    class NormalTimer extends AbilityTimer {
        private final Damageable damageable;
        private final double damage;

        public NormalTimer(Damageable damageable, double damage){
            super(10);
            this.damageable = damageable;
            this.damage = damage;
            setPeriod(TimeUnit.TICKS, 1);
        }

        public Damageable getDamageable() {
            return damageable;
        }

        public double getDamage() {
            return damage;
        }
    }

    private class DarknessStack extends AbilityTimer {
        private final AbstractGame.Participant target;
        private final IHologram hologram;
        private final int maxCounter = MAX_COUNTER.getValue();
        private int stack;

        private DarknessStack(AbstractGame.Participant target) {
            super();
            this.setPeriod(TimeUnit.TICKS, 1);
            this.target = target;
            final Player targetPlayer = target.getPlayer();
            this.hologram = NMS.newHologram(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ());
            this.hologram.setText(Strings.repeat("§4⚛", stack));
            this.hologram.display(getPlayer());
            this.stack = 1;
            this.start();
        }

        @Override
        protected void run(int arg0) {
            this.hologram.setText(Strings.repeat("§4⚛", stack));
            final Player targetPlayer = target.getPlayer();
            hologram.teleport(targetPlayer.getWorld(), targetPlayer.getLocation().getX(), targetPlayer.getLocation().getY() + targetPlayer.getEyeHeight() + 0.6, targetPlayer.getLocation().getZ(), targetPlayer.getLocation().getYaw(), 0);
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            hologram.hide(getPlayer());
            hologram.unregister();
        }

        private void addStack() {
            if (stack < maxCounter) {
                stack++;
            }
        }

        public void startDamage() {
            new AbilityTimer(stack) {
                @Override
                protected void run(int count) {
                    Damages.damageFixed(target.getPlayer(), getPlayer(), 2.0f);
                    target.getPlayer().setNoDamageTicks(0);
                }
            }.setPeriod(TimeUnit.TICKS, 10).start();
        }
    }
}
